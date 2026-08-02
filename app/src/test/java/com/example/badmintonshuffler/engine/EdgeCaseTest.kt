package com.example.badmintonshuffler.engine

import com.example.badmintonshuffler.model.MatchStatus
import com.example.badmintonshuffler.model.SessionState
import com.example.badmintonshuffler.model.SessionStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

/**
 * The situations a real Tuesday evening produces: people leaving, people arriving, not quite enough
 * for a court, and the hall closing while a game is still on.
 *
 * The rule for all of these is the same — never produce a broken round, and never lose anybody.
 */
class EdgeCaseTest {

    @Test
    fun `fewer than four active players mid-session produces an empty round, not a crash`() {
        var state = simulate(activeSession(playerCount = 8, courts = 2), rounds = 3).last()

        // Five people go home at once.
        state.activePlayers.take(5).forEach { state = removePlayer(state, it.id).state }
        assertEquals(3, state.activePlayers.size)

        val round = generateRound(state, SIM_SEED)

        assertTrue(round.matches.isEmpty())
        assertEquals(3, round.sittingOut.size)
        // Everyone still active is accounted for — nobody silently vanishes.
        assertEquals(
            state.activePlayers.map { it.id }.toSet(),
            round.sittingOut.toSet(),
        )

        // Advancing is still allowed — the app does not trap the organiser on a dead round. It
        // deals an empty one and the screen explains why the courts are bare.
        val advanced = startNextRound(state, SIM_SEED + 1)
        assertTrue(advanced.currentRound!!.matches.isEmpty())
        assertEquals(3, advanced.activePlayers.size)
    }

    @Test
    fun `exactly four players and three courts uses one court and leaves two idle`() {
        val state = activeSession(playerCount = 4, courts = 3)

        val round = generateRound(state, SIM_SEED)

        assertEquals(1, round.matches.size)
        assertEquals(1, round.matches.single().courtNumber)
        assertTrue(round.sittingOut.isEmpty())
        assertEquals(1, state.usableCourts)
    }

    @Test
    fun `removing every player leaves a coherent, empty session`() {
        var state = simulate(activeSession(playerCount = 8, courts = 2), rounds = 2).last()
        state.players.forEach { state = removePlayer(state, it.id).state }

        assertTrue(state.activePlayers.isEmpty())

        val round = generateRound(state, SIM_SEED)
        assertTrue(round.matches.isEmpty())
        assertTrue(round.sittingOut.isEmpty())

        val report = getFairnessReport(state)
        assertEquals(0, report.spread)
        assertTrue("An empty roster is vacuously fair, not unfair", report.isFair)

        // Their records survive.
        assertEquals(8, buildLeaderboard(state).size)
    }

    @Test
    fun `a player removed and then re-added rejoins level rather than ahead`() {
        var state = simulate(activeSession(playerCount = 9, courts = 2), rounds = 6).last()
        val player = state.activePlayers.maxByOrNull { it.gamesPlayed }!!
        val gamesWhenTheyLeft = player.gamesPlayed

        state = removePlayer(state, player.id).state
        state = simulate(state, rounds = 4, seedBase = SIM_SEED + 50).last()

        // While they were away, everyone else kept playing.
        assertTrue(state.activePlayers.all { it.gamesPlayed >= gamesWhenTheyLeft })

        state = reinstatePlayer(state, player.id)
        val back = state.player(player.id)!!

        assertTrue(back.isActive)
        assertEquals(gamesWhenTheyLeft, back.gamesPlayed)
        // Their queue credit lifts them to the level of the least-played player, so they neither
        // hog the court on return nor get frozen out by their own history.
        assertEquals(
            state.activePlayers.filter { it.id != back.id }.minOf { it.gamesPlayed },
            back.effectiveGames,
        )

        state = simulate(state, rounds = 6, seedBase = SIM_SEED + 90).last()
        assertTrue(
            "Spread was ${getFairnessReport(state).spread} after a return:\n" +
                distributionTable(state),
            getFairnessReport(state).spread <= 1,
        )
    }

