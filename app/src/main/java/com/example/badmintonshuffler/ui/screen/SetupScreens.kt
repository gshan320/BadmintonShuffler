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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.badmintonshuffler.model.SessionConfig
import com.example.badmintonshuffler.model.SessionDefaults
import com.example.badmintonshuffler.ui.component.Hint
import com.example.badmintonshuffler.ui.component.NumberStepper
import com.example.badmintonshuffler.ui.component.PrimaryButton
import com.example.badmintonshuffler.ui.component.Screen
import com.example.badmintonshuffler.ui.component.StepHeader
import com.example.badmintonshuffler.ui.theme.CourtColors
import com.example.badmintonshuffler.ui.theme.CourtType
import com.example.badmintonshuffler.ui.theme.Radius
import com.example.badmintonshuffler.ui.theme.Sizes
import com.example.badmintonshuffler.ui.theme.Space
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * The setup wizard: one question per screen, in a fixed order, forward-only with a back button.
 *
 * Every screen writes to the store on every keystroke and tap, so going back and forward never
 * loses anything. There is deliberately nothing else on these screens — no tabs, no shortcuts.
 */

const val SETUP_STEPS = 5

private val timeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a")

fun LocalTime.display(): String = format(timeFormat)

// ---------------------------------------------------------------------------------------------
// Step 1 — courts
// ---------------------------------------------------------------------------------------------

@Composable
fun SetupCourtsScreen(
    config: SessionConfig,
    onCourtCountChange: (Int) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    Screen(bottomBar = { PrimaryButton("Next", onNext) }) {
        StepHeader(
            step = 1,
            totalSteps = SETUP_STEPS,
            title = "How many courts do you have?",
            onBack = onBack,
        )

        NumberStepper(
            value = config.courtCount,
            onValueChange = onCourtCountChange,
            range = SessionDefaults.MIN_COURTS..SessionDefaults.MAX_COURTS,
            label = "Courts",
            unit = if (config.courtCount == 1) "court" else "courts",
        )

        Spacer(Modifier.height(Space.xl))
        Hint("${config.courtCount} ${if (config.courtCount == 1) "court" else "courts"} = " +
            "${config.playersOnCourt} players on at once.")
    }
}

// ---------------------------------------------------------------------------------------------
// Step 2 — time
// ---------------------------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupTimeScreen(
    config: SessionConfig,
    onTimesChange: (LocalTime, LocalTime) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    var editingStart by remember { mutableStateOf(true) }
    val endIsValid = config.durationMinutes > 0

    val pickerState = rememberTimePickerState(
        initialHour = if (editingStart) config.startTime.hour else config.endTime.hour,
        initialMinute = if (editingStart) config.startTime.minute else config.endTime.minute,
        is24Hour = false,
    )

    Screen(bottomBar = { PrimaryButton("Next", onNext, enabled = endIsValid) }) {
        StepHeader(
            step = 2,
            totalSteps = SETUP_STEPS,
            title = "When are you playing?",
            onBack = onBack,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(Space.md)) {
            TimeSlot(
                label = "Starts",
                time = config.startTime,
                selected = editingStart,
                onClick = { editingStart = true },
                modifier = Modifier.weight(1f),
            )
            TimeSlot(
                label = "Ends",
                time = config.endTime,
                selected = !editingStart,
                onClick = { editingStart = false },
                modifier = Modifier.weight(1f),
                isError = !endIsValid,
            )
        }

        Spacer(Modifier.height(Space.lg))

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            TimePicker(
                state = pickerState,
                colors = TimePickerDefaults.colors(
                    clockDialColor = CourtColors.ServiceBox,
                    selectorColor = CourtColors.ShuttleCork,
                    clockDialSelectedContentColor = CourtColors.NetTape,
                    clockDialUnselectedContentColor = CourtColors.CourtLine,
                    periodSelectorSelectedContainerColor = CourtColors.ShuttleCork,
                    periodSelectorSelectedContentColor = CourtColors.NetTape,
                    periodSelectorUnselectedContentColor = CourtColors.CourtLine,
                    timeSelectorSelectedContainerColor = CourtColors.ShuttleCork,
                    timeSelectorSelectedContentColor = CourtColors.NetTape,
                    timeSelectorUnselectedContainerColor = CourtColors.ServiceBox,
                    timeSelectorUnselectedContentColor = CourtColors.CourtLine,
                ),
            )
        }

        // Push the picked value back on every change, so nothing needs confirming.
        val picked = LocalTime.of(pickerState.hour, pickerState.minute)
        LaunchedEffect(picked, editingStart) {
            if (editingStart) onTimesChange(picked, config.endTime)
            else onTimesChange(config.startTime, picked)
        }

        Spacer(Modifier.height(Space.lg))
        if (endIsValid) {
            Hint("That is ${formatDuration(config.durationMinutes)} of badminton.")
        } else {
            Hint("The end time needs to be after the start time.", isError = true)
        }
    }
}

