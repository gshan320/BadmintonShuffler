package com.example.badmintonshuffler.model

import java.time.LocalTime
import java.time.temporal.ChronoUnit

/**
 * The complete data model for a session.
 *
 * Everything here is immutable. Engine functions take a [SessionState] and return a new one; nothing
 * in this file is ever mutated in place. See CLAUDE.md.
 */

// ---------------------------------------------------------------------------------------------
// Players
// ---------------------------------------------------------------------------------------------

data class Player(
    val id: String,
    val name: String,
    /** False once someone has left. Their record is kept forever — see [SessionState.players]. */
    val isActive: Boolean = true,
    /** Index of the round at which this player became available. 0 for everyone who set up. */
    val joinedAtRound: Int = 0,

    /** True number of games played. This is the honest count, used for stats and fairness. */
    val gamesPlayed: Int = 0,

    /**
     * A virtual head start used **only** by the rotation queue, never shown to the user and never
     * part of any statistic.
     *
     * The rotation queue sorts by `gamesPlayed + queueCredit`, so a player with credit looks like
     * they have already played that many games and therefore waits their turn like everybody else.
     *
     * This is what makes a late joiner fair. When someone arrives at round 6 with the group on 5
     * games each, [LateJoinerMode.FAIR_FORWARD] sets their credit to the group's minimum games
     * played. They enter the queue level with the least-played player, so from that moment on
     * everyone — including them — gets an equal share of games. Without the credit they would look
     * infinitely under-played and would monopolise every court until they caught up, pushing the
     * people who turned up on time off the court to pay for it.
     *
     * Crucially it does *not* touch [wins], [pointsFor] or the leaderboard, so the early birds keep
     * the lead they actually earned.
     */
    val queueCredit: Int = 0,

    val wins: Int = 0,
    val losses: Int = 0,

    /** Rally points scored and conceded across all completed games. */
    val pointsFor: Int = 0,
    val pointsAgainst: Int = 0,

    /** Consecutive rounds sat out. Drives "whoever has waited longest goes first". */
    val restStreak: Int = 0,

    /** playerId -> number of times partnered with them. */
    val partnerHistory: Map<String, Int> = emptyMap(),
    /** playerId -> number of times faced them. */
    val opponentHistory: Map<String, Int> = emptyMap(),
) {
    /** The value the rotation queue actually sorts on. Never display this. */
    val effectiveGames: Int get() = gamesPlayed + queueCredit

    val gamesDecided: Int get() = wins + losses
    val winRate: Float get() = if (gamesDecided == 0) 0f else wins.toFloat() / gamesDecided
    val pointDifferential: Int get() = pointsFor - pointsAgainst
}

// ---------------------------------------------------------------------------------------------
// Matches and rounds
// ---------------------------------------------------------------------------------------------

/** Exactly two player ids — doubles, always. Modelled as two fields so it cannot be otherwise. */
data class Team(val first: String, val second: String) {
    val ids: List<String> get() = listOf(first, second)

    fun contains(playerId: String): Boolean = first == playerId || second == playerId

    /** The other member of this team, or null if [playerId] is not on it. */
    fun partnerOf(playerId: String): String? = when (playerId) {
        first -> second
        second -> first
        else -> null
    }

    fun replacing(outgoing: String, incoming: String): Team = when (outgoing) {
        first -> copy(first = incoming)
        second -> copy(second = incoming)
        else -> this
    }
}

enum class MatchStatus { PENDING, COMPLETED, VOIDED }

data class Match(
    val id: String,
    val courtNumber: Int,
    val teamA: Team,
    val teamB: Team,
    val status: MatchStatus = MatchStatus.PENDING,
    val scoreA: Int? = null,
    val scoreB: Int? = null,
    val roundIndex: Int,
) {
    val playerIds: List<String> get() = teamA.ids + teamB.ids

    fun contains(playerId: String): Boolean = teamA.contains(playerId) || teamB.contains(playerId)

    /** The winning team, or null if the match has no valid recorded result. */
    val winner: Team?
        get() {
            if (status != MatchStatus.COMPLETED) return null
            val a = scoreA ?: return null
            val b = scoreB ?: return null
            return if (a > b) teamA else if (b > a) teamB else null
        }
}

enum class RoundStatus { IN_PROGRESS, COMPLETED }

data class Round(
    val index: Int,
    val matches: List<Match> = emptyList(),
    val sittingOut: List<String> = emptyList(),
    val status: RoundStatus = RoundStatus.IN_PROGRESS,
) {
    /** A round is finished when every match that still counts has a result. */
    val isFullyScored: Boolean
        get() = matches.none { it.status == MatchStatus.PENDING }

    val playingIds: List<String> get() = matches.flatMap { it.playerIds }
}

