package com.example.badmintonshuffler.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.badmintonshuffler.ui.theme.CourtColors
import com.example.badmintonshuffler.ui.theme.CourtType
import com.example.badmintonshuffler.ui.theme.Sizes
import com.example.badmintonshuffler.ui.theme.Space

/**
 * The top of every setup screen: where you are, what is being asked, and how far through you are.
 *
 * The progress dots are the only navigation on these screens — the setup flow is forward-only with
 * a back arrow, and deliberately has no tabs or shortcuts. One question at a time.
 */
@Composable
fun StepHeader(
    step: Int,
    totalSteps: Int,
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(top = Space.md, bottom = Space.xl)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) {
                Box(
                    modifier = Modifier
                        .size(Sizes.minTapTarget)
                        .clickable(role = Role.Button, onClick = onBack),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    CourtIcon(
                        glyph = CourtGlyph.BACK,
                        contentDescription = "Go back",
                        tint = CourtColors.CourtLine,
                    )
                }
            } else {
                Spacer(Modifier.height(Sizes.minTapTarget))
            }

            ProgressDots(
                current = step,
                total = totalSteps,
                modifier = Modifier.padding(start = if (onBack != null) Space.sm else 0.dp),
            )
        }

        Spacer(Modifier.height(Space.lg))

        Text(
            text = "Step $step of $totalSteps",
            style = CourtType.Eyebrow,
            color = CourtColors.ShuttleCork,
        )
        Spacer(Modifier.height(Space.sm))
        Text(text = title, style = CourtType.Title, color = CourtColors.CourtLine)

        if (subtitle != null) {
            Spacer(Modifier.height(Space.sm))
            Text(text = subtitle, style = CourtType.Body17, color = CourtColors.Chalk60)
        }
    }
}

@Composable
private fun ProgressDots(current: Int, total: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.semantics {
            contentDescription = "Step $current of $total"
        },
        horizontalArrangement = Arrangement.spacedBy(Space.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(total) { index ->
            val isDone = index < current
            val isCurrent = index == current - 1
            Box(
                modifier = Modifier
                    .clearAndSetSemantics { }
                    .height(Space.sm)
                    // The current step is a dash rather than a dot, so position is readable at a
                    // glance without counting.
                    .width(if (isCurrent) Space.xl else Space.sm)
                    .background(
                        color = when {
                            isCurrent -> CourtColors.ShuttleCork
                            isDone -> CourtColors.Chalk60
                            else -> CourtColors.LineFaint
                        },
                        shape = CircleShape,
                    )
            )
        }
    }
}
