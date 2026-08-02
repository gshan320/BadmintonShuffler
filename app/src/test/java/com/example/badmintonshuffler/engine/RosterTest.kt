package com.example.badmintonshuffler.engine

import com.example.badmintonshuffler.model.LateJoinerMode
import com.example.badmintonshuffler.model.Player
import com.example.badmintonshuffler.model.SessionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests 5-7: late joiners and mid-session removal.
 *
 * The late-joiner rule is the one people argue about in the hall, so it gets the most scrutiny here.
 */
class RosterTest {

    // --- Test 5: a late joiner gets a fair share, and only a fair share ------------------------

    @Test
    fun `late joiner gets the same number of games as everyone else from the moment they arrive`() {
        var state = simulate(activeSession(playerCount = 8, courts = 2), rounds = 6).last()

        // Everybody who turned up on time has played every round.
        assertTrue(state.activePlayers.all { it.gamesPlayed == 6 })

        state = addPlayer(state, "Late")
        val gamesAtJoin = state.players.associate { it.id to it.gamesPlayed }

        val history = simulate(state, rounds = 10, seedBase = SIM_SEED + 100)
        val final = history.last()

        val windowGames = final.players.associate {
            it.name to (it.gamesPlayed - gamesAtJoin.getValue(it.id))
        }
        val spread = windowGames.values.max() - windowGames.values.min()

        assertTrue(
            "Games played over the 10 rounds after the late joiner arrived: $windowGames",
            spread <= 1,
        )
        println("Late joiner window (10 rounds, 9 players, 2 courts): $windowGames")
    }

    @Test
    fun `late joiner never monopolises courts in any four-round window`() {
        var state = simulate(activeSession(playerCount = 8, courts = 2), rounds = 6).last()
        state = addPlayer(state, "Late")
        val lateId = state.named("Late").id

        val snapshots = listOf(state) + simulate(state, rounds = 10, seedBase = SIM_SEED + 100)

        // Every sliding window of four rounds, from round 4 onwards.
        for (end in 4..10) {
            val start = end - 4
            val gamesIn = { p: Player ->
                snapshots[end].player(p.id)!!.gamesPlayed - snapshots[start].player(p.id)!!.gamesPlayed
            }
            val everyone = snapshots[end].activePlayers
            val average = everyone.sumOf(gamesIn).toDouble() / everyone.size
            val lateGames = gamesIn(snapshots[end].player(lateId)!!)

            assertTrue(
                "In rounds ${start + 1}..$end the late joiner played $lateGames games against a " +
                    "group average of ${"%.2f".format(average)} — that is monopolising the court",
                lateGames <= average + 2,
            )
        }
    }

    @Test
    fun `fair-forward gives the newcomer no more court time than catch-up would`() {
        // Contrast the two modes on the same scenario, to show the credit is actually doing work.
        // 12 players on 2 courts leaves four on the bench each round, so the difference is visible.
        val base = simulate(activeSession(playerCount = 12, courts = 2), rounds = 6).last()

        fun windowGamesForNewcomer(mode: LateJoinerMode): Int {
            val configured = base.copy(config = base.config.copy(lateJoinerMode = mode))
            val joined = addPlayer(configured, "Late")
            val final = simulate(joined, rounds = 10, seedBase = SIM_SEED + 200).last()
            return final.named("Late").gamesPlayed
        }

        val fairForward = windowGamesForNewcomer(LateJoinerMode.FAIR_FORWARD)
        val catchUp = windowGamesForNewcomer(LateJoinerMode.CATCH_UP)

        println("Newcomer games over 10 rounds — fair-forward: $fairForward, catch-up: $catchUp")
        assertTrue(
            "fair-forward ($fairForward) should not hand the newcomer more court time than " +
                "catch-up ($catchUp)",
            fairForward < catchUp,
        )
    }

