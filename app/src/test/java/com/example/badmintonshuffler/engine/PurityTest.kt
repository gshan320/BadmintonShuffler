package com.example.badmintonshuffler.engine

import com.example.badmintonshuffler.model.SessionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.time.LocalTime

/**
 * Test 10: the engine never mutates what it is given.
 *
 * This matters beyond tidiness — the ViewModel hands the engine the state that Compose is currently
 * rendering. A hidden mutation would change the UI's data underneath it without a recomposition,
 * producing a screen that disagrees with itself.
 */
class PurityTest {

    @Test
    fun `recordResult leaves the input state untouched`() {
        val before = startNextRound(activeSession(playerCount = 8, courts = 2), SIM_SEED)
        val snapshot = before.deepCopyForComparison()
        val matchId = before.currentRound!!.matches.first().id

        val outcome = recordResult(before, matchId, 21, 15)

        assertEquals(snapshot, before.deepCopyForComparison())
        assertNotEquals(before, (outcome as ScoreOutcome.Accepted).state)
    }

    @Test
    fun `addPlayer leaves the input state untouched`() {
        val before = simulate(activeSession(playerCount = 8, courts = 2), rounds = 3).last()
        val snapshot = before.deepCopyForComparison()

        val after = addPlayer(before, "Late")

        assertEquals(snapshot, before.deepCopyForComparison())
        assertEquals(8, before.players.size)
        assertEquals(9, after.players.size)
    }

    @Test
    fun `removePlayer and substitute leave the input state untouched`() {
        val before = startNextRound(
            simulate(activeSession(playerCount = 12, courts = 2), rounds = 3).last(),
            SIM_SEED + 1,
        )
        val snapshot = before.deepCopyForComparison()
        val onCourt = before.currentRound!!.matches.first().teamA.first
        val resting = before.currentRound!!.sittingOut.first()

        removePlayer(before, onCourt)
        substituteInPendingMatch(before, onCourt, resting)
        voidMatch(before, before.currentRound!!.matches.first().id)
        startNextRound(before, SIM_SEED + 2)
        buildLeaderboard(before)
        getFairnessReport(before)

        assertEquals(snapshot, before.deepCopyForComparison())
    }

    @Test
    fun `generateRound is a pure read of the state`() {
        val before = simulate(activeSession(playerCount = 11, courts = 2), rounds = 4).last()
        val snapshot = before.deepCopyForComparison()

        generateRound(before, SIM_SEED)

        assertEquals(snapshot, before.deepCopyForComparison())
        // Generating a round does not, by itself, add one.
        assertEquals(4, before.rounds.size)
    }

    @Test
    fun `estimateRemainingRounds only reads the clock it is handed`() {
        val state = activeSession(playerCount = 8, courts = 2)
            .let { it.copy(config = it.config.copy(endTime = LocalTime.of(20, 0), minutesPerGame = 12)) }

        assertEquals(10, estimateRemainingRounds(state, LocalTime.of(18, 0)))
        assertEquals(5, estimateRemainingRounds(state, LocalTime.of(19, 0)))
        assertEquals(0, estimateRemainingRounds(state, LocalTime.of(20, 0)))
        // Running late is not an error, it just means no more rounds.
        assertEquals(0, estimateRemainingRounds(state, LocalTime.of(21, 0)))
    }

    /**
     * A structural snapshot. The model is built from immutable data classes, so `equals` on a
     * rebuilt copy catches any in-place edit of a list or map that slipped through.
     */
    private fun SessionState.deepCopyForComparison(): String = buildString {
        append(status).append('|').append(config).append('\n')
        players.forEach { p ->
            append(p.id).append(' ').append(p.name).append(' ').append(p.isActive)
                .append(" g=").append(p.gamesPlayed)
                .append(" c=").append(p.queueCredit)
                .append(" w=").append(p.wins).append(" l=").append(p.losses)
                .append(" pf=").append(p.pointsFor).append(" pa=").append(p.pointsAgainst)
                .append(" rs=").append(p.restStreak)
                .append(" ph=").append(p.partnerHistory.toSortedMap())
                .append(" oh=").append(p.opponentHistory.toSortedMap())
                .append('\n')
        }
        rounds.forEach { r ->
            append("round ").append(r.index).append(' ').append(r.status)
                .append(" out=").append(r.sittingOut).append('\n')
            r.matches.forEach { m ->
                append("  ").append(m.id).append(' ').append(m.teamA).append(" vs ").append(m.teamB)
                    .append(' ').append(m.status).append(' ').append(m.scoreA).append('-')
                    .append(m.scoreB).append('\n')
            }
        }
    }
}
