package com.example.badmintonshuffler.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import com.example.badmintonshuffler.engine.FairnessReport
import com.example.badmintonshuffler.model.Match
import com.example.badmintonshuffler.model.MatchStatus
import com.example.badmintonshuffler.model.Round
import com.example.badmintonshuffler.model.SessionState
import com.example.badmintonshuffler.state.SessionProgress
import com.example.badmintonshuffler.ui.KeepScreenOn
import com.example.badmintonshuffler.ui.component.CourtCard
import com.example.badmintonshuffler.ui.component.CourtCardState
import com.example.badmintonshuffler.ui.component.CourtGlyph
import com.example.badmintonshuffler.ui.component.CourtIcon
import com.example.badmintonshuffler.ui.component.CourtSide
import com.example.badmintonshuffler.ui.component.EmptyState
import com.example.badmintonshuffler.ui.component.FairnessPill
import com.example.badmintonshuffler.ui.component.PrimaryButton
import com.example.badmintonshuffler.ui.component.RestingStrip
import com.example.badmintonshuffler.ui.component.Screen
import com.example.badmintonshuffler.ui.component.SecondaryButton
import com.example.badmintonshuffler.ui.theme.CourtColors
import com.example.badmintonshuffler.ui.theme.CourtType
import com.example.badmintonshuffler.ui.theme.Radius
import com.example.badmintonshuffler.ui.theme.Sizes
import com.example.badmintonshuffler.ui.theme.Space

/**
 * The screen the organiser stares at for three hours.
 *
 * Calm on purpose: it is read at a glance from across a court, so nothing here moves, blinks or
 * competes for attention except the one thing that just changed.
 */
@Composable
fun SessionScreen(
    state: SessionState,
    progress: SessionProgress,
    fairness: FairnessReport,
    canAdvance: Boolean,
    onOpenMatch: (Match) -> Unit,
    onNextRound: () -> Unit,
    onEditPlayers: () -> Unit,
    onOpenFairness: () -> Unit,
    onOpenLeaderboard: () -> Unit,
    onEndSession: () -> Unit,
    modifier: Modifier = Modifier,
) {
    KeepScreenOn()

    val round = state.currentRound
    var menuOpen by remember { mutableStateOf(false) }

    Screen(
        modifier = modifier,
        bottomBar = {
            Row(horizontalArrangement = Arrangement.spacedBy(Space.md)) {
                SecondaryButton(
                    text = "Edit players",
                    onClick = onEditPlayers,
                    modifier = Modifier.weight(1f),
                )
                PrimaryButton(
                    text = "Next round",
                    onClick = onNextRound,
                    enabled = canAdvance,
                    modifier = Modifier.weight(1f),
                )
            }
            if (!canAdvance) {
                Spacer(Modifier.height(Space.sm))
                Text(
                    text = pendingExplanation(round),
                    style = CourtType.Caption,
                    color = CourtColors.Chalk60,
                )
            }
        },
    ) {
        // --- Header ---
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = Space.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Round ${(round?.index ?: 0) + 1}",
                    style = CourtType.RoundTitle,
                    color = CourtColors.CourtLine,
                )
                Text(
                    text = progressLine(progress),
                    style = CourtType.Caption,
                    color = CourtColors.Chalk60,
                )
            }

            FairnessPill(
                spread = fairness.spread,
                isFair = fairness.isFair,
                onClick = onOpenFairness,
            )

            Box {
                Box(
                    modifier = Modifier
                        .size(Sizes.minTapTarget)
                        .clickable(role = Role.Button) { menuOpen = true },
                    contentAlignment = Alignment.Center,
                ) {
                    CourtIcon(
                        glyph = CourtGlyph.OVERFLOW,
                        contentDescription = "More options",
                        tint = CourtColors.CourtLine,
                    )
                }
                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false },
                    containerColor = CourtColors.ServiceBox,
                ) {
                    DropdownMenuItem(
                        text = { Text("Leaderboard", style = CourtType.Body17) },
                        onClick = { menuOpen = false; onOpenLeaderboard() },
                    )
                    DropdownMenuItem(
                        text = {
                            Text("End session", style = CourtType.Body17, color = CourtColors.FaultRed)
                        },
                        onClick = { menuOpen = false; onEndSession() },
                    )
                }
            }
        }

        Spacer(Modifier.height(Space.xl))

        // --- Courts ---
        if (round == null || round.matches.isEmpty()) {
            EmptyState(
                title = "No courts in play",
                body = emptyCourtsReason(state),
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(Space.md)) {
                round.matches.forEach { match ->
                    CourtCard(
                        courtNumber = match.courtNumber,
                        top = match.teamA.toSide(state, match),
                        bottom = match.teamB.toSide(state, match),
                        state = when (match.status) {
                            MatchStatus.PENDING -> CourtCardState.PENDING
                            MatchStatus.COMPLETED -> CourtCardState.COMPLETED
                            MatchStatus.VOIDED -> CourtCardState.VOIDED
                        },
                        onClick = if (match.status == MatchStatus.VOIDED) null
                        else ({ onOpenMatch(match) }),
                    )
                }
            }
        }

        Spacer(Modifier.height(Space.xl))

        // --- Resting ---
        RestingStrip(
            names = round?.sittingOut.orEmpty().mapNotNull { state.player(it)?.name },
            modifier = Modifier
                .background(CourtColors.NetTape, RoundedCornerShape(Radius.md))
                .padding(Space.lg),
        )

        Spacer(Modifier.height(Space.xxl))
    }
}

private fun com.example.badmintonshuffler.model.Team.toSide(
    state: SessionState,
    match: Match,
): CourtSide {
    val isTeamA = match.teamA == this
    val score = if (isTeamA) match.scoreA else match.scoreB
    val won = match.winner == this
    return CourtSide(
        playerNames = ids.map { state.player(it)?.name ?: "—" },
        score = score,
        pointsEarned = if (match.status == MatchStatus.COMPLETED) {
            if (won) state.config.pointsPerWin else state.config.pointsPerLoss
        } else {
            null
        },
    )
}

private fun progressLine(progress: SessionProgress): String {
    val ends = progress.endTime.display()
    return when {
        progress.isPastEndTime -> "Past the booked time — ends $ends"
        progress.estimatedRoundsRemaining == 0 -> "Last round — ends $ends"
        progress.estimatedRoundsRemaining == 1 -> "About 1 round left, ends $ends"
        else -> "About ${progress.estimatedRoundsRemaining} rounds left, ends $ends"
    }
}

private fun pendingExplanation(round: Round?): String {
    val pending = round?.matches.orEmpty().count { it.status == MatchStatus.PENDING }
    return when (pending) {
        0 -> "Waiting for the round to be dealt."
        1 -> "One court still needs a score before the next round."
        else -> "$pending courts still need a score before the next round."
    }
}

/** A court is never just blank — the screen always says why. */
private fun emptyCourtsReason(state: SessionState): String {
    val active = state.activePlayers.size
    return when {
        active == 0 -> "Nobody is active. Add players to start a round."
        active < 4 -> "Only $active ${if (active == 1) "player" else "players"} available — " +
            "doubles needs 4. Add someone, or bring back a player who left."
        else -> "The next round has not been dealt yet."
    }
}