// ---------------------------------------------------------------------------------------------
// Configuration
// ---------------------------------------------------------------------------------------------

/**
 * How a player who arrives after the session has started enters the rotation.
 *
 * This is the single most argued-about rule in a badminton hall, so the app takes a position and
 * explains it: [FAIR_FORWARD] is the default.
 */
enum class LateJoinerMode {
    /**
     * The newcomer's [Player.queueCredit] is set to the minimum games played among active players,
     * so they join the queue level with the least-played person. Everyone, newcomer included, gets
     * an equal share of games **from now on**. The newcomer does not displace anyone to catch up,
     * and because their points genuinely start at zero, whoever arrived on time keeps their lead.
     */
    FAIR_FORWARD,

    /**
     * The newcomer gets no credit, so the queue treats them as having played zero games and
     * prioritises them on every court until their games played matches the group. Fair in a "you
     * missed out, have it back" sense, but it costs everyone else court time.
     */
    CATCH_UP,
}

data class SessionConfig(
    val courtCount: Int = SessionDefaults.COURT_COUNT,
    val startTime: LocalTime = SessionDefaults.START_TIME,
    val endTime: LocalTime = SessionDefaults.END_TIME,
    val minutesPerGame: Int = SessionDefaults.MINUTES_PER_GAME,
    val pointsPerWin: Int = SessionDefaults.POINTS_PER_WIN,
    val pointsPerLoss: Int = SessionDefaults.POINTS_PER_LOSS,
    val targetScore: Int = SessionDefaults.TARGET_SCORE,
    val lateJoinerMode: LateJoinerMode = SessionDefaults.LATE_JOINER_MODE,
) {
    /** Session length in minutes. Zero if the end time is not after the start time. */
    val durationMinutes: Int
        get() = ChronoUnit.MINUTES.between(startTime, endTime).toInt().coerceAtLeast(0)

    /** How many rounds the whole session is expected to fit, at the configured pace. */
    val estimatedTotalRounds: Int
        get() = if (minutesPerGame <= 0) 0 else durationMinutes / minutesPerGame

    /** Players on court simultaneously when every court is busy. */
    val playersOnCourt: Int get() = courtCount * 4
}

object SessionDefaults {
    const val COURT_COUNT = 2
    const val MINUTES_PER_GAME = 12
    const val POINTS_PER_WIN = 3
    const val POINTS_PER_LOSS = 1
    const val TARGET_SCORE = 21
    val LATE_JOINER_MODE = LateJoinerMode.FAIR_FORWARD

    val START_TIME: LocalTime = LocalTime.of(18, 0)
    val END_TIME: LocalTime = LocalTime.of(20, 0)

    const val MIN_COURTS = 1
    const val MAX_COURTS = 8
    const val MIN_MINUTES_PER_GAME = 5
    const val MAX_MINUTES_PER_GAME = 30

    /** Doubles: you cannot run a round with fewer than one full court of people. */
    const val MIN_PLAYERS = 4
}

// ---------------------------------------------------------------------------------------------
// Session
// ---------------------------------------------------------------------------------------------

enum class SessionStatus { SETUP, ACTIVE, ENDED }

data class SessionState(
    val config: SessionConfig = SessionConfig(),
    /** Every player who has ever been in this session, including those who left. */
    val players: List<Player> = emptyList(),
    val rounds: List<Round> = emptyList(),
    val currentRoundIndex: Int = -1,
    val status: SessionStatus = SessionStatus.SETUP,
) {
    val activePlayers: List<Player> get() = players.filter { it.isActive }

    val currentRound: Round? get() = rounds.getOrNull(currentRoundIndex)

    fun player(id: String): Player? = players.firstOrNull { it.id == id }

    fun match(matchId: String): Match? =
        rounds.asSequence().flatMap { it.matches }.firstOrNull { it.id == matchId }

    /** Courts that can actually be filled right now, given how many people are on their feet. */
    val usableCourts: Int get() = minOf(config.courtCount, activePlayers.size / 4)
}

// ---------------------------------------------------------------------------------------------
// Leaderboard
// ---------------------------------------------------------------------------------------------

data class LeaderboardEntry(
    /** 1-based, with ties sharing a rank: two players tied for 1st are followed by 3rd. */
    val rank: Int,
    val player: Player,
    val sessionPoints: Int,
    val pointDifferential: Int,
    val winRate: Float,
)
