package com.example.badmintonshuffler.ui.screen

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.badmintonshuffler.engine.sessionPoints
import com.example.badmintonshuffler.model.Player
import com.example.badmintonshuffler.model.SessionState
import com.example.badmintonshuffler.state.RemovalPrompt
import com.example.badmintonshuffler.ui.component.ConfirmDialog
import com.example.badmintonshuffler.ui.component.CourtTextField
import com.example.badmintonshuffler.ui.component.Hint
import com.example.badmintonshuffler.ui.component.DangerButton
import com.example.badmintonshuffler.ui.component.PrimaryButton
import com.example.badmintonshuffler.ui.component.SecondaryButton
import com.example.badmintonshuffler.ui.theme.CourtColors
import com.example.badmintonshuffler.ui.theme.CourtType
import com.example.badmintonshuffler.ui.theme.Radius
import com.example.badmintonshuffler.ui.theme.Sizes
import com.example.badmintonshuffler.ui.theme.Space

/**
 * Mid-session roster edits, reachable at any time.
 *
 * The important thing on this sheet is not a control, it is a sentence: when a player is added the
 * app explains its own fairness rule in plain words, so the argument never has to happen in the hall.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RosterSheet(
    state: SessionState,
    onAddPlayer: (String) -> Unit,
    onRemovePlayer: (String) -> RemovalPrompt,
    onSubstitute: (outgoing: String, incoming: String) -> Unit,
    onVoidMatch: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var draft by remember { mutableStateOf("") }
    var lastAdded by remember { mutableStateOf<String?>(null) }
    var confirmingRemoval by remember { mutableStateOf<Player?>(null) }
    var pendingPrompt by remember { mutableStateOf<RemovalPrompt?>(null) }

    val active = state.activePlayers
    val departed = state.players.filterNot { it.isActive }
    val roundNumber = (state.currentRound?.index ?: 0) + 1

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CourtColors.CourtDeep,
        scrimColor = CourtColors.NetTape.copy(alpha = 0.72f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = Space.screenGutter, vertical = Space.sm),
        ) {
            Text("Players", style = CourtType.Title, color = CourtColors.CourtLine)
            Spacer(Modifier.height(Space.xs))
            Text(
                text = "Changes take effect from the next round, unless you substitute someone " +
                    "into a game that is already on.",
                style = CourtType.Caption,
                color = CourtColors.Chalk60,
            )

            Spacer(Modifier.height(Space.lg))

            // --- Add ---
            Row(verticalAlignment = Alignment.Top) {
                CourtTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    modifier = Modifier.weight(1f),
                    placeholder = "Add a player",
                    imeAction = ImeAction.Done,
                    onImeAction = {
                        if (draft.isNotBlank()) {
                            onAddPlayer(draft.trim()); lastAdded = draft.trim(); draft = ""
                        }
                    },
                )
                Spacer(Modifier.width(Space.sm))
                SecondaryButton(
                    text = "Add",
                    enabled = draft.isNotBlank(),
                    onClick = {
                        onAddPlayer(draft.trim()); lastAdded = draft.trim(); draft = ""
                    },
                    modifier = Modifier.width(Sizes.inlineActionWidth),
                )
            }

            // The app explaining its own rule, in the words an organiser would use.
            lastAdded?.let { name ->
                Spacer(Modifier.height(Space.md))
                Hint(
                    "$name joins from round $roundNumber. They'll get the same number of games as " +
                        "everyone from here on, but start at 0 points."
                )
            }

            Spacer(Modifier.height(Space.xl))

            // --- Active ---
            Text("ON THE ROSTER", style = CourtType.Eyebrow, color = CourtColors.Chalk60)
            Spacer(Modifier.height(Space.sm))
            Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                active.forEach { player ->
                    PlayerRow(
                        player = player,
                        points = sessionPoints(player, state.config),
                        onRemove = { confirmingRemoval = player },
                    )
                }
            }

            // --- Departed ---
            if (departed.isNotEmpty()) {
                Spacer(Modifier.height(Space.xl))
                Text("LEFT EARLY", style = CourtType.Eyebrow, color = CourtColors.Chalk60)
                Spacer(Modifier.height(Space.sm))
                Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                    departed.forEach { player ->
                        PlayerRow(
                            player = player,
                            points = sessionPoints(player, state.config),
                            onRemove = null,
                            modifier = Modifier.alpha(0.55f),
                        )
                    }
                }
                Spacer(Modifier.height(Space.sm))
                Text(
                    text = "They keep the points they earned.",
                    style = CourtType.Caption,
                    color = CourtColors.Chalk60,
                )
            }

            Spacer(Modifier.height(Space.xxl))
        }
    }

    // --- Confirm a removal ---
    confirmingRemoval?.let { player ->
        ConfirmDialog(
            title = "Mark ${player.name} as left?",
            body = "They stop being picked for rounds from now on. Their ${player.gamesPlayed} " +
                "games and their points stay on the leaderboard.",
            confirmText = "Mark as left",
            onConfirm = {
                val prompt = onRemovePlayer(player.id)
                confirmingRemoval = null
                pendingPrompt = prompt.takeIf { it.affectedMatchId != null }
            },
            onDismiss = { confirmingRemoval = null },
        )
    }

    // --- They were mid-match: exactly two choices ---
    pendingPrompt?.let { prompt ->
        val matchId = prompt.affectedMatchId ?: return@let
        SubstituteDialog(
            state = state,
            prompt = prompt,
            onSubstitute = { incoming ->
                onSubstitute(prompt.playerId, incoming)
                pendingPrompt = null
            },
            onVoid = {
                onVoidMatch(matchId)
                pendingPrompt = null
            },
        )
    }
}

@Composable
private fun PlayerRow(
    player: Player,
    points: Int,
    onRemove: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = Sizes.minTapTarget)
            .background(CourtColors.ServiceBox, RoundedCornerShape(Radius.md))
            .padding(horizontal = Space.lg, vertical = Space.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(player.name, style = CourtType.PlayerName, color = CourtColors.CourtLine)
            Text(
                text = "${player.gamesPlayed} games · ${player.wins}W-${player.losses}L · $points pts",
                style = CourtType.Caption,
                color = CourtColors.Chalk60,
            )
        }
        if (onRemove != null) {
            Box(
                modifier = Modifier
                    .heightIn(min = Sizes.minTapTarget)
                    .clickable(role = Role.Button, onClick = onRemove)
                    .padding(horizontal = Space.md),
                contentAlignment = Alignment.Center,
            ) {
                Text("Mark as left", style = CourtType.Label, color = CourtColors.FaultRed)
            }
        }
    }
}

/**
 * The removed player was mid-game. Two options, no third — either someone takes their place or the
 * match does not count.
 */
