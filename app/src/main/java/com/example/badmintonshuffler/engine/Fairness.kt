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
    /** Most games played minus fewest, across active players only. */
    val spread: Int,
    val mostPlayed: List<Player>,
    val leastPlayed: List<Player>,
    val isFair: Boolean,
)

/**
 * The number the app puts on screen as a pill, and the promise it is making: across a session, no
 * active player should be more than one game ahead of any other.
 *
 * Players who have left are excluded — they stopped accruing games when they walked out, and
 * counting them would make the spread grow forever and the indicator meaningless.
 */
fun getFairnessReport(state: SessionState): FairnessReport {
    val active = state.activePlayers
    if (active.isEmpty()) {
        return FairnessReport(spread = 0, mostPlayed = emptyList(), leastPlayed = emptyList(), isFair = true)
    }

    val max = active.maxOf { it.gamesPlayed }
    val min = active.minOf { it.gamesPlayed }

    return FairnessReport(
        spread = max - min,
        mostPlayed = active.filter { it.gamesPlayed == max },
        leastPlayed = active.filter { it.gamesPlayed == min },
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
