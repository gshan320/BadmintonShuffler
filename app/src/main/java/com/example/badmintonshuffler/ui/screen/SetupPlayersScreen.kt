package com.example.badmintonshuffler.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.badmintonshuffler.model.Player
import com.example.badmintonshuffler.model.SessionConfig
import com.example.badmintonshuffler.model.SessionDefaults
import com.example.badmintonshuffler.ui.component.CourtTextField
import com.example.badmintonshuffler.ui.component.PlayerChip
import com.example.badmintonshuffler.ui.component.PrimaryButton
import com.example.badmintonshuffler.ui.component.Screen
import com.example.badmintonshuffler.ui.component.SecondaryButton
import com.example.badmintonshuffler.ui.component.StepHeader
import com.example.badmintonshuffler.ui.theme.CourtColors
import com.example.badmintonshuffler.ui.theme.CourtType
import com.example.badmintonshuffler.ui.theme.Sizes
import com.example.badmintonshuffler.ui.theme.Space

/**
 * The last and most important setup screen.
 *
 * The design constraint that matters: the organiser is standing in a hall typing fourteen names.
 * The field keeps focus after every add and the keyboard's action key is "Next", so it is
 * name-Enter-name-Enter without ever touching the screen again.
 */
@Composable
fun SetupPlayersScreen(
    config: SessionConfig,
    players: List<Player>,
    onAddPlayer: (String) -> Unit,
    onRemovePlayer: (String) -> Unit,
    onBack: () -> Unit,
    onStart: () -> Unit,
) {
    var draft by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    val trimmed = draft.trim()
    val isDuplicate = trimmed.isNotEmpty() &&
        players.any { it.name.equals(trimmed, ignoreCase = true) }
    val enoughPlayers = players.size >= SessionDefaults.MIN_PLAYERS

    fun submit() {
        if (trimmed.isEmpty()) return
        onAddPlayer(trimmed)
        draft = ""
        focusRequester.requestFocus()
    }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Screen(
        modifier = Modifier.imePadding(),
        bottomBar = {
            PrimaryButton(
                text = "Start shuffling",
                onClick = onStart,
                enabled = enoughPlayers,
            )
            if (!enoughPlayers) {
                Spacer(Modifier.height(Space.sm))
                Text(
                    text = "Doubles needs at least ${SessionDefaults.MIN_PLAYERS} players — " +
                        "${SessionDefaults.MIN_PLAYERS - players.size} to go.",
                    style = CourtType.Caption,
                    color = CourtColors.Chalk60,
                )
            }
        },
    ) {
        StepHeader(
            step = 5,
            totalSteps = SETUP_STEPS,
            title = "Who's playing?",
            onBack = onBack,
        )

        // Pinned above the list so the keyboard never covers what is being typed.
        Row(verticalAlignment = Alignment.Top) {
            CourtTextField(
                value = draft,
                onValueChange = { draft = it },
                modifier = Modifier.weight(1f),
                placeholder = "Player name",
                supportingText = when {
                    isDuplicate -> "Already on the list — this one will be added as " +
                        "\"$trimmed (${players.count { it.name.startsWith(trimmed, true) } + 1})\"."
                    else -> null
                },
                imeAction = ImeAction.Next,
                onImeAction = { submit() },
                focusRequester = focusRequester,
            )
            Spacer(Modifier.width(Space.sm))
            SecondaryButton(
                text = "Add",
                onClick = { submit() },
                enabled = trimmed.isNotEmpty(),
                modifier = Modifier.width(Sizes.inlineActionWidth),
            )
        }

        Spacer(Modifier.height(Space.lg))

        Text(
            text = readout(players.size, config.courtCount),
            style = CourtType.Body17,
            color = CourtColors.Chalk60,
        )

        Spacer(Modifier.height(Space.lg))

        if (players.isEmpty()) {
            Text(
                text = "Nobody yet. Type a name and press enter.",
                style = CourtType.Body17,
                color = CourtColors.Inert,
            )
        } else {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Space.sm),
                verticalArrangement = Arrangement.spacedBy(Space.sm),
            ) {
                players.forEach { player ->
                    PlayerChip(name = player.name, onRemove = { onRemovePlayer(player.id) })
                }
            }
        }

        Spacer(Modifier.height(Space.xxl))
    }
}

/** "12 players, 3 courts — 12 on court, 0 resting each round." */
internal fun readout(playerCount: Int, courtCount: Int): String {
    if (playerCount == 0) return "No players yet."
    val usableCourts = minOf(courtCount, playerCount / 4)
    val onCourt = usableCourts * 4
    val resting = playerCount - onCourt
    val playerWord = if (playerCount == 1) "player" else "players"
    val courtWord = if (courtCount == 1) "court" else "courts"

    if (usableCourts == 0) {
        return "$playerCount $playerWord, $courtCount $courtWord — not enough for a doubles game yet."
    }

    val idleCourts = courtCount - usableCourts
    val idleNote = if (idleCourts > 0) {
        " ($idleCourts ${if (idleCourts == 1) "court" else "courts"} unused)"
    } else {
        ""
    }
    return "$playerCount $playerWord, $courtCount $courtWord — $onCourt on court, " +
        "$resting resting each round$idleNote."
}
