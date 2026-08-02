package com.example.badmintonshuffler.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import com.example.badmintonshuffler.ui.theme.CourtColors
import com.example.badmintonshuffler.ui.theme.CourtType
import com.example.badmintonshuffler.ui.theme.Radius
import com.example.badmintonshuffler.ui.theme.Sizes
import com.example.badmintonshuffler.ui.theme.Space

/**
 * The three button weights. There is no fourth — if something needs a different emphasis, it is
 * probably a [SecondaryButton] with better words.
 *
 * All of them are at least [Sizes.controlHeight] tall, which comfortably clears the 48dp minimum.
 */

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) = CourtButton(
    text = text,
    onClick = onClick,
    modifier = modifier,
    enabled = enabled,
    container = CourtColors.ShuttleCork,
    content = CourtColors.NetTape,
)

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) = CourtButton(
    text = text,
    onClick = onClick,
    modifier = modifier,
    enabled = enabled,
    container = Color.Transparent,
    content = CourtColors.CourtLine,
    border = BorderStroke(Sizes.courtStroke, CourtColors.LineStrong),
)

@Composable
fun DangerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) = CourtButton(
    text = text,
    onClick = onClick,
    modifier = modifier,
    enabled = enabled,
    container = Color.Transparent,
    content = CourtColors.FaultRed,
    border = BorderStroke(Sizes.courtStroke, CourtColors.FaultRed),
)

@Composable
private fun CourtButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    container: Color,
    content: Color,
    border: BorderStroke? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = Sizes.controlHeight)
            .defaultMinSize(minHeight = Sizes.minTapTarget)
            // Pressed state is a dim rather than a scale: the hall is loud and the phone is often
            // being glanced at, so the feedback needs to survive being seen out of the corner of an eye.
            .alpha(if (!enabled) 0.4f else if (pressed) 0.72f else 1f)
            .background(container, RoundedCornerShape(Radius.md))
            .then(border?.let { Modifier.border(it, RoundedCornerShape(Radius.md)) } ?: Modifier)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = Space.lg, vertical = Space.md),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = CourtType.Label,
            color = content,
            textAlign = TextAlign.Center,
        )
    }
}
