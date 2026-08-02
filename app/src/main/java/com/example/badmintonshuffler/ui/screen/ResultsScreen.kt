package com.example.badmintonshuffler.ui.screen

import android.content.ClipData
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.example.badmintonshuffler.engine.SessionStats
import com.example.badmintonshuffler.model.LeaderboardEntry
import com.example.badmintonshuffler.ui.rememberReduceMotion
import com.example.badmintonshuffler.ui.component.ConfirmDialog
import com.example.badmintonshuffler.ui.component.DangerButton
import com.example.badmintonshuffler.ui.component.Hint
import com.example.badmintonshuffler.ui.component.EmptyState
import com.example.badmintonshuffler.ui.component.PrimaryButton
import com.example.badmintonshuffler.ui.component.Screen
import com.example.badmintonshuffler.ui.theme.CourtColors
import com.example.badmintonshuffler.ui.theme.CourtType
import com.example.badmintonshuffler.ui.theme.Radius
import com.example.badmintonshuffler.ui.theme.Sizes
import com.example.badmintonshuffler.ui.theme.Space
import kotlinx.coroutines.launch

/**
 * The celebration screen — the one place in this app where boldness is warranted.
 *
 * Everything else got quieter so this could be loud.
 */
@Composable
fun ResultsScreen(
    entries: List<LeaderboardEntry>,
    stats: SessionStats,
    onClearSession: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val reduceMotion = rememberReduceMotion()

    var revealed by remember { mutableStateOf(reduceMotion) }
    var showStandings by remember { mutableStateOf(false) }
    var confirmingClear by remember { mutableStateOf(false) }
    var copied by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { revealed = true }

    val podium = entries.filter { it.rank <= 3 }

    Screen(
        modifier = modifier,
        bottomBar = {
            PrimaryButton(
                text = if (copied) "Copied to clipboard" else "Share results",
                onClick = {
                    val text = shareText(entries, stats)
                    scope.launch {
                        clipboard.setClipEntry(
                            ClipEntry(ClipData.newPlainText("Badminton results", text))
                        )
                        copied = true
                    }
                },
            )
            Spacer(Modifier.height(Space.sm))
            DangerButton(
                text = "Clear session and start fresh",
                onClick = { confirmingClear = true },
            )
        },
    ) {
        Spacer(Modifier.height(Space.lg))
        Text("THAT'S TIME", style = CourtType.Eyebrow, color = CourtColors.ShuttleCork)
        Spacer(Modifier.height(Space.sm))
        Text("Final standings", style = CourtType.Title, color = CourtColors.CourtLine)
        Spacer(Modifier.height(Space.xl))

        if (podium.isEmpty()) {
            EmptyState(
                title = "No results",
                body = "The session ended before any scores were recorded.",
            )
        } else {
            Podium(podium = podium, revealed = revealed, reduceMotion = reduceMotion)

            val firstPlaces = entries.count { it.rank == 1 }
            if (firstPlaces > 1) {
                Spacer(Modifier.height(Space.lg))
                Hint(tieExplanation(entries))
            }
        }

        Spacer(Modifier.height(Space.xxl))

        // --- Full standings, collapsed ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = Sizes.minTapTarget)
                .clickable(role = Role.Button) { showStandings = !showStandings },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Full standings",
                style = CourtType.SectionTitle,
                color = CourtColors.CourtLine,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = if (showStandings) "Hide" else "Show all ${entries.size}",
                style = CourtType.Label,
                color = CourtColors.ShuttleCork,
            )
        }

        if (showStandings) {
            Spacer(Modifier.height(Space.md))
            StandingsTable(entries)
        }

        Spacer(Modifier.height(Space.xxl))
        SessionStatsBlock(stats)
        Spacer(Modifier.height(Space.xxl))
    }

    if (confirmingClear) {
        ConfirmDialog(
            title = "Clear everything?",
            body = "Nothing in this app is saved anywhere — not on this phone, not on a server. " +
                "Once you clear it, these results are gone for good. Share them first if you " +
                "want to keep them.",
            confirmText = "Clear session",
            destructive = true,
            onConfirm = { confirmingClear = false; onClearSession() },
            onDismiss = { confirmingClear = false },
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Podium
// ---------------------------------------------------------------------------------------------

@Composable
private fun Podium(podium: List<LeaderboardEntry>, revealed: Boolean, reduceMotion: Boolean) {
    // Visual order puts first in the middle and tallest, the way a podium actually looks.
    val first = podium.filter { it.rank == 1 }
    val second = podium.filter { it.rank == 2 }
    val third = podium.filter { it.rank == 3 }

    Column(verticalArrangement = Arrangement.spacedBy(Space.md)) {
        first.forEachIndexed { index, entry ->
            PodiumBlock(entry, tier = 1, revealed = revealed, reduceMotion = reduceMotion, delayIndex = index)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Space.md)) {
            second.forEach { entry ->
                PodiumBlock(
                    entry, tier = 2, revealed = revealed, reduceMotion = reduceMotion,
                    delayIndex = 1, modifier = Modifier.weight(1f),
                )
            }
            third.forEach { entry ->
                PodiumBlock(
                    entry, tier = 3, revealed = revealed, reduceMotion = reduceMotion,
                    delayIndex = 2, modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun PodiumBlock(
    entry: LeaderboardEntry,
    tier: Int,
    revealed: Boolean,
    reduceMotion: Boolean,
    delayIndex: Int,
    modifier: Modifier = Modifier,
) {
    val progress by animateFloatAsState(
        targetValue = if (revealed) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (reduceMotion) 0 else 420,
            delayMillis = if (reduceMotion) 0 else delayIndex * 120,
        ),
        label = "podium$tier",
    )

    val isFirst = tier == 1
    Column(
        modifier = modifier
            .fillMaxWidth()
            .alpha(progress)
            .background(
                if (isFirst) CourtColors.ShuttleCork.copy(alpha = 0.16f) else CourtColors.ServiceBox,
                RoundedCornerShape(Radius.lg),
            )
            .border(
                Sizes.courtStroke,
                if (isFirst) CourtColors.ShuttleCork else CourtColors.LineFaint,
                RoundedCornerShape(Radius.lg),
            )
            .padding(if (isFirst) Space.xl else Space.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = ordinal(entry.rank),
            style = CourtType.Eyebrow,
            color = if (isFirst) CourtColors.ShuttleCork else CourtColors.Chalk60,
        )
        Spacer(Modifier.height(Space.sm))
        Text(
            text = entry.player.name,
            style = if (isFirst) CourtType.Title else CourtType.SectionTitle,
            color = CourtColors.CourtLine,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(Space.sm))
        Text(
            text = "${entry.sessionPoints}",
            style = if (isFirst) CourtType.DisplayScore else CourtType.Score,
            color = if (isFirst) CourtColors.ShuttleCork else CourtColors.CourtLine,
        )
        Text(
            text = "points",
            style = CourtType.Caption,
            color = CourtColors.Chalk60,
        )
        Spacer(Modifier.height(Space.xs))
        Text(
            text = "${entry.player.wins}W – ${entry.player.losses}L",
            style = CourtType.Caption,
            color = CourtColors.Chalk60,
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Stats
// ---------------------------------------------------------------------------------------------

@Composable
private fun SessionStatsBlock(stats: SessionStats) {
    Column {
        Text("SESSION", style = CourtType.Eyebrow, color = CourtColors.Chalk60)
        Spacer(Modifier.height(Space.md))
        StatLine("Games played", "${stats.totalGames}")
        StatLine(
            label = "Most games",
            value = if (stats.mostGamesPlayers.isEmpty()) "—" else
                "${stats.mostGamesPlayed} · ${stats.mostGamesPlayers.joinToString(", ") { it.name }}",
        )
        StatLine(
            label = "Longest win streak",
            value = if (stats.longestWinStreakPlayers.isEmpty()) "—" else
                "${stats.longestWinStreak} · " +
                    stats.longestWinStreakPlayers.joinToString(", ") { it.name },
        )
        StatLine(
            label = "Best partnership",
            value = stats.bestPartnership?.let {
                "${it.playerA.name} & ${it.playerB.name} · ${it.wins}/${it.games}"
            } ?: "—",
        )
    }
}

@Composable
private fun StatLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = Space.xs),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            style = CourtType.Caption,
            color = CourtColors.Chalk60,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = CourtType.Caption,
            color = CourtColors.CourtLine,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1.4f),
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Words
// ---------------------------------------------------------------------------------------------

private fun ordinal(rank: Int): String = when (rank) {
    1 -> "FIRST"
    2 -> "SECOND"
    3 -> "THIRD"
    else -> "${rank}TH"
}

/** Ties are shown honestly, with the reason they could not be separated. */
private fun tieExplanation(entries: List<LeaderboardEntry>): String {
    val tied = entries.filter { it.rank == 1 }
    val names = tied.joinToString(" and ") { it.player.name }
    return "$names finished level on points, point difference and win rate — genuinely tied, so " +
        "they share first place."
}

/** Plain text, ready to paste into a group chat. No markdown, no emoji, no app name. */
internal fun shareText(entries: List<LeaderboardEntry>, stats: SessionStats): String = buildString {
    appendLine("Badminton results")
    appendLine()
    entries.filter { it.rank <= 3 }.forEach { entry ->
        appendLine(
            "${entry.rank}. ${entry.player.name} — ${entry.sessionPoints} pts " +
                "(${entry.player.wins}W-${entry.player.losses}L)"
        )
    }
    appendLine()
    appendLine("Full standings")
    entries.forEach { entry ->
        val left = if (entry.player.isActive) "" else " (left early)"
        appendLine(
            "${entry.rank}. ${entry.player.name}$left — ${entry.sessionPoints} pts, " +
                "${entry.player.wins}W-${entry.player.losses}L, " +
                "${entry.player.gamesPlayed} games"
        )
    }
    appendLine()
    append("${stats.totalGames} games played.")
    stats.bestPartnership?.let {
        append(" Best pair: ${it.playerA.name} & ${it.playerB.name} (${it.wins}/${it.games}).")
    }
}
