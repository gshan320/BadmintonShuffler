package com.example.badmintonshuffler.engine

import com.example.badmintonshuffler.model.LeaderboardEntry
import com.example.badmintonshuffler.model.Match
import com.example.badmintonshuffler.model.MatchStatus
import com.example.badmintonshuffler.model.Player
import com.example.badmintonshuffler.model.SessionConfig
import com.example.badmintonshuffler.model.SessionState

/**
 * Recording results and turning them into a leaderboard. Pure Kotlin; never mutates its input.
 */

enum class ScoreRejection {
    MATCH_NOT_FOUND,
    MATCH_NOT_PENDING,
    NEGATIVE_SCORE,
    /** Badminton has no draws. A tied score is always a typo. */
    EQUAL_SCORES,
}

sealed interface ScoreOutcome {
    data class Accepted(val state: SessionState) : ScoreOutcome
    data class Rejected(val reason: ScoreRejection) : ScoreOutcome
}

/**
 * Validation that stops short of rejecting.
 *
 * A score below the target is usually a game cut short because the hall is closing, which is normal
 * and must be allowed — but it is also what a mis-tap looks like, so the UI warns first.
 */
data class ScoreCheck(
    val rejection: ScoreRejection?,
    val belowTarget: Boolean,
) {
    val isAcceptable: Boolean get() = rejection == null
}

fun checkScore(config: SessionConfig, scoreA: Int, scoreB: Int): ScoreCheck = ScoreCheck(
    rejection = when {
        scoreA < 0 || scoreB < 0 -> ScoreRejection.NEGATIVE_SCORE
        scoreA == scoreB -> ScoreRejection.EQUAL_SCORES
        else -> null
    },
    belowTarget = maxOf(scoreA, scoreB) < config.targetScore,
)

/**
 * Write a result for one match.
 *
 * Both winners are credited identically — that is the promise. Individual totals diverge later only
 * because the shuffler puts people in different teams, never because of anything recorded here.
 */
fun recordResult(state: SessionState, matchId: String, scoreA: Int, scoreB: Int): ScoreOutcome {
    val match = state.match(matchId) ?: return ScoreOutcome.Rejected(ScoreRejection.MATCH_NOT_FOUND)
    if (match.status != MatchStatus.PENDING) {
        return ScoreOutcome.Rejected(ScoreRejection.MATCH_NOT_PENDING)
    }
    checkScore(state.config, scoreA, scoreB).rejection?.let {
        return ScoreOutcome.Rejected(it)
    }

    val aWon = scoreA > scoreB
    val updatedPlayers = state.players.map { player ->
        when {
            match.teamA.contains(player.id) ->
                player.applyResult(match, ownScore = scoreA, otherScore = scoreB, won = aWon)
            match.teamB.contains(player.id) ->
                player.applyResult(match, ownScore = scoreB, otherScore = scoreA, won = !aWon)
            else -> player
        }
    }

    return ScoreOutcome.Accepted(
        state.copy(
            players = updatedPlayers,
            rounds = state.replacingMatch(matchId) {
                it.copy(status = MatchStatus.COMPLETED, scoreA = scoreA, scoreB = scoreB)
            },
        )
    )
}

/**
 * Correct a score that has already been recorded, by unwinding the old result and applying the new
 * one. Used when someone fat-fingers 21-51 and spots it before the round advances.
 */
fun amendResult(state: SessionState, matchId: String, scoreA: Int, scoreB: Int): ScoreOutcome {
    val match = state.match(matchId) ?: return ScoreOutcome.Rejected(ScoreRejection.MATCH_NOT_FOUND)
    if (match.status != MatchStatus.COMPLETED) {
        return ScoreOutcome.Rejected(ScoreRejection.MATCH_NOT_PENDING)
    }
    checkScore(state.config, scoreA, scoreB).rejection?.let {
        return ScoreOutcome.Rejected(it)
    }
    return recordResult(reverseResult(state, match), matchId, scoreA, scoreB)
}

/**
 * Void a match: nobody gets credit and the court frees up. Offered when a player has to leave
 * mid-game and no substitute is available.
 */
fun voidMatch(state: SessionState, matchId: String): SessionState {
    val match = state.match(matchId) ?: return state
    val unwound = if (match.status == MatchStatus.COMPLETED) reverseResult(state, match) else state
    return unwound.copy(
        rounds = unwound.replacingMatch(matchId) {
            it.copy(status = MatchStatus.VOIDED, scoreA = null, scoreB = null)
        }
    )
}