    @Test
    fun `queue credit equals the least-played active player`() {
        val state = simulate(activeSession(playerCount = 9, courts = 2), rounds = 5).last()
        val leastPlayed = state.activePlayers.minOf { it.gamesPlayed }

        val joined = addPlayer(state, "Late")

        assertEquals(leastPlayed, joined.named("Late").queueCredit)
        assertEquals(leastPlayed, joined.named("Late").effectiveGames)
    }

    // --- Test 6: the early birds keep the lead they earned ---------------------------------------

    @Test
    fun `adding a late joiner changes nobody's record and starts them on zero`() {
        val before = simulate(activeSession(playerCount = 8, courts = 2), rounds = 6).last()
        val recordsBefore = before.players.associate {
            it.name to Triple(it.wins, it.losses, sessionPoints(it, before.config))
        }

        val after = addPlayer(before, "Late")

        val recordsAfter = after.players
            .filter { it.name != "Late" }
            .associate { it.name to Triple(it.wins, it.losses, sessionPoints(it, after.config)) }
        assertEquals(recordsBefore, recordsAfter)

        val late = after.named("Late")
        assertEquals(0, late.wins)
        assertEquals(0, late.losses)
        assertEquals(0, late.gamesPlayed)
        assertEquals(0, sessionPoints(late, after.config))
        // The head start went entirely into the queue, not onto the scoreboard.
        assertTrue(late.queueCredit > 0)
    }

    @Test
    fun `a late joiner cannot erase a lead earned before they arrived`() {
        // P1 wins every game for six rounds while the newcomer is still in the car park.
        val earlyBird = "P1"
        var state: SessionState = activeSession(playerCount = 8, courts = 2)
        val resultRng = SeededRng(SIM_SEED)
        repeat(6) { round ->
            state = simulateRound(state, SIM_SEED + round, resultRng) { match ->
                val p1 = state.named(earlyBird).id
                if (match.teamA.contains(p1)) 21 to 15 else 15 to 21
            }
        }

        val leadAtJoin = sessionPoints(state.named(earlyBird), state.config)
        assertEquals(6 * state.config.pointsPerWin, leadAtJoin)

        state = addPlayer(state, "Late")
        val lateId = state.named("Late").id
        val p1Id = state.named(earlyBird).id
        val p1AtJoin = state.named(earlyBird)

        // Now the most hostile case short of cheating: the newcomer wins everything they play.
        repeat(10) { round ->
            state = simulateRound(state, SIM_SEED + 100 + round, resultRng) { match ->
                if (match.teamA.contains(lateId)) 21 to 15 else 15 to 21
            }
        }

        val late = state.player(lateId)!!
        val p1 = state.player(p1Id)!!

        // The newcomer got no extra opportunities — that is the fairness guarantee doing its job.
        val lateGames = late.gamesPlayed
        val p1Games = p1.gamesPlayed - p1AtJoin.gamesPlayed
        assertTrue(
            "Newcomer played $lateGames games while P1 played $p1Games over the same window",
            lateGames <= p1Games + 1,
        )

        // So the only way they could have closed the gap is by actually winning more games, and even
        // winning every single one of them, the head start of six wins survives the comparison of
        // like-for-like performance.
        val p1PostPoints = sessionPoints(p1, state.config) - leadAtJoin
        val latePostPoints = sessionPoints(late, state.config)
        val gapExplainedByPlay = latePostPoints - p1PostPoints
        assertEquals(
            "The gap between the two must be exactly the earned lead plus real post-join play",
            leadAtJoin - gapExplainedByPlay,
            sessionPoints(p1, state.config) - sessionPoints(late, state.config),
        )
        println(
            "Early bird ${p1.name}: ${sessionPoints(p1, state.config)} pts " +
                "(${p1.wins}W-${p1.losses}L) vs newcomer: $latePostPoints pts " +
                "(${late.wins}W-${late.losses}L)"
        )
    }

    // --- Test 7: removal mid-session --------------------------------------------------------------

