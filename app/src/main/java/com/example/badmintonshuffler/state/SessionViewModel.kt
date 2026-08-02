package com.example.badmintonshuffler.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.badmintonshuffler.BuildConfig
import com.example.badmintonshuffler.engine.FairnessReport
import com.example.badmintonshuffler.engine.ScoreOutcome
import com.example.badmintonshuffler.engine.ScoreRejection
import com.example.badmintonshuffler.engine.SessionStats
import com.example.badmintonshuffler.engine.addPlayer
import com.example.badmintonshuffler.engine.amendResult
import com.example.badmintonshuffler.engine.availableSubstitutes
import com.example.badmintonshuffler.engine.buildLeaderboard
import com.example.badmintonshuffler.engine.buildSessionStats
import com.example.badmintonshuffler.engine.canAdvanceRound
import com.example.badmintonshuffler.engine.canStartSession
// Aliased: an unqualified clearSession() inside the member of the same name would recurse.
import com.example.badmintonshuffler.engine.clearSession as emptySession
import com.example.badmintonshuffler.engine.endSession
import com.example.badmintonshuffler.engine.estimateRemainingRounds
import com.example.badmintonshuffler.engine.getFairnessReport
import com.example.badmintonshuffler.engine.nextInQueue
import com.example.badmintonshuffler.engine.recordResult
import com.example.badmintonshuffler.engine.removePlayer
import com.example.badmintonshuffler.engine.startNextRound
import com.example.badmintonshuffler.engine.startSession
import com.example.badmintonshuffler.engine.substituteInPendingMatch
import com.example.badmintonshuffler.engine.voidMatch
import com.example.badmintonshuffler.model.LeaderboardEntry
import com.example.badmintonshuffler.model.Match
import com.example.badmintonshuffler.model.MatchStatus
import com.example.badmintonshuffler.model.Player
import com.example.badmintonshuffler.model.Round
import com.example.badmintonshuffler.model.SessionConfig
import com.example.badmintonshuffler.model.SessionState
import com.example.badmintonshuffler.model.SessionStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalTime
import kotlin.random.Random

/**
 * A thin wrapper around the engine.
 *
 * Every mutating action delegates to a pure function in `engine/` and replaces the state with what
 * comes back. There is no logic here worth unit testing — that is the point, and it is why the
 * fairness tests can cover the whole product without touching Android.
 *
 * State lives in memory only. Nothing is persisted, by design (see SPEC.md). Surviving a screen
 * rotation is the ViewModel's job and is as far as it goes; killing the process ends the session.
 */
class SessionViewModel : ViewModel() {

    private val _state = MutableStateFlow(SessionState())
    val state: StateFlow<SessionState> = _state.asStateFlow()

    /**
     * One random base per session, so real sessions differ from each other while any single session
     * stays reproducible. Tests drive the engine directly and pass their own seeds.
     */
    private var sessionSeed: Int = Random.nextInt()

    /** Ticks every minute so "about 9 rounds left" doesn't go stale on a phone left on a bench. */
    private val clock = MutableStateFlow(LocalTime.now())

    init {
        viewModelScope.launch {
            while (true) {
                delay(60_000)
                clock.value = LocalTime.now()
            }
        }
    }

    // -----------------------------------------------------------------------------------------
    // Selectors
    // -----------------------------------------------------------------------------------------