@Composable
private fun TimeSlot(
    label: String,
    time: LocalTime,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
) {
    Column(
        modifier = modifier
            .heightIn(min = Sizes.minTapTarget)
            .background(CourtColors.ServiceBox, RoundedCornerShape(Radius.md))
            .border(
                width = Sizes.courtStroke,
                color = when {
                    isError -> CourtColors.FaultRed
                    selected -> CourtColors.ShuttleCork
                    else -> CourtColors.LineFaint
                },
                shape = RoundedCornerShape(Radius.md),
            )
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(Space.lg),
    ) {
        Text(label.uppercase(), style = CourtType.Eyebrow, color = CourtColors.Chalk60)
        Spacer(Modifier.height(Space.xs))
        Text(time.display(), style = CourtType.Numeric, color = CourtColors.CourtLine)
    }
}

// ---------------------------------------------------------------------------------------------
// Step 3 — pace
// ---------------------------------------------------------------------------------------------

@Composable
fun SetupPaceScreen(
    config: SessionConfig,
    onMinutesChange: (Int) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    Screen(bottomBar = { PrimaryButton("Next", onNext) }) {
        StepHeader(
            step = 3,
            totalSteps = SETUP_STEPS,
            title = "How long is a typical game?",
            onBack = onBack,
        )

        NumberStepper(
            value = config.minutesPerGame,
            onValueChange = onMinutesChange,
            range = SessionDefaults.MIN_MINUTES_PER_GAME..SessionDefaults.MAX_MINUTES_PER_GAME,
            label = "Minutes per game",
            unit = "minutes",
        )

        Spacer(Modifier.height(Space.xl))
        Hint("About ${config.estimatedTotalRounds} rounds today.")
    }
}

// ---------------------------------------------------------------------------------------------
// Step 4 — scoring
// ---------------------------------------------------------------------------------------------

@Composable
fun SetupScoringScreen(
    config: SessionConfig,
    onTargetChange: (Int) -> Unit,
    onPointsPerWinChange: (Int) -> Unit,
    onPointsPerLossChange: (Int) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    Screen(bottomBar = { PrimaryButton("Next", onNext) }) {
        StepHeader(
            step = 4,
            totalSteps = SETUP_STEPS,
            title = "How are we scoring?",
            subtitle = "The defaults suit most groups. You can pass this screen with one tap.",
            onBack = onBack,
        )

        Text("GAMES PLAYED TO", style = CourtType.Eyebrow, color = CourtColors.Chalk60)
        Spacer(Modifier.height(Space.md))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            listOf(11, 15, 21).forEach { target ->
                ChoiceChip(
                    text = "$target",
                    selected = config.targetScore == target,
                    onClick = { onTargetChange(target) },
                )
            }
        }
        Spacer(Modifier.height(Space.md))
        NumberStepper(
            value = config.targetScore,
            onValueChange = onTargetChange,
            range = 5..51,
            label = "Target score",
            unit = "points to win a game",
        )

        Spacer(Modifier.height(Space.xxl))
        Text("SESSION POINTS", style = CourtType.Eyebrow, color = CourtColors.Chalk60)
        Spacer(Modifier.height(Space.md))
        NumberStepper(
            value = config.pointsPerWin,
            onValueChange = onPointsPerWinChange,
            range = 0..10,
            label = "Points per win",
            unit = "per win",
        )
        Spacer(Modifier.height(Space.lg))
        NumberStepper(
            value = config.pointsPerLoss,
            onValueChange = onPointsPerLossChange,
            range = 0..10,
            label = "Points per loss",
            unit = "per loss",
        )

        Spacer(Modifier.height(Space.lg))
        Hint(
            "Turning up to play can be rewarded with more than zero points."
        )
    }
}

@Composable
private fun ChoiceChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .heightIn(min = Sizes.minTapTarget)
            .background(
                if (selected) CourtColors.ShuttleCork else CourtColors.ServiceBox,
                RoundedCornerShape(Radius.pill),
            )
            .border(
                Sizes.courtStroke,
                if (selected) CourtColors.ShuttleCork else CourtColors.LineFaint,
                RoundedCornerShape(Radius.pill),
            )
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = Space.xl),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = CourtType.Numeric,
            color = if (selected) CourtColors.NetTape else CourtColors.CourtLine,
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Shared bits
// ---------------------------------------------------------------------------------------------

fun formatDuration(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h > 0 && m > 0 -> "${h}h ${m}m"
        h > 0 -> "${h}h"
        else -> "${m}m"
    }
}