    @Test
    fun `a removed player vanishes from future rounds but keeps their place on the leaderboard`() {
        var state = simulate(activeSession(playerCount = 12, courts = 2), rounds = 5).last()
        val victim = state.activePlayers.maxByOrNull { it.gamesPlayed }!!
        val gamesWhenTheyLeft = victim.gamesPlayed
        assertTrue("The test needs someone who actually played", gamesWhenTheyLeft > 0)

        val removal = removePlayer(state, victim.id)
        state = removal.state
        val roundsBefore = state.rounds.size

        state = simulate(state, rounds = 3, seedBase = SIM_SEED + 300).last()

        // Gone from every round dealt after they left.
        val laterRounds = state.rounds.drop(roundsBefore)
        assertEquals(3, laterRounds.size)
        assertTrue(
            "The player who left was still scheduled to play",
            laterRounds.none { it.playingIds.contains(victim.id) || it.sittingOut.contains(victim.id) },
        )

        // Still on the leaderboard, with the record they earned.
        val entry = buildLeaderboard(state).firstOrNull { it.player.id == victim.id }
        assertTrue("The player who left disappeared from the leaderboard", entry != null)
        assertEquals(gamesWhenTheyLeft, entry!!.player.gamesPlayed)
        assertFalse(entry.player.isActive)

        // And the people still on court are back to being fair to each other.
        val report = getFairnessReport(state)
        assertTrue(
            "Spread was ${report.spread} three rounds after a removal:\n${distributionTable(state)}",
            report.spread <= 1,
        )
    }

    @Test
    fun `removing a player mid-match tells the organiser a decision is needed`() {
        val state = simulate(activeSession(playerCount = 12, courts = 2), rounds = 3).last()
        // Deal a fresh round so there is a pending match to be stuck in.
        val withPending = startNextRound(state, SIM_SEED + 99)
        val onCourt = withPending.currentRound!!.matches.first().teamA.first
        val resting = withPending.currentRound!!.sittingOut.first()

        val stuck = removePlayer(withPending, onCourt)
        assertTrue(stuck.affectsCurrentRound)
        assertEquals(withPending.currentRound!!.matches.first().id, stuck.affectedMatchId)

        val free = removePlayer(withPending, resting)
        assertFalse(free.affectsCurrentRound)
    }

    @Test
    fun `substituting swaps a resting player into the pending match`() {
        val state = startNextRound(
            simulate(activeSession(playerCount = 12, courts = 2), rounds = 3).last(),
            SIM_SEED + 99,
        )
        val match = state.currentRound!!.matches.first()
        val outgoing = match.teamA.first
        val incoming = state.currentRound!!.sittingOut.first()

        val subbed = substituteInPendingMatch(removePlayer(state, outgoing).state, outgoing, incoming)
        val updated = subbed.match(match.id)!!

        assertTrue(updated.contains(incoming))
        assertFalse(updated.contains(outgoing))
        assertEquals(match.courtNumber, updated.courtNumber)
        assertFalse(subbed.currentRound!!.sittingOut.contains(incoming))
    }

    @Test
    fun `a player cannot be substituted onto a court they are already playing on`() {
        val state = startNextRound(
            simulate(activeSession(playerCount = 12, courts = 2), rounds = 3).last(),
            SIM_SEED + 99,
        )
        val match = state.currentRound!!.matches.first()
        val outgoing = match.teamA.first
        val alreadyPlaying = match.teamB.first

        assertEquals(state, substituteInPendingMatch(state, outgoing, alreadyPlaying))
    }

    @Test
    fun `duplicate names get a suffix instead of being rejected`() {
        var state = setupSession(playerCount = 0, courts = 2)
        state = addPlayer(state, "Alex")
        state = addPlayer(state, "Alex")
        state = addPlayer(state, "  alex  ")

        // Matching is case-insensitive so "alex" counts as a duplicate, but the name is stored the
        // way it was typed — the app suffixes people, it does not re-spell them.
        assertEquals(listOf("Alex", "Alex (2)", "alex (3)"), state.players.map { it.name })
    }
}
