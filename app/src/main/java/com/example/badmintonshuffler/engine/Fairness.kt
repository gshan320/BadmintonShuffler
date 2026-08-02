package com.example.badmintonshuffler.engine

import com.example.badmintonshuffler.model.MatchStatus
import com.example.badmintonshuffler.model.Player
import com.example.badmintonshuffler.model.SessionState
import com.example.badmintonshuffler.model.Team
import java.time.LocalTime
import java.time.temporal.ChronoUnit

/**
 * Fairness reporting and session pacing. Pure Kotlin — the clock is always passed in.
 */

data class FairnessReport(
    /**
     * The gap in *effective* games — what the rotation queue equalises, and therefore the promise
     * the app is actually making. Fair means this is 0 or 1.
     */
    val spread: Int,
    /**
     * The gap in raw games played. Informational only: it is legitimately large whenever somebody
     * joined late or came back after a break, and says nothing about whether the shuffler is being
     * fair.
     */
    val rawSpread: Int,
    val mostPlayed: List<Player>,
    val leastPlayed: List<Player>,
    val isFair: Boolean,
) {
    /** True when late joins or returns make the raw count look worse than the rotation really is. */
    val hasLateArrivals: Boolean get() = rawSpread > spread
}

/**
 * The number the app puts on screen as a pill, and the promise it is making: across a session, no
 * active player gets meaningfully more court time than any other.
 *
 * Measured on `effectiveGames` (games played + queue credit), not on raw games played. Those are
 * the same number for everybody who was there at the start, so for a stable roster this is exactly
 * "most games minus fewest". They diverge only for someone who joined late or came back from a
 * break — and there the raw count is the wrong measure. A player who arrives at round 15 is fifteen
 * games behind and always will be; saying so every round would peg the indicator to amber for the
 * rest of the afternoon and train the organiser to ignore it. What they need to know is whether
 * court time is being shared evenly *from here*, which is precisely what the queue equalises.
 *
 * Players who have left are excluded — they stopped accruing games when they walked out.
 */
fun getFairnessReport(state: SessionState): FairnessReport {
    val active = state.activePlayers
    if (active.isEmpty()) {
        return FairnessReport(
            spread = 0,
            rawSpread = 0,
            mostPlayed = emptyList(),
            leastPlayed = emptyList(),
            isFair = true,
        )
    }

    val max = active.maxOf { it.effectiveGames }
    val min = active.minOf { it.effectiveGames }
    val rawMax = active.maxOf { it.gamesPlayed }
    val rawMin = active.minOf { it.gamesPlayed }

    return FairnessReport(
        spread = max - min,
        rawSpread = rawMax - rawMin,
        mostPlayed = active.filter { it.effectiveGames == max },
        leastPlayed = active.filter { it.effectiveGames == min },
        isFair = max - min <= 1,
    )
}

/**
 * How many more rounds will fit before the hall closes, at the configured pace.
 *
 * Deliberately floor rather than round: promising a round there isn't time for is worse than
 * finishing five minutes early.
 */
fun estimateRemainingRounds(state: SessionState, now: LocalTime): Int {
    val minutesPerGame = state.config.minutesPerGame
    if (minutesPerGame <= 0) return 0
    val minutesLeft = ChronoUnit.MINUTES.between(now, state.config.endTime)
    if (minutesLeft <= 0) return 0
    return (minutesLeft / minutesPerGame).toInt()
}

/** Rounds that have actually been played (a round counts once it has been generated). */
fun roundsPlayed(state: SessionState): Int = state.rounds.size

// ---------------------------------------------------------------------------------------------
// End-of-session statistics
// ---------------------------------------------------------------------------------------------

data class PartnershipRecord(
    val playerA: Player,
    val playerB: Player,
    val wins: Int,
    val games: Int,
) {
    val winRate: Float get() = if (games == 0) 0f else wins.toFloat() / games
}

data class SessionStats(
    val totalGames: Int,
    val mostGamesPlayed: Int,
    val mostGamesPlayers: List<Player>,
    val longestWinStreak: Int,
    val longestWinStreakPlayers: List<Player>,
    val bestPartnership: PartnershipRecord?,
)

/** The small-type facts on the results screen. Nothing here affects ranking. */
fun buildSessionStats(state: SessionState): SessionStats {
    val completed = state.rounds.flatMap { it.matches }.filter { it.status == MatchStatus.COMPLETED }

    val maxGames = state.players.maxOfOrNull { it.gamesPlayed } ?: 0
    val streaks = state.players.associate { it.id to longestWinStreak(state, it.id) }
    val maxStreak = streaks.values.maxOrNull() ?: 0

    val partnerships = mutableMapOf<Pair<String, String>, Pair<Int, Int>>() // key -> (wins, games)
    for (match in completed) {
        val winner = match.winner
        for (team in listOf(match.teamA, match.teamB)) {
            val key = team.key()
            val (wins, games) = partnerships[key] ?: (0 to 0)
            val won = winner?.key() == key
            partnerships[key] = (if (won) wins + 1 else wins) to (games + 1)
        }
    }

    val best = partnerships.entries
        .mapNotNull { (key, record) ->
            val a = state.player(key.first) ?: return@mapNotNull null
            val b = state.player(key.second) ?: return@mapNotNull null
            PartnershipRecord(a, b, wins = record.first, games = record.second)
        }
        // Require at least two games together, otherwise "best partnership" is just whoever happened
        // to win once — technically true, entirely meaningless.
        .filter { it.games >= 2 }
        .maxWithOrNull(compareBy<PartnershipRecord> { it.winRate }.thenBy { it.games })

    return SessionStats(
        totalGames = completed.size,
        mostGamesPlayed = maxGames,
        mostGamesPlayers = state.players.filter { it.gamesPlayed == maxGames && maxGames > 0 },
        longestWinStreak = maxStreak,
        longestWinStreakPlayers = state.players.filter { streaks[it.id] == maxStreak && maxStreak > 0 },
        bestPartnership = best,
    )
}

/** Consecutive wins, in the order the matches were played. */
fun longestWinStreak(state: SessionState, playerId: String): Int {
    var best = 0
    var current = 0
    state.rounds.sortedBy { it.index }.forEach { round ->
        round.matches
            .filter { it.status == MatchStatus.COMPLETED && it.contains(playerId) }
            .sortedBy { it.courtNumber }
            .forEach { match ->
                if (match.winner?.contains(playerId) == true) {
                    current++
                    if (current > best) best = current
                } else {
                    current = 0
                }
            }
    }
    return best
}

private fun Team.key(): Pair<String, String> =
    if (first <= second) first to second else second to first
