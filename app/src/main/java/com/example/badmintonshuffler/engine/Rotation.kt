package com.example.badmintonshuffler.engine

import com.example.badmintonshuffler.model.Match
import com.example.badmintonshuffler.model.MatchStatus
import com.example.badmintonshuffler.model.Player
import com.example.badmintonshuffler.model.Round
import com.example.badmintonshuffler.model.RoundStatus
import com.example.badmintonshuffler.model.SessionState
import com.example.badmintonshuffler.model.Team

/**
 * The rotation engine — who plays, with whom, on which court.
 *
 * Pure Kotlin. No Android, no Compose, no clocks, no unseeded randomness.
 */

/** Repeating a partner is three times as bad as repeating an opponent. */
const val PARTNER_WEIGHT = 3
const val OPPONENT_WEIGHT = 1

/** How many arrangements to try before picking the least repetitive one. */
const val CANDIDATE_ARRANGEMENTS = 300

/**
 * Build the next round without applying it.
 *
 * Pure: returns a [Round] and changes nothing. [startNextRound] is what commits it to the session.
 */
fun generateRound(state: SessionState, seed: Int): Round {
    val roundIndex = state.rounds.size
    val rng = SeededRng(seed)
    val eligible = state.activePlayers

    // Step 1 — capacity. Doubles only, so people come off the bench four at a time.
    val playableSlots = minOf(state.config.courtCount * 4, (eligible.size / 4) * 4)
    if (playableSlots == 0) {
        return Round(
            index = roundIndex,
            matches = emptyList(),
            sittingOut = eligible.map { it.id },
            status = RoundStatus.IN_PROGRESS,
        )
    }

    // Step 2 — selection queue.
    val queue = rotationQueue(eligible, rng)
    val selected = queue.take(playableSlots)
    val sittingOut = queue.drop(playableSlots)

    // Step 3 — team formation.
    val arrangement = bestArrangement(selected, state, rng)

    // Step 4 — courts, in order.
    val matches = arrangement.mapIndexed { i, group ->
        Match(
            id = matchId(roundIndex, i + 1),
            courtNumber = i + 1,
            teamA = group.teamA,
            teamB = group.teamB,
            status = MatchStatus.PENDING,
            roundIndex = roundIndex,
        )
    }

    return Round(
        index = roundIndex,
        matches = matches,
        sittingOut = sittingOut.map { it.id },
        status = RoundStatus.IN_PROGRESS,
    )
}

/**
 * Generate the next round and commit it: append it, close the previous one, move the pointer, and
 * update everyone's rest streak.
 *
 * Rest streaks are maintained here rather than in scoring so that the "nobody sits out three rounds
 * running" promise holds even for rounds that get voided or left unscored.
 */
fun startNextRound(state: SessionState, seed: Int): SessionState {
    val round = generateRound(state, seed)
    val playing = round.playingIds.toSet()
    val resting = round.sittingOut.toSet()

    val players = state.players.map { player ->
        when (player.id) {
            in playing -> player.copy(restStreak = 0)
            in resting -> player.copy(restStreak = player.restStreak + 1)
            else -> player
        }
    }

    val rounds = state.rounds.map { existing ->
        if (existing.index == state.currentRoundIndex) existing.copy(status = RoundStatus.COMPLETED)
        else existing
    } + round

    return state.copy(
        players = players,
        rounds = rounds,
        currentRoundIndex = round.index,
    )
}

/**
 * The order in which players come off the bench:
 *  1. fewest effective games (games played + queue credit) first — this is the fairness guarantee;
 *  2. then whoever has waited longest — this is what prevents starvation;
 *  3. then seeded jitter, so equal players don't always resolve in the same order.
 */
fun rotationQueue(eligible: List<Player>, rng: SeededRng): List<Player> {
    // Draw jitter in a stable order so the result depends only on the seed and the roster.
    val jitter = eligible.sortedBy { it.id }.associate { it.id to rng.nextFloat() }
    return eligible.sortedWith(
        compareBy<Player> { it.effectiveGames }
            .thenByDescending { it.restStreak }
            .thenBy { jitter.getValue(it.id) }
    )
}

/** The player the app should offer first when a substitute is needed. */
fun nextInQueue(state: SessionState, excluding: Set<String>, seed: Int): Player? {
    val candidates = state.activePlayers.filter { it.id !in excluding }
    if (candidates.isEmpty()) return null
    return rotationQueue(candidates, SeededRng(seed)).first()
}

fun matchId(roundIndex: Int, courtNumber: Int): String = "r$roundIndex-c$courtNumber"

// ---------------------------------------------------------------------------------------------
// Team formation
// ---------------------------------------------------------------------------------------------

private data class Foursome(val teamA: Team, val teamB: Team)

/**
 * Try [CANDIDATE_ARRANGEMENTS] seeded shuffles of the selected players and keep the one that repeats
 * the fewest partnerships and match-ups.
 *
 * Deliberately not a naive pairing of the queue in order: that would lock the same four
 * least-played people together round after round, which is fair on games but miserable to play.
 */
private fun bestArrangement(
    selected: List<Player>,
    state: SessionState,
    rng: SeededRng,
): List<Foursome> {
    var best: List<Foursome>? = null
    var bestCost = Int.MAX_VALUE
    var tiesSeen = 0

    repeat(CANDIDATE_ARRANGEMENTS) {
        val candidate = rng.shuffled(selected).chunked(4).map { four ->
            Foursome(
                teamA = Team(four[0].id, four[1].id),
                teamB = Team(four[2].id, four[3].id),
            )
        }
        val cost = arrangementCost(candidate, state)

        when {
            cost < bestCost -> {
                best = candidate
                bestCost = cost
                tiesSeen = 1
            }
            // Reservoir sampling over equal-cost arrangements: every tied candidate is equally
            // likely to win, decided by the seed rather than by iteration order.
            cost == bestCost -> {
                tiesSeen++
                if (rng.oneIn(tiesSeen)) best = candidate
            }
        }
    }

    return best ?: emptyList()
}

/** Lower is better. Zero means nobody in this arrangement has partnered or faced each other before. */
private fun arrangementCost(arrangement: List<Foursome>, state: SessionState): Int {
    var partnerCost = 0
    var opponentCost = 0

    for (foursome in arrangement) {
        partnerCost += partnerCount(state, foursome.teamA.first, foursome.teamA.second)
        partnerCost += partnerCount(state, foursome.teamB.first, foursome.teamB.second)

        for (a in foursome.teamA.ids) {
            for (b in foursome.teamB.ids) {
                opponentCost += opponentCount(state, a, b)
            }
        }
    }

    return PARTNER_WEIGHT * partnerCost + OPPONENT_WEIGHT * opponentCost
}

private fun partnerCount(state: SessionState, a: String, b: String): Int =
    state.player(a)?.partnerHistory?.get(b) ?: 0

private fun opponentCount(state: SessionState, a: String, b: String): Int =
    state.player(a)?.opponentHistory?.get(b) ?: 0
