package com.example.badmintonshuffler.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.badmintonshuffler.ui.theme.CourtColors
import com.example.badmintonshuffler.ui.theme.CourtType
import com.example.badmintonshuffler.ui.theme.Radius
import com.example.badmintonshuffler.ui.theme.Sizes
import com.example.badmintonshuffler.ui.theme.Space

/**
 * The signature component: a badminton court, seen from above.
 *
 * The doubles boundary is the card's outline, the net runs across the middle, and the four players
 * sit in their own quadrants with the two teams on opposite sides of the net. That geometry is the
 * whole point — someone glancing at the phone from the far side of the hall should be able to see
 * who is on their court and which end they are playing without reading a word of it.
 */

enum class CourtCardState { PENDING, SCORE_ENTRY, COMPLETED, VOIDED }

data class CourtSide(
    val playerNames: List<String>,
    val score: Int? = null,
    /** Points each player on this side just earned. Shown only once the match is completed. */
    val pointsEarned: Int? = null,
)

@Composable
fun CourtCard(
    courtNumber: Int,
    top: CourtSide,
    bottom: CourtSide,
    state: CourtCardState,
    modifier: Modifier = Modifier,
    animateResult: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    val topWon = state == CourtCardState.COMPLETED &&
        (top.score ?: 0) > (bottom.score ?: 0)
    val bottomWon = state == CourtCardState.COMPLETED &&
        (bottom.score ?: 0) > (top.score ?: 0)

    // The one animation on this screen: the moment a result lands. Brief, once, then still.
    val revealProgress by animateFloatAsState(
        targetValue = if (state == CourtCardState.COMPLETED) 1f else 0f,
        animationSpec = tween(durationMillis = if (animateResult) 320 else 0),
        label = "resultReveal",
    )

    val borderColor by animateColorAsState(
        targetValue = when (state) {
            CourtCardState.SCORE_ENTRY -> CourtColors.ShuttleCork
            CourtCardState.COMPLETED -> CourtColors.LineStrong
            CourtCardState.VOIDED -> CourtColors.LineFaint
            CourtCardState.PENDING -> CourtColors.LineStrong
        },
        animationSpec = tween(if (animateResult) 220 else 0),
        label = "courtBorder",
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.lg))
            .background(CourtColors.ServiceBox)
            .border(Sizes.courtStroke, borderColor, RoundedCornerShape(Radius.lg))
            .then(
                if (onClick != null) {
                    Modifier.clickable(role = Role.Button, onClick = onClick)
                } else {
                    Modifier
                }
            )
            .semantics(mergeDescendants = true) {
                contentDescription = describe(courtNumber, top, bottom, state)
            }
    ) {
        CourtHeader(courtNumber = courtNumber, state = state)

        // --- The court itself ---
        Column(modifier = Modifier.padding(horizontal = Space.md, vertical = Space.sm)) {
            HalfCourt(
                side = top,
                won = topWon,
                showScore = state == CourtCardState.COMPLETED,
                reveal = revealProgress,
            )
            Net()
            HalfCourt(
                side = bottom,
                won = bottomWon,
                showScore = state == CourtCardState.COMPLETED,
                reveal = revealProgress,
            )
        }

        Spacer(Modifier.height(Space.sm))
    }
}

@Composable
private fun CourtHeader(courtNumber: Int, state: CourtCardState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = Space.lg, end = Space.md, top = Space.md, bottom = Space.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "COURT",
            style = CourtType.Eyebrow,
            color = CourtColors.Chalk60,
        )
        Spacer(Modifier.width(Space.sm))
        Text(
            text = "$courtNumber",
            style = CourtType.Numeric,
            color = CourtColors.CourtLine,
        )
        Spacer(Modifier.weight(1f))

        // No tag on a completed card. The score is right there and the winning side is tinted —
        // a row of green "DONE" chips was the busiest, least informative thing on the screen.
        when (state) {
            CourtCardState.PENDING -> StatusTag("TAP TO SCORE", CourtColors.ShuttleCork)
            CourtCardState.SCORE_ENTRY -> StatusTag("ENTERING", CourtColors.ShuttleCork)
            CourtCardState.VOIDED -> StatusTag("VOIDED", CourtColors.Chalk60)
            CourtCardState.COMPLETED -> Unit
        }
    }
}