    val currentRound: StateFlow<Round?> =
        _state.map { it.currentRound }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val activePlayers: StateFlow<List<Player>> =
        _state.map { it.activePlayers }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val leaderboard: StateFlow<List<LeaderboardEntry>> =
        _state.map { buildLeaderboard(it) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val fairness: StateFlow<FairnessReport> =
        _state.map { getFairnessReport(it) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, getFairnessReport(SessionState()))

    val sessionProgress: StateFlow<SessionProgress> =
        combine(_state, clock) { session, now ->
            SessionProgress(
                roundsPlayed = session.rounds.size,
                estimatedRoundsRemaining = estimateRemainingRounds(session, now),
                endTime = session.config.endTime,
                isPastEndTime = now.isAfter(session.config.endTime),
            )
        }.stateIn(viewModelScope, SharingStarted.Eagerly, SessionProgress())

    val stats: StateFlow<SessionStats> =
        _state.map { buildSessionStats(it) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, buildSessionStats(SessionState()))

    val canStart: StateFlow<Boolean> =
        _state.map { canStartSession(it) }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val canAdvance: StateFlow<Boolean> =
        _state.map { canAdvanceRound(it) }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    // -----------------------------------------------------------------------------------------
    // Setup
    // -----------------------------------------------------------------------------------------

    fun setConfig(transform: SessionConfig.() -> SessionConfig) {
        _state.update { it.copy(config = it.config.transform()) }
    }

    fun addPlayer(name: String) {
        if (name.isBlank()) return
        _state.update { addPlayer(it, name.trim()) }
    }

    fun removePlayerDuringSetup(playerId: String) {
        _state.update { current ->
            if (current.status != SessionStatus.SETUP) current
            else current.copy(players = current.players.filterNot { it.id == playerId })
        }
    }

    /** Write the config, deal round one. */
    fun startSession() {
        _state.update { current ->
            if (!canStartSession(current)) current else startSession(current, seedFor(current))
        }
    }

    // -----------------------------------------------------------------------------------------
    // Live session
    // -----------------------------------------------------------------------------------------

    fun generateNextRound() {
        _state.update { current ->
            if (!canAdvanceRound(current)) current else startNextRound(current, seedFor(current))
        }
    }

    /**
     * Record or correct a score.
     *
     * Returns the rejection reason if the engine refused, so the sheet can say why rather than
     * silently doing nothing. A double tap lands here twice; the second call finds the match already
     * completed and is rejected, which is exactly the guard we want.
     */
    fun recordResult(matchId: String, scoreA: Int, scoreB: Int): ScoreRejection? {
        val current = _state.value
        val match = current.match(matchId) ?: return ScoreRejection.MATCH_NOT_FOUND

        val outcome = if (match.status == MatchStatus.COMPLETED) {
            amendResult(current, matchId, scoreA, scoreB)
        } else {
            recordResult(current, matchId, scoreA, scoreB)
        }

        return when (outcome) {
            is ScoreOutcome.Accepted -> {
                _state.value = outcome.state
                null
            }
            is ScoreOutcome.Rejected -> outcome.reason
        }
    }

    fun voidMatch(matchId: String) {
        _state.update { voidMatch(it, matchId) }
    }

    // -----------------------------------------------------------------------------------------
    // Mid-session roster edits
    // -----------------------------------------------------------------------------------------

    /**
     * Remove a player, and report whether they were mid-match so the UI can offer the substitute or
     * void choice. Nothing is decided here.
     */
    fun removePlayer(playerId: String): RemovalPrompt {
        val outcome = removePlayer(_state.value, playerId)
        _state.value = outcome.state
        return RemovalPrompt(
            playerId = playerId,
            affectedMatchId = outcome.affectedMatchId,
            recommendedSubstitute = outcome.affectedMatchId?.let {
                nextInQueue(
                    state = outcome.state,
                    excluding = outcome.state.currentRound?.playingIds.orEmpty().toSet(),
                    seed = seedFor(outcome.state),
                )
            },
        )
    }

    fun substitutePlayer(outgoingPlayerId: String, incomingPlayerId: String) {
        _state.update { substituteInPendingMatch(it, outgoingPlayerId, incomingPlayerId) }
    }

    fun substitutesFor(match: Match): List<Player> =
        availableSubstitutes(_state.value, excluding = match.playerIds.toSet())

    // -----------------------------------------------------------------------------------------
    // Closing down
    // -----------------------------------------------------------------------------------------

    fun endSession() {
        _state.update { endSession(it) }
    }

    /** Wipes everything. There is nothing on disk to recover from — confirm before calling. */
    fun clearSession() {
        sessionSeed = Random.nextInt()
        _state.value = emptySession()
    }

    // -----------------------------------------------------------------------------------------
    // Internals
    // -----------------------------------------------------------------------------------------

    /** Same session, same sequence of rounds; different sessions, different shuffles. */
    private fun seedFor(state: SessionState): Int = sessionSeed + state.rounds.size * 7919

    // -----------------------------------------------------------------------------------------
    // Dev helper
    // -----------------------------------------------------------------------------------------

    /**
     * Fill the session with fake players and a few played rounds, so the live screens can be
     * reviewed without typing twelve names into a phone every time. Debug builds only.
     */
    fun seedDemoSession(playerCount: Int = 12, roundsToPlay: Int = 3) {
        if (!BuildConfig.DEBUG) return

        val names = listOf(
            "Aisha", "Ben", "Chloe", "Dan", "Ella", "Faisal", "Grace", "Hari",
            "Iris", "Jun", "Kira", "Leo", "Mei", "Nadia", "Omar", "Priya",
        )

        var demo = SessionState(config = SessionConfig(courtCount = 3))
        repeat(playerCount) { i ->
            demo = addPlayer(demo, names.getOrElse(i) { "Player ${i + 1}" })
        }
        demo = startSession(demo, sessionSeed)

        val rng = Random(sessionSeed)
        repeat(roundsToPlay) {
            demo.currentRound?.matches.orEmpty().forEach { match ->
                val loser = 12 + rng.nextInt(9)
                val aWins = rng.nextBoolean()
                val outcome = recordResult(
                    demo,
                    match.id,
                    if (aWins) 21 else loser,
                    if (aWins) loser else 21,
                )
                if (outcome is ScoreOutcome.Accepted) demo = outcome.state
            }
            if (canAdvanceRound(demo)) {
                demo = startNextRound(demo, seedFor(demo))
            }
        }

        _state.value = demo
    }
}

data class SessionProgress(
    val roundsPlayed: Int = 0,
    val estimatedRoundsRemaining: Int = 0,
    val endTime: LocalTime = LocalTime.MIDNIGHT,
    val isPastEndTime: Boolean = false,
)

/** What the UI must ask about after a removal. Null [affectedMatchId] means there is nothing to ask. */
data class RemovalPrompt(
    val playerId: String,
    val affectedMatchId: String?,
    val recommendedSubstitute: Player?,
)