@Composable
private fun SubstituteDialog(
    state: SessionState,
    prompt: RemovalPrompt,
    onSubstitute: (String) -> Unit,
    onVoid: () -> Unit,
) {
    val leaving = state.player(prompt.playerId)?.name ?: "That player"
    val onCourt = state.currentRound?.playingIds.orEmpty().toSet()
    val candidates = state.activePlayers.filter { it.id !in onCourt }
    var selected by remember(prompt.playerId) {
        mutableStateOf(prompt.recommendedSubstitute?.id ?: candidates.firstOrNull()?.id)
    }

    Dialog(onDismissRequest = { /* a decision is required */ }) {
        Column(
            modifier = Modifier
                .background(CourtColors.CourtDeep, RoundedCornerShape(Radius.lg))
                .border(Sizes.hairline, CourtColors.LineFaint, RoundedCornerShape(Radius.lg))
                .padding(Space.xl),
        ) {
            Text(
                text = "$leaving is mid-match",
                style = CourtType.SectionTitle,
                color = CourtColors.CourtLine,
            )
            Spacer(Modifier.height(Space.sm))
            Text(
                text = "Their court is still playing. Put someone in, or void the match so nobody " +
                    "gets credit and the court frees up.",
                style = CourtType.Body17,
                color = CourtColors.Chalk60,
            )

            if (candidates.isNotEmpty()) {
                Spacer(Modifier.height(Space.lg))
                Text("SUBSTITUTE", style = CourtType.Eyebrow, color = CourtColors.Chalk60)
                Spacer(Modifier.height(Space.sm))
                Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                    candidates.forEach { candidate ->
                        val isRecommended = candidate.id == prompt.recommendedSubstitute?.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = Sizes.minTapTarget)
                                .background(
                                    if (selected == candidate.id) {
                                        CourtColors.ShuttleCork.copy(alpha = 0.14f)
                                    } else {
                                        CourtColors.ServiceBox
                                    },
                                    RoundedCornerShape(Radius.md),
                                )
                                .border(
                                    Sizes.courtStroke,
                                    if (selected == candidate.id) CourtColors.ShuttleCork
                                    else CourtColors.LineFaint,
                                    RoundedCornerShape(Radius.md),
                                )
                                .clickable(role = Role.RadioButton) { selected = candidate.id }
                                .padding(horizontal = Space.lg, vertical = Space.md),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = candidate.name,
                                style = CourtType.PlayerName,
                                color = CourtColors.CourtLine,
                                modifier = Modifier.weight(1f),
                            )
                            if (isRecommended) {
                                Text(
                                    text = "NEXT UP",
                                    style = CourtType.Eyebrow,
                                    color = CourtColors.ShuttleCork,
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(Space.lg))
                PrimaryButton(
                    text = "Substitute in",
                    enabled = selected != null,
                    onClick = { selected?.let(onSubstitute) },
                )
            } else {
                Spacer(Modifier.height(Space.lg))
                Text(
                    text = "Nobody is resting, so there is no one to bring on.",
                    style = CourtType.Caption,
                    color = CourtColors.Chalk60,
                )
            }

            Spacer(Modifier.height(Space.md))
            DangerButton(text = "Void this match", onClick = onVoid)
        }
    }
}
