package com.example.badmintonshuffler.ui.component

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.badmintonshuffler.ui.theme.CourtColors
import com.example.badmintonshuffler.ui.theme.CourtType
import com.example.badmintonshuffler.ui.theme.Radius
import com.example.badmintonshuffler.ui.theme.Sizes
import com.example.badmintonshuffler.ui.theme.Space

/**
 * Big minus, big number, big plus.
 *
 * Used for court count, game length and score entry. The value is a tabular figure so it does not
 * shift sideways as it is stepped, and both buttons are 56dp because this gets used with a racket
 * in the other hand.
 */
@Composable
fun NumberStepper(
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    range: IntRange = 0..99,
    label: String? = null,
    unit: String? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.lg),
    ) {
        StepperButton(
            glyph = CourtGlyph.MINUS,
            description = "Decrease${label?.let { " $it" } ?: ""}",
            enabled = value > range.first,
            onClick = { onValueChange((value - 1).coerceIn(range)) },
        )

        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "$value",
                style = CourtType.Score,
                color = CourtColors.CourtLine,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics {
                    contentDescription = "${label ?: "Value"}: $value ${unit ?: ""}".trim()
                },
            )
            if (unit != null) {
                Text(
                    text = unit,
                    style = CourtType.Caption,
                    color = CourtColors.Chalk60,
                    modifier = Modifier.clearAndSetSemantics { },
                )
            }
        }

        StepperButton(
            glyph = CourtGlyph.PLUS,
            description = "Increase${label?.let { " $it" } ?: ""}",
            enabled = value < range.last,
            onClick = { onValueChange((value + 1).coerceIn(range)) },
        )
    }
}

@Composable
private fun StepperButton(
    glyph: CourtGlyph,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(Sizes.stepperButton)
            .alpha(if (enabled) 1f else 0.35f)
            .background(CourtColors.ServiceBox, CircleShape)
            .border(Sizes.courtStroke, CourtColors.LineStrong, CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        CourtIcon(glyph = glyph, contentDescription = description, tint = CourtColors.CourtLine)
    }
}

/** The one text input in the app. */
@Composable
fun CourtTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    label: String? = null,
    supportingText: String? = null,
    isError: Boolean = false,
    imeAction: ImeAction = ImeAction.Done,
    onImeAction: () -> Unit = {},
    focusRequester: FocusRequester? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = Sizes.controlHeight)
            .then(focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier),
        textStyle = CourtType.Body17,
        placeholder = { Text(placeholder, style = CourtType.Body17, color = CourtColors.Inert) },
        label = label?.let { { Text(it, style = CourtType.Label) } },
        supportingText = supportingText?.let {
            {
                Text(
                    text = it,
                    style = CourtType.Caption,
                    color = if (isError) CourtColors.FaultRed else CourtColors.Chalk60,
                )
            }
        },
        isError = isError,
        singleLine = true,
        shape = RoundedCornerShape(Radius.md),
        keyboardOptions = KeyboardOptions(imeAction = imeAction),
        keyboardActions = KeyboardActions(
            onDone = { onImeAction() },
            onNext = { onImeAction() },
            onGo = { onImeAction() },
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = CourtColors.CourtLine,
            unfocusedTextColor = CourtColors.CourtLine,
            focusedBorderColor = CourtColors.ShuttleCork,
            unfocusedBorderColor = CourtColors.LineStrong,
            errorBorderColor = CourtColors.FaultRed,
            cursorColor = CourtColors.ShuttleCork,
            focusedContainerColor = CourtColors.ServiceBox,
            unfocusedContainerColor = CourtColors.ServiceBox,
            errorContainerColor = CourtColors.ServiceBox,
            focusedLabelColor = CourtColors.ShuttleCork,
            unfocusedLabelColor = CourtColors.Chalk60,
        ),
    )
}

/** A player's name, with a way to take them off the list. */
@Composable
fun PlayerChip(
    name: String,
    modifier: Modifier = Modifier,
    onRemove: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .heightIn(min = Sizes.minTapTarget)
            .background(CourtColors.ServiceBox, RoundedCornerShape(Radius.pill))
            .border(Sizes.hairline, CourtColors.LineFaint, RoundedCornerShape(Radius.pill))
            .padding(start = Space.lg, end = if (onRemove != null) Space.xs else Space.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = name,
            style = CourtType.PlayerName,
            color = CourtColors.CourtLine,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = Sizes.chipNameMaxWidth),
        )
        if (onRemove != null) {
            Spacer(Modifier.width(Space.xs))
            Box(
                modifier = Modifier
                    .size(Sizes.minTapTarget - Space.sm)
                    .clickable(role = Role.Button, onClick = onRemove),
                contentAlignment = Alignment.Center,
            ) {
                CourtIcon(
                    glyph = CourtGlyph.CLOSE,
                    contentDescription = "Remove $name",
                    tint = CourtColors.Chalk60,
                    size = Sizes.iconSmall,
                )
            }
        }
    }
}

/** Shown wherever a list is legitimately empty, so a blank area never reads as a bug. */
@Composable
fun EmptyState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(CourtColors.ServiceBox, RoundedCornerShape(Radius.md))
            .border(Sizes.hairline, CourtColors.LineFaint, RoundedCornerShape(Radius.md))
            .padding(Space.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = CourtType.SectionTitle, color = CourtColors.CourtLine)
        Spacer(Modifier.height(Space.sm))
        Text(
            text = body,
            style = CourtType.Body17,
            color = CourtColors.Chalk60,
            textAlign = TextAlign.Center,
        )
    }
}

/** A short strip of secondary information. Present, quiet. */
@Composable
fun RestingStrip(
    names: List<String>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "RESTING THIS ROUND",
            style = CourtType.Eyebrow,
            color = CourtColors.Chalk60,
        )
        Spacer(Modifier.height(Space.sm))
        if (names.isEmpty()) {
            Text(
                text = "Nobody — everyone is on court.",
                style = CourtType.Body17,
                color = CourtColors.Chalk60,
            )
        } else {
            Text(
                text = names.joinToString("  ·  "),
                style = CourtType.Body17,
                color = CourtColors.CourtLine,
            )
            Spacer(Modifier.height(Space.xs))
            Text(
                text = "Up first next round.",
                style = CourtType.Caption,
                color = CourtColors.Chalk60,
            )
        }
    }
}

/**
 * The fairness indicator. Green while the games-played spread is 0 or 1, amber beyond that.
 *
 * It is tappable because "why is it amber" is the first thing anyone asks.
 */
@Composable
fun FairnessPill(
    spread: Int,
    isFair: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val color = if (isFair) CourtColors.FairGreen else CourtColors.WarnAmber
    Row(
        modifier = modifier
            .heightIn(min = Sizes.minTapTarget)
            .background(color.copy(alpha = 0.14f), RoundedCornerShape(Radius.pill))
            .then(
                if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick)
                else Modifier
            )
            .padding(horizontal = Space.lg)
            .semantics(mergeDescendants = true) {
                contentDescription = if (isFair) {
                    "Games are even. The gap between the most and least played is $spread."
                } else {
                    "Games are uneven by $spread. Tap for the breakdown."
                }
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.sm),
    ) {
        Box(Modifier.size(Space.sm).background(color, CircleShape))
        Text(
            text = if (isFair) "Even" else "Uneven +$spread",
            style = CourtType.Label,
            color = color,
        )
    }
}
