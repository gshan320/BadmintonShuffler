package com.example.badmintonshuffler.engine

import com.example.badmintonshuffler.model.MatchStatus
import com.example.badmintonshuffler.model.Player
import com.example.badmintonshuffler.model.SessionConfig
import com.example.badmintonshuffler.model.SessionState
import com.example.badmintonshuffler.model.SessionStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests 8-9: recording a result, and ranking the people who earned it.
 */
class ScoringTest {

    private fun oneMatchSession(): SessionState =
        startNextRound(activeSession(playerCount = 4, courts = 1), SIM_SEED)

    private fun accept(outcome: ScoreOutcome): SessionState = when (outcome) {
        is ScoreOutcome.Accepted -> outcome.state
        is ScoreOutcome.Rejected -> error("Expected the score to be accepted, got ${outcome.reason}")
    }

    // --- Test 8: scoring --------------------------------------------------------------------------

    @Test
    fun `a 21-15 win credits both winners identically and both losers identically`() {
        val state = oneMatchSession()
        val match = state.currentRound!!.matches.single()

        val after = accept(recordResult(state, match.id, scoreA = 21, scoreB = 15))

        val winners = match.teamA.ids.map { after.player(it)!! }
        val losers = match.teamB.ids.map { after.player(it)!! }

        winners.forEach { p ->
            assertEquals(1, p.wins)
            assertEquals(0, p.losses)
            assertEquals(21, p.pointsFor)
            assertEquals(15, p.pointsAgainst)
            assertEquals(1, p.gamesPlayed)
            assertEquals(0, p.restStreak)
            assertEquals(after.config.pointsPerWin, sessionPoints(p, after.config))
        }
        // Identical, to the point of being interchangeable.
        assertEquals(sessionPoints(winners[0], after.config), sessionPoints(winners[1], after.config))

        losers.forEach { p ->
            assertEquals(0, p.wins)
            assertEquals(1, p.losses)
            assertEquals(15, p.pointsFor)
            assertEquals(21, p.pointsAgainst)
            assertEquals(after.config.pointsPerLoss, sessionPoints(p, after.config))
        }

        assertEquals(MatchStatus.COMPLETED, after.match(match.id)!!.status)
        assertEquals(match.teamA, after.match(match.id)!!.winner)
    }

    @Test
    fun `recording a result increments partner and opponent histories`() {
        val state = oneMatchSession()
        val match = state.currentRound!!.matches.single()
        val (a1, a2) = match.teamA.first to match.teamA.second
        val (b1, b2) = match.teamB.first to match.teamB.second

        val after = accept(recordResult(state, match.id, 21, 15))

        assertEquals(1, after.player(a1)!!.partnerHistory[a2])
        assertEquals(1, after.player(a2)!!.partnerHistory[a1])
        assertNull("Opponents are not partners", after.player(a1)!!.partnerHistory[b1])

        listOf(b1, b2).forEach { opponent ->
            assertEquals(1, after.player(a1)!!.opponentHistory[opponent])
            assertEquals(1, after.player(a2)!!.opponentHistory[opponent])
        }
        listOf(a1, a2).forEach { opponent ->
            assertEquals(1, after.player(b1)!!.opponentHistory[opponent])
        }
    }

    @Test
    fun `equal negative and duplicate scores are rejected`() {
        val state = oneMatchSession()
        val match = state.currentRound!!.matches.single()

        assertEquals(
            ScoreRejection.EQUAL_SCORES,
            (recordResult(state, match.id, 21, 21) as ScoreOutcome.Rejected).reason,
        )
        assertEquals(
            ScoreRejection.NEGATIVE_SCORE,
            (recordResult(state, match.id, -1, 21) as ScoreOutcome.Rejected).reason,
        )
        assertEquals(
            ScoreRejection.MATCH_NOT_FOUND,
            (recordResult(state, "nope", 21, 15) as ScoreOutcome.Rejected).reason,
        )

        // A score recorded twice — the fast double-tap case — must not count twice.
        val once = accept(recordResult(state, match.id, 21, 15))
        assertEquals(
            ScoreRejection.MATCH_NOT_PENDING,
            (recordResult(once, match.id, 21, 15) as ScoreOutcome.Rejected).reason,
        )
        assertTrue(once.players.filter { it.gamesPlayed > 0 }.all { it.gamesPlayed == 1 })
    }

    @Test
    fun `a score below the target is allowed but flagged`() {
        val config = SessionConfig(targetScore = 21)

        assertTrue(checkScore(config, 21, 15).isAcceptable)
        assertTrue("21 reaches the target", !checkScore(config, 21, 15).belowTarget)
        assertTrue("A hall closing early is a real thing", checkScore(config, 14, 11).isAcceptable)
        assertTrue(checkScore(config, 14, 11).belowTarget)
    }

