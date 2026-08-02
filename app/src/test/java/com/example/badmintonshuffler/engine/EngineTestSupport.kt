package com.example.badmintonshuffler.engine

import com.example.badmintonshuffler.model.Match
import com.example.badmintonshuffler.model.Player
import com.example.badmintonshuffler.model.SessionConfig
import com.example.badmintonshuffler.model.SessionState
import com.example.badmintonshuffler.model.SessionStatus

/**
 * Shared fixtures for the engine tests.
 *
 * Everything here is deterministic: given the same seeds, a simulation replays exactly. That is the
 * whole point of [SeededRng] — a fairness claim you can only verify sometimes is not a claim.
 */

const val SIM_SEED = 20260803

fun setupSession(playerCount: Int, courts: Int): SessionState {
    var state = SessionState(config = SessionConfig(courtCount = courts))
    repeat(playerCount) { i -> state = addPlayer(state, "P${i + 1}") }
    return state
}

/** A session already flipped to active, with no rounds dealt yet. */
fun activeSession(playerCount: Int, courts: Int): SessionState =
    setupSession(playerCount, courts).copy(status = SessionStatus.ACTIVE)

/**
 * Deal a round and record a result for every match on it.
 *
 * [resultRng] decides the winners, so results are reproducible but not correlated with the shuffle.
 */
fun simulateRound(
    state: SessionState,
    roundSeed: Int,
    resultRng: SeededRng,
    scoreFor: (Match) -> Pair<Int, Int> = { defaultScore(resultRng) },
): SessionState {
    var next = startNextRound(state, roundSeed)
    val matchIds = next.currentRound?.matches?.map { it.id }.orEmpty()

    for (id in matchIds) {
        val match = next.match(id) ?: continue
        val (a, b) = scoreFor(match)
        next = when (val outcome = recordResult(next, id, a, b)) {
            is ScoreOutcome.Accepted -> outcome.state
            is ScoreOutcome.Rejected -> error("Simulation produced an invalid score: ${outcome.reason}")
        }
    }
    return next
}

/** A plausible 21-x scoreline, winner chosen by the seeded RNG. */
fun defaultScore(rng: SeededRng): Pair<Int, Int> {
    val loserScore = 12 + rng.nextInt(9) // 12..20
    return if (rng.nextFloat() < 0.5f) 21 to loserScore else loserScore to 21
}

/** Run [rounds] rounds from [state], returning every intermediate state including the last. */
fun simulate(state: SessionState, rounds: Int, seedBase: Int = SIM_SEED): List<SessionState> {
    val resultRng = SeededRng(seedBase xor 0x5EED)
    val history = mutableListOf<SessionState>()
    var current = state
    repeat(rounds) { r ->
        current = simulateRound(current, seedBase + r, resultRng)
        history += current
    }
    return history
}

fun SessionState.gamesByName(): Map<String, Int> =
    players.associate { it.name to it.gamesPlayed }

fun SessionState.named(name: String): Player =
    players.first { it.name == name }

/** Unordered pair key, so P1+P2 and P2+P1 are the same partnership. */
fun pairKey(a: String, b: String): Pair<String, String> =
    if (a <= b) a to b else b to a

/** A readable games-played distribution, printed by the long-run fairness test. */
fun distributionTable(state: SessionState): String {
    val header = "%-6s %6s %6s %5s %5s %6s".format("player", "games", "rest", "W", "L", "pts")
    val rows = state.players.sortedBy { it.name.padStart(4, '0') }.joinToString("\n") { p ->
        "%-6s %6d %6d %5d %5d %6d".format(
            p.name,
            p.gamesPlayed,
            p.restStreak,
            p.wins,
            p.losses,
            sessionPoints(p, state.config),
        )
    }
    val report = getFairnessReport(state)
    return "$header\n$rows\nspread=${report.spread} fair=${report.isFair}"
}