    @Test
    fun `the session running past its end time reports zero rounds left, not negative`() {
        val state = activeSession(playerCount = 8, courts = 2).let {
            it.copy(config = it.config.copy(endTime = LocalTime.of(20, 0), minutesPerGame = 12))
        }

        assertEquals(0, estimateRemainingRounds(state, LocalTime.of(20, 30)))
        assertEquals(0, estimateRemainingRounds(state, LocalTime.of(23, 59)))
        // And a round can still be dealt — the app does not stop you playing on.
        assertEquals(2, generateRound(state, SIM_SEED).matches.size)
    }

    @Test
    fun `a round generated when everyone has identical stats is still valid and deterministic`() {
        val state = activeSession(playerCount = 12, courts = 3)
        assertTrue(state.players.all { it.gamesPlayed == 0 && it.restStreak == 0 })

        val round = generateRound(state, SIM_SEED)

        assertEquals(3, round.matches.size)
        assertEquals(12, round.playingIds.toSet().size)
        assertEquals(round, generateRound(state, SIM_SEED))
    }

    @Test
    fun `zero minutes per game does not divide by zero`() {
        val state = activeSession(playerCount = 8, courts = 2).let {
            it.copy(config = it.config.copy(minutesPerGame = 0))
        }

        assertEquals(0, estimateRemainingRounds(state, LocalTime.of(18, 0)))
        assertEquals(0, state.config.estimatedTotalRounds)
    }

    @Test
    fun `an end time before the start time yields zero duration rather than a negative one`() {
        val config = activeSession(playerCount = 8, courts = 2).config
            .copy(startTime = LocalTime.of(20, 0), endTime = LocalTime.of(18, 0))

        assertEquals(0, config.durationMinutes)
        assertFalse(canStartSession(SessionState(config = config)))
    }

    @Test
    fun `a voided match frees the round to advance without crediting anyone`() {
        var state = startNextRound(activeSession(playerCount = 8, courts = 2), SIM_SEED)
        val matches = state.currentRound!!.matches

        state = (recordResult(state, matches[0].id, 21, 15) as ScoreOutcome.Accepted).state
        assertFalse("A pending match should block the next round", canAdvanceRound(state))

        state = voidMatch(state, matches[1].id)

        assertTrue(canAdvanceRound(state))
        assertEquals(MatchStatus.VOIDED, state.match(matches[1].id)!!.status)
        matches[1].playerIds.forEach { id ->
            assertEquals(0, state.player(id)!!.gamesPlayed)
        }
    }

    @Test
    fun `substituting into a match that is already scored is refused`() {
        var state = startNextRound(activeSession(playerCount = 12, courts = 2), SIM_SEED)
        val match = state.currentRound!!.matches.first()
        val resting = state.currentRound!!.sittingOut.first()
        state = (recordResult(state, match.id, 21, 15) as ScoreOutcome.Accepted).state

        val attempted = substituteInPendingMatch(state, match.teamA.first, resting)

        assertEquals(state, attempted)
    }

    @Test
    fun `a session can survive a whole afternoon of churn`() {
        // Eight rounds, someone leaves, two arrive, someone comes back, another eight rounds.
        var state: SessionState = activeSession(playerCount = 10, courts = 2)
        state = simulate(state, rounds = 8).last()

        val leaver = state.activePlayers.first()
        state = removePlayer(state, leaver.id).state
        state = addPlayer(state, "Late One")
        state = addPlayer(state, "Late Two")
        state = simulate(state, rounds = 4, seedBase = SIM_SEED + 400).last()
        state = reinstatePlayer(state, leaver.id)
        state = simulate(state, rounds = 8, seedBase = SIM_SEED + 800).last()

        // Nobody was lost, nobody is starved, and the active roster is still even.
        assertEquals(12, state.players.size)
        assertEquals(12, state.activePlayers.size)
        assertTrue(state.activePlayers.none { it.restStreak >= 3 })
        assertTrue(
            "Final spread ${getFairnessReport(state).spread}:\n${distributionTable(state)}",
            getFairnessReport(state).spread <= 1,
        )

        // Every round dealt is internally consistent.
        state.rounds.forEach { round ->
            assertEquals(round.playingIds.size, round.playingIds.toSet().size)
            assertTrue((round.playingIds intersect round.sittingOut.toSet()).isEmpty())
            round.matches.forEach { match -> assertNotNull(state.player(match.teamA.first)) }
        }

        println("Afternoon-of-churn final table:\n${distributionTable(state)}")
    }
}