@Composable
private fun StatusTag(text: String, color: Color) {
    Text(
        text = text,
        style = CourtType.Eyebrow,
        color = color,
        modifier = Modifier
            .background(color.copy(alpha = 0.14f), RoundedCornerShape(Radius.pill))
            .padding(horizontal = Space.md, vertical = Space.xs),
    )
}

/**
 * One half of the court: two service boxes, each with a player in it, and the score for that end
 * out at the tramline where a scoreboard would be.
 */
@Composable
private fun HalfCourt(
    side: CourtSide,
    won: Boolean,
    showScore: Boolean,
    reveal: Float,
) {
    val winnerTint = if (won) CourtColors.ShuttleCork.copy(alpha = 0.12f * reveal) else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Sizes.halfCourtMinHeight)
            .clip(RoundedCornerShape(Radius.sm))
            .background(winnerTint),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Two quadrants, split by the centre service line.
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Quadrant(side.playerNames.getOrNull(0), Modifier.weight(1f))
            Box(
                Modifier
                    .width(Sizes.hairline)
                    .height(Sizes.centreLineHeight)
                    .background(CourtColors.LineFaint)
            )
            Quadrant(side.playerNames.getOrNull(1), Modifier.weight(1f))
        }

        if (showScore) {
            Column(
                modifier = Modifier
                    .width(Sizes.courtScoreColumn)
                    .padding(end = Space.xs),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = side.score?.toString() ?: "–",
                    style = CourtType.Score,
                    color = if (won) CourtColors.ShuttleCork else CourtColors.Chalk60,
                )
                if (side.pointsEarned != null) {
                    Text(
                        text = "+${side.pointsEarned}",
                        style = CourtType.Caption,
                        color = if (won) CourtColors.ShuttleCork else CourtColors.Chalk60,
                    )
                }
            }
        }
    }
}

@Composable
private fun Quadrant(name: String?, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.padding(horizontal = Space.sm, vertical = Space.md),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = name ?: "—",
            style = CourtType.PlayerName,
            color = if (name != null) CourtColors.CourtLine else CourtColors.Inert,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** The net: a solid band with the white tape along its top edge, same as the real thing. */
@Composable
private fun Net() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Space.xs)
            .clearAndSetSemantics { },
        verticalArrangement = Arrangement.spacedBy(Sizes.hairline),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(Sizes.netThickness)
                .background(CourtColors.CourtLine)
        )
        Box(
            Modifier
                .fillMaxWidth()
                .height(Sizes.netThickness)
                .background(CourtColors.LineFaint)
        )
    }
}

private fun describe(
    courtNumber: Int,
    top: CourtSide,
    bottom: CourtSide,
    state: CourtCardState,
): String {
    val teams = "${top.playerNames.joinToString(" and ")} versus " +
        bottom.playerNames.joinToString(" and ")
    return when (state) {
        CourtCardState.PENDING -> "Court $courtNumber, $teams. No score yet, tap to enter one."
        CourtCardState.SCORE_ENTRY -> "Court $courtNumber, $teams. Entering score."
        CourtCardState.VOIDED -> "Court $courtNumber, $teams. Match voided, nobody scored."
        CourtCardState.COMPLETED -> {
            val a = top.score ?: 0
            val b = bottom.score ?: 0
            val winners = if (a > b) top.playerNames else bottom.playerNames
            "Court $courtNumber, $teams. Final score $a to $b. " +
                "${winners.joinToString(" and ")} won."
        }
    }
}