    @Test
    fun `amending a mis-entered score replaces it rather than stacking on top`() {
        val state = oneMatchSession()
        val match = state.currentRound!!.matches.single()

        val typo = accept(recordResult(state, match.id, 21, 51))
        val fixed = accept(amendResult(typo, match.id, 21, 15))

        val winner = fixed.player(match.teamA.first)!!
        assertEquals(1, winner.gamesPlayed)
        assertEquals(1, winner.wins)
        assertEquals(0, winner.losses)
        assertEquals(21, winner.pointsFor)
        assertEquals(15, winner.pointsAgainst)
        assertEquals(1, winner.partnerHistory[match.teamA.second])
    }

    @Test
    fun `voiding a match gives nobody credit`() {
        val state = oneMatchSession()
        val match = state.currentRound!!.matches.single()

        val scored = accept(recordResult(state, match.id, 21, 15))
        val voided = voidMatch(scored, match.id)

        assertEquals(MatchStatus.VOIDED, voided.match(match.id)!!.status)
        assertTrue(voided.players.all { it.gamesPlayed == 0 && it.wins == 0 && it.losses == 0 })
        assertTrue(voided.players.all { it.partnerHistory.isEmpty() && it.opponentHistory.isEmpty() })
        // A round with only voided matches is still finished — the court is free, move on.
        assertTrue(voided.currentRound!!.isFullyScored)
    }

    // --- Test 9: leaderboard ties -----------------------------------------------------------------

    private fun playerWith(
        name: String,
        wins: Int,
        losses: Int,
        pointsFor: Int,
        pointsAgainst: Int,
    ) = Player(
        id = name,
        name = name,
        wins = wins,
        losses = losses,
        gamesPlayed = wins + losses,
        pointsFor = pointsFor,
        pointsAgainst = pointsAgainst,
    )

    @Test
    fun `two players tied for first are both first and the next is third`() {
        val state = SessionState(
            status = SessionStatus.ENDED,
            players = listOf(
                playerWith("Bea", wins = 3, losses = 1, pointsFor = 80, pointsAgainst = 60),
                playerWith("Chris", wins = 1, losses = 3, pointsFor = 60, pointsAgainst = 80),
                playerWith("Ana", wins = 3, losses = 1, pointsFor = 80, pointsAgainst = 60),
            ),
        )

        val board = buildLeaderboard(state)

        assertEquals(listOf("Ana", "Bea", "Chris"), board.map { it.player.name })
        assertEquals(listOf(1, 1, 3), board.map { it.rank })
        assertEquals(board[0].sessionPoints, board[1].sessionPoints)
    }

    @Test
    fun `the tiebreak chain runs points then differential then win rate`() {
        // With the default 3 per win and 1 per loss, 2W-2L and 1W-5L both score 8 session points,
        // so the chain has to keep going: differential first, then win rate.
        val state = SessionState(
            players = listOf(
                playerWith("Grinder", wins = 1, losses = 5, pointsFor = 110, pointsAgainst = 80),
                playerWith("LowDiff", wins = 2, losses = 2, pointsFor = 70, pointsAgainst = 68),
                playerWith("Sharp", wins = 2, losses = 2, pointsFor = 80, pointsAgainst = 50),
            ),
        )

        val board = buildLeaderboard(state)

        assertEquals(listOf(8, 8, 8), board.map { it.sessionPoints })
        // Sharp and Grinder both have +30, so differential separates them from LowDiff (+2)...
        assertEquals(listOf(30, 30, 2), board.map { it.pointDifferential })
        // ...and win rate separates Sharp (0.50) from Grinder (0.17).
        assertEquals(listOf("Sharp", "Grinder", "LowDiff"), board.map { it.player.name })
        assertEquals(listOf(1, 2, 3), board.map { it.rank })
    }

    @Test
    fun `players who left keep their points on the leaderboard`() {
        val state = SessionState(
            players = listOf(
                playerWith("Stayed", wins = 1, losses = 2, pointsFor = 60, pointsAgainst = 70),
                playerWith("Left", wins = 3, losses = 0, pointsFor = 63, pointsAgainst = 40)
                    .copy(isActive = false),
            ),
        )

        val board = buildLeaderboard(state)

        assertEquals("Left", board.first().player.name)
        assertEquals(1, board.first().rank)
        assertEquals(9, board.first().sessionPoints)
    }
}