/** Undo everything [recordResult] did for a completed match. */
private fun reverseResult(state: SessionState, match: Match): SessionState {
    val scoreA = match.scoreA ?: return state
    val scoreB = match.scoreB ?: return state
    val aWon = scoreA > scoreB

    val players = state.players.map { player ->
        when {
            match.teamA.contains(player.id) ->
                player.reverseResult(match, ownScore = scoreA, otherScore = scoreB, won = aWon)
            match.teamB.contains(player.id) ->
                player.reverseResult(match, ownScore = scoreB, otherScore = scoreA, won = !aWon)
            else -> player
        }
    }

    return state.copy(
        players = players,
        rounds = state.replacingMatch(match.id) {
            it.copy(status = MatchStatus.PENDING, scoreA = null, scoreB = null)
        },
    )
}

private fun Player.applyResult(match: Match, ownScore: Int, otherScore: Int, won: Boolean): Player {
    val ownTeam = if (match.teamA.contains(id)) match.teamA else match.teamB
    val otherTeam = if (match.teamA.contains(id)) match.teamB else match.teamA
    val partner = ownTeam.partnerOf(id)

    return copy(
        gamesPlayed = gamesPlayed + 1,
        restStreak = 0,
        wins = if (won) wins + 1 else wins,
        losses = if (won) losses else losses + 1,
        pointsFor = pointsFor + ownScore,
        pointsAgainst = pointsAgainst + otherScore,
        partnerHistory = partner?.let { partnerHistory.bump(it, 1) } ?: partnerHistory,
        opponentHistory = otherTeam.ids.fold(opponentHistory) { acc, opponent -> acc.bump(opponent, 1) },
    )
}

private fun Player.reverseResult(match: Match, ownScore: Int, otherScore: Int, won: Boolean): Player {
    val ownTeam = if (match.teamA.contains(id)) match.teamA else match.teamB
    val otherTeam = if (match.teamA.contains(id)) match.teamB else match.teamA
    val partner = ownTeam.partnerOf(id)

    return copy(
        gamesPlayed = (gamesPlayed - 1).coerceAtLeast(0),
        wins = if (won) (wins - 1).coerceAtLeast(0) else wins,
        losses = if (won) losses else (losses - 1).coerceAtLeast(0),
        pointsFor = (pointsFor - ownScore).coerceAtLeast(0),
        pointsAgainst = (pointsAgainst - otherScore).coerceAtLeast(0),
        partnerHistory = partner?.let { partnerHistory.bump(it, -1) } ?: partnerHistory,
        opponentHistory = otherTeam.ids.fold(opponentHistory) { acc, opponent -> acc.bump(opponent, -1) },
    )
}

private fun Map<String, Int>.bump(key: String, by: Int): Map<String, Int> {
    val next = (this[key] ?: 0) + by
    return if (next <= 0) this - key else this + (key to next)
}

private fun SessionState.replacingMatch(matchId: String, transform: (Match) -> Match) =
    rounds.map { round ->
        if (round.matches.none { it.id == matchId }) round
        else round.copy(matches = round.matches.map { if (it.id == matchId) transform(it) else it })
    }

// ---------------------------------------------------------------------------------------------
// Leaderboard
// ---------------------------------------------------------------------------------------------

fun sessionPoints(player: Player, config: SessionConfig): Int =
    player.wins * config.pointsPerWin + player.losses * config.pointsPerLoss

/**
 * Everyone who ever played, ranked.
 *
 * Players who left are included — they earned their points and they keep them. Ranks share on ties,
 * so two players tied for first are both first and the next player is third.
 */
fun buildLeaderboard(state: SessionState): List<LeaderboardEntry> {
    val ranked = state.players
        .map { player ->
            LeaderboardEntry(
                rank = 0,
                player = player,
                sessionPoints = sessionPoints(player, state.config),
                pointDifferential = player.pointDifferential,
                winRate = player.winRate,
            )
        }
        .sortedWith(
            compareByDescending<LeaderboardEntry> { it.sessionPoints }
                .thenByDescending { it.pointDifferential }
                .thenByDescending { it.winRate }
                .thenByDescending { it.player.pointsFor }
                .thenBy { it.player.gamesPlayed }
                .thenBy { it.player.name.lowercase() }
        )

    // Standard competition ranking. Name is a display-order tiebreak only — it must never be what
    // separates two genuinely tied players into different ranks.
    var lastKey: List<Comparable<*>>? = null
    var lastRank = 0
    return ranked.mapIndexed { index, entry ->
        val key = entry.tieKey()
        val rank = if (key == lastKey) lastRank else index + 1
        lastKey = key
        lastRank = rank
        entry.copy(rank = rank)
    }
}

private fun LeaderboardEntry.tieKey(): List<Comparable<*>> =
    listOf(sessionPoints, pointDifferential, winRate, player.pointsFor, player.gamesPlayed)
