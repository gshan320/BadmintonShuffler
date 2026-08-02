package com.example.badmintonshuffler.engine

import com.example.badmintonshuffler.model.SessionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests 1-4: capacity, long-run fairness, starvation, partner variety.
 *
 * These encode the product promise. If one fails, the engine is wrong — not the test.
 */
class RotationTest {

    // --- Test 1: capacity -----------------------------------------------------------------------

    @Test
    fun `two courts and eleven players puts eight on court and three on the bench`() {
        val round = generateRound(activeSession(playerCount = 11, courts = 2), seed = SIM_SEED)

        assertEquals(2, round.matches.size)
        assertEquals(8, round.playingIds.size)
        assertEquals(3, round.sittingOut.size)
        assertEquals(listOf(1, 2), round.matches.map { it.courtNumber })
    }

    @Test
    fun `two courts and seven players can only fill one court`() {
        val round = generateRound(activeSession(playerCount = 7, courts = 2), seed = SIM_SEED)

        assertEquals(1, round.matches.size)
        assertEquals(4, round.playingIds.size)
        assertEquals(3, round.sittingOut.size)
    }

    @Test
    fun `fewer than four players means nobody plays and nobody is lost`() {
        val round = generateRound(activeSession(playerCount = 3, courts = 2), seed = SIM_SEED)

        assertTrue(round.matches.isEmpty())
        assertEquals(3, round.sittingOut.size)
    }

    @Test
    fun `nobody is ever scheduled on two courts at once`() {
        val round = generateRound(activeSession(playerCount = 14, courts = 3), seed = SIM_SEED)

        assertEquals(12, round.playingIds.size)
        assertEquals(12, round.playingIds.toSet().size)
    }

    // --- Test 2: long-run fairness --------------------------------------------------------------

    @Test
    fun `over twenty rounds the games-played spread never exceeds one`() {
        val history = simulate(activeSession(playerCount = 14, courts = 3), rounds = 20)

        history.forEachIndexed { index, state ->
            val report = getFairnessReport(state)
            assertTrue(
                "Round ${index + 1} had a spread of ${report.spread}:\n${distributionTable(state)}",
                report.spread <= 1,
            )
        }

        // Printed so a human can eyeball the distribution rather than trusting the assertion.
        println("Games-played distribution after 20 rounds (3 courts, 14 players):")
        println(distributionTable(history.last()))
    }

    @Test
    fun `total games dealt matches the courts available`() {
        val history = simulate(activeSession(playerCount = 14, courts = 3), rounds = 20)
        val totalGames = history.last().players.sumOf { it.gamesPlayed }

        // 3 courts x 4 players x 20 rounds.
        assertEquals(3 * 4 * 20, totalGames)
    }

    // --- Test 3: no starvation ------------------------------------------------------------------

    @Test
    fun `no player ever sits out three rounds in a row`() {
        val history = simulate(activeSession(playerCount = 14, courts = 3), rounds = 20)

        history.forEachIndexed { index, state ->
            val stuck = state.activePlayers.filter { it.restStreak >= 3 }
            assertTrue(
                "After round ${index + 1} these players had sat out ${stuck.map { it.restStreak }}: " +
                    stuck.map { it.name },
                stuck.isEmpty(),
            )
        }
    }

    @Test
    fun `nobody starves even when the roster barely exceeds capacity`() {
        // 9 players on 2 courts: one person sits every single round, so the queue has to circulate.
        val history = simulate(activeSession(playerCount = 9, courts = 2), rounds = 18)

        history.forEach { state ->
            assertTrue(state.activePlayers.none { it.restStreak >= 3 })
        }
        assertTrue(getFairnessReport(history.last()).spread <= 1)
    }

    // --- Test 4: partner variety ----------------------------------------------------------------

    @Test
    fun `no pair partners a third time before every pair has partnered once`() {
        val players = (1..8).map { "P$it" }
        val allPairs = players.flatMapIndexed { i, a ->
            players.drop(i + 1).map { b -> pairKey(a, b) }
        }.toSet()
        assertEquals(28, allPairs.size)

        val counts = mutableMapOf<Pair<String, String>, Int>()
        var state: SessionState = activeSession(playerCount = 8, courts = 2)
        val resultRng = SeededRng(SIM_SEED xor 0x5EED)

        repeat(10) { round ->
            state = simulateRound(state, SIM_SEED + round, resultRng)
            val current = state

            current.currentRound?.matches?.forEach { match ->
                listOf(match.teamA, match.teamB).forEach { team ->
                    val key = pairKey(
                        current.player(team.first)!!.name,
                        current.player(team.second)!!.name,
                    )
                    val next = (counts[key] ?: 0) + 1
                    counts[key] = next

                    if (next == 3) {
                        val unpartnered = allPairs.filter { (counts[it] ?: 0) == 0 }
                        assertTrue(
                            "Pair $key partnered a 3rd time in round ${round + 1} while " +
                                "${unpartnered.size} pairs had never partnered: $unpartnered",
                            unpartnered.isEmpty(),
                        )
                    }
                }
            }
        }

        println("Distinct partnerships formed in 10 rounds: ${counts.size} of 28 possible")
    }

    @Test
    fun `the engine prefers a fresh partner over a repeat when one is available`() {
        // After a round, the same four people should not be paired the same way again immediately.
        var state: SessionState = activeSession(playerCount = 8, courts = 2)
        val resultRng = SeededRng(SIM_SEED)
        state = simulateRound(state, SIM_SEED, resultRng)
        val firstRoundTeams = state.currentRound!!.matches
            .flatMap { listOf(it.teamA, it.teamB) }
            .map { pairKey(it.first, it.second) }
            .toSet()

        state = simulateRound(state, SIM_SEED + 1, resultRng)
        val secondRoundTeams = state.currentRound!!.matches
            .flatMap { listOf(it.teamA, it.teamB) }
            .map { pairKey(it.first, it.second) }
            .toSet()

        assertTrue(
            "Round 2 reused partnerships from round 1: ${firstRoundTeams intersect secondRoundTeams}",
            (firstRoundTeams intersect secondRoundTeams).isEmpty(),
        )
    }

    // --- Determinism ----------------------------------------------------------------------------

    @Test
    fun `the same seed always produces the same round`() {
        val state = activeSession(playerCount = 11, courts = 2)

        val first = generateRound(state, seed = 4242)
        val second = generateRound(state, seed = 4242)
        val different = generateRound(state, seed = 4243)

        assertEquals(first, second)
        assertTrue("Different seeds should not produce identical rounds", first != different)
    }
}
