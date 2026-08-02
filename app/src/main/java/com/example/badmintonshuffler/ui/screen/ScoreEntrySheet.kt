package com.example.badmintonshuffler.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.badmintonshuffler.engine.ScoreRejection
import com.example.badmintonshuffler.engine.checkScore
import com.example.badmintonshuffler.model.Match
import com.example.badmintonshuffler.model.MatchStatus
import com.example.badmintonshuffler.model.SessionState
import com.example.badmintonshuffler.ui.component.DangerButton
import com.example.badmintonshuffler.ui.component.NumberStepper
import com.example.badmintonshuffler.ui.component.PrimaryButton
import com.example.badmintonshuffler.ui.theme.CourtColors
import com.example.badmintonshuffler.ui.theme.CourtType
import com.example.badmintonshuffler.ui.theme.Radius
import com.example.badmintonshuffler.ui.theme.Sizes
import com.example.badmintonshuffler.ui.theme.Space

/**
 * Score entry: two big steppers and a row of quick fills, so a 21-15 is two taps.
 *
 * The confirm button locks itself the instant it is pressed. A fast double tap on a phone with a
 * wet screen is not a hypothetical, and the second press must not record a second game.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScoreEntrySheet(
    state: SessionState,
    match: Match,
    onConfirm: (scoreA: Int, scoreB: Int) -> ScoreRejection?,
    onVoid: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val target = state.config.targetScore
    val isCorrection = match.status == MatchStatus.COMPLETED

    var scoreA by rememberSaveable(match.id) { mutableStateOf(match.scoreA ?: target) }
    var scoreB by rememberSaveable(match.id) { mutableStateOf(match.scoreB ?: 0) }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val check = checkScore(state.config, scoreA, scoreB)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CourtColors.CourtDeep,
        scrimColor = CourtColors.NetTape.copy(alpha = 0.72f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = Space.screenGutter)
                .padding(bottom = Space.lg),
        ) {
            Text(
                text = "COURT ${match.courtNumber}",
                style = CourtType.Eyebrow,
                color = CourtColors.Chalk60,
            )
            Spacer(Modifier.height(Space.xs))
            Text(
                text = if (isCorrection) "Correct the score" else "What was the score?",
                style = CourtType.Title,
                color = CourtColors.CourtLine,
            )

            Spacer(Modifier.height(Space.xl))

            TeamScore(
                names = match.teamA.ids.mapNotNull { state.player(it)?.name },
                score = scoreA,
                onScoreChange = { scoreA = it; error = null },
                leading = scoreA > scoreB,
            )
            Spacer(Modifier.height(Space.lg))
            TeamScore(
                names = match.teamB.ids.mapNotNull { state.player(it)?.name },
                score = scoreB,
                onScoreChange = { scoreB = it; error = null },
                leading = scoreB > scoreA,
            )

            Spacer(Modifier.height(Space.xl))

            Text("QUICK FILL", style = CourtType.Eyebrow, color = CourtColors.Chalk60)
            Spacer(Modifier.height(Space.sm))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Space.sm),
                verticalArrangement = Arrangement.spacedBy(Space.sm),
            ) {
                val commonLosses = listOf(target - 2, target - 6, target - 9, target - 13)
                    .filter { it >= 0 }
                    .distinct()
                commonLosses.forEach { loss ->
                    QuickFill("$target–$loss") { scoreA = target; scoreB = loss; error = null }
                }
                commonLosses.forEach { loss ->
                    QuickFill("$loss–$target") { scoreA = loss; scoreB = target; error = null }
                }
            }

            if (check.belowTarget && check.isAcceptable) {
                Spacer(Modifier.height(Space.lg))
                Hint("Neither side reached $target. That is fine if the game was cut short.")
            }

            error?.let {
                Spacer(Modifier.height(Space.lg))
                Hint(it, isError = true)
            }

            Spacer(Modifier.height(Space.xl))

            PrimaryButton(
                text = if (isCorrection) "Save correction" else "Record result",
                enabled = check.isAcceptable && !submitting,
                onClick = {
                    if (submitting) return@PrimaryButton
                    submitting = true
                    val rejection = onConfirm(scoreA, scoreB)
                    if (rejection == null) {
                        onDismiss()
                    } else {
                        submitting = false
                        error = rejection.message()
                    }
                },
            )

            if (!check.isAcceptable) {
                Spacer(Modifier.height(Space.sm))
                Text(
                    text = check.rejection.message(),
                    style = CourtType.Caption,
                    color = CourtColors.FaultRed,
                )
            }

            Spacer(Modifier.height(Space.md))
            DangerButton(text = "Void this match", onClick = { onVoid(); onDismiss() })

            Spacer(Modifier.height(Space.sm))
            Text(
                text = "Voiding gives nobody credit and frees the court.",
                style = CourtType.Caption,
                color = CourtColors.Chalk60,
            )
        }
    }
}

@Composable
private fun TeamScore(
    names: List<String>,
    score: Int,
    onScoreChange: (Int) -> Unit,
    leading: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (leading) CourtColors.ShuttleCork.copy(alpha = 0.10f) else CourtColors.ServiceBox,
                RoundedCornerShape(Radius.md),
            )
            .border(
                Sizes.courtStroke,
                if (leading) CourtColors.ShuttleCork else CourtColors.LineFaint,
                RoundedCornerShape(Radius.md),
            )
            .padding(Space.lg),
    ) {
        Text(
            text = names.joinToString(" & "),
            style = CourtType.PlayerName,
            color = CourtColors.CourtLine,
        )
        Spacer(Modifier.height(Space.md))
        NumberStepper(
            value = score,
            onValueChange = onScoreChange,
            range = 0..99,
            label = names.joinToString(" and "),
        )
    }
}

@Composable
private fun QuickFill(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .heightIn(min = Sizes.minTapTarget)
            .background(CourtColors.ServiceBox, RoundedCornerShape(Radius.pill))
            .border(1.dp, CourtColors.LineFaint, RoundedCornerShape(Radius.pill))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = Space.lg),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, style = CourtType.Label, color = CourtColors.CourtLine)
    }
}

private fun ScoreRejection?.message(): String = when (this) {
    ScoreRejection.EQUAL_SCORES -> "Badminton has no draws — one side has to be ahead."
    ScoreRejection.NEGATIVE_SCORE -> "Scores cannot be negative."
    ScoreRejection.MATCH_NOT_PENDING -> "That result was already recorded."
    ScoreRejection.MATCH_NOT_FOUND -> "That match is no longer on the board."
    null -> ""
}
