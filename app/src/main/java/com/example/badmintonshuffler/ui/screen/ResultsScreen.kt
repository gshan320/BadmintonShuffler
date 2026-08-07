package com.example.badmintonshuffler.ui.screen

import android.content.ClipData
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import com.example.badmintonshuffler.R
import com.example.badmintonshuffler.engine.PartnershipRecord
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
        SessionAnalysis(stats)
        Spacer(Modifier.height(Space.xxl))
    }

    if (confirmingClear) {
        ConfirmDialog(
            title = "Clear everything?",
            body = "Nothing in this app is saved anywhere. Not on this phone, not on a server. " +
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
            ChampionBlock(
                entry = entry,
                // A smash is one player's. Two people who could not be separated get the box, the
                // gold and the explanation underneath, but not a picture of a single winner.
                showFigure = first.size == 1,
                revealed = revealed,
                reduceMotion = reduceMotion,
                delayIndex = index,
            )
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

/**
 * The winner's box: the one place a photograph earns its keep.
 *
 * The details move off centre and take the left half so the champion can stand in the right half at
 * full strength, rather than being faded out behind the type. Its column is reserved, so a long
 * name wraps and truncates instead of running into a racket.
 */
@Composable
private fun ChampionBlock(
    entry: LeaderboardEntry,
    showFigure: Boolean,
    revealed: Boolean,
    reduceMotion: Boolean,
    delayIndex: Int,
) {
    val progress by animateFloatAsState(
        targetValue = if (revealed) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (reduceMotion) 0 else 420,
            delayMillis = if (reduceMotion) 0 else delayIndex * 120,
        ),
        label = "podium1",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(progress)
            .background(
                CourtColors.ShuttleCork.copy(alpha = 0.16f),
                RoundedCornerShape(Radius.lg),
            )
            .border(
                Sizes.courtStroke,
                CourtColors.ShuttleCork,
                RoundedCornerShape(Radius.lg),
            )
            .padding(Space.xl),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = if (showFigure) Alignment.Start
            else Alignment.CenterHorizontally,
        ) {
            Text(
                text = ordinal(entry.rank),
                style = CourtType.Eyebrow,
                color = CourtColors.ShuttleCork,
            )
            Spacer(Modifier.height(Space.sm))
            Text(
                text = entry.player.name,
                style = CourtType.Title,
                color = CourtColors.CourtLine,
                textAlign = if (showFigure) TextAlign.Start else TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(Space.sm))
            Text(
                text = "${entry.sessionPoints}",
                style = CourtType.DisplayScore,
                color = CourtColors.ShuttleCork,
            )
            Text("points", style = CourtType.Caption, color = CourtColors.Chalk60)
            Spacer(Modifier.height(Space.xs))
            Text(
                text = "${entry.player.wins}W – ${entry.player.losses}L",
                style = CourtType.Caption,
                color = CourtColors.Chalk60,
            )
        }

        if (showFigure) {
            Spacer(Modifier.width(Space.md))
            Image(
                painter = painterResource(R.drawable.champion_player),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                alignment = Alignment.BottomCenter,
                modifier = Modifier
                    .width(Sizes.championFigureWidth)
                    .height(Sizes.championFigureHeight),
            )
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

    Column(
        modifier = modifier
            .fillMaxWidth()
            .alpha(progress)
            .background(CourtColors.ServiceBox, RoundedCornerShape(Radius.lg))
            .border(Sizes.courtStroke, CourtColors.LineFaint, RoundedCornerShape(Radius.lg))
            .padding(Space.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = ordinal(entry.rank),
            style = CourtType.Eyebrow,
            color = if (tier == 2) CourtColors.Silver else CourtColors.Bronze,
        )
        Spacer(Modifier.height(Space.sm))
        Text(
            text = entry.player.name,
            style = CourtType.SectionTitle,
            color = CourtColors.CourtLine,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(Space.sm))
        Text(
            text = "${entry.sessionPoints}",
            style = CourtType.Score,
            color = CourtColors.CourtLine,
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
// Session analysis
// ---------------------------------------------------------------------------------------------

/**
 * What the afternoon actually looked like, once the podium has had its moment.
 *
 * The same four facts the spec asks for — games played, most games, longest streak, best pair — but
 * laid out as read-at-a-glance cards rather than a label/value list, because these are the numbers
 * that get read aloud to the group while the nets are coming down.
 */
@Composable
private fun SessionAnalysis(stats: SessionStats) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("SESSION ANALYSIS", style = CourtType.Eyebrow, color = CourtColors.Chalk60)
            Spacer(Modifier.width(Space.md))
            Box(
                Modifier
                    .weight(1f)
                    .height(Sizes.hairline)
                    .background(CourtColors.LineFaint)
            )
        }
        Spacer(Modifier.height(Space.lg))

        TotalGamesCard(stats.totalGames)

        Spacer(Modifier.height(Space.md))

        // Side by side: both are "who did the most of something", so they read as a pair.
        Row(
            modifier = Modifier.height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(Space.md),
        ) {
            AnalysisCard(
                eyebrow = "MOST GAMES",
                value = if (stats.mostGamesPlayers.isEmpty()) "—" else "${stats.mostGamesPlayed}",
                detail = nameList(stats.mostGamesPlayers.map { it.name }, "Nobody finished a game."),
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
            AnalysisCard(
                eyebrow = "LONGEST STREAK",
                value = if (stats.longestWinStreakPlayers.isEmpty()) "—"
                else "${stats.longestWinStreak}",
                detail = nameList(
                    names = stats.longestWinStreakPlayers.map { it.name },
                    fallback = "No one won a game.",
                ),
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
        }

        Spacer(Modifier.height(Space.md))
        PartnershipCard(stats.bestPartnership)
    }
}

/**
 * The headline number, laid out sideways so the count and its label share the width instead of
 * leaving a stacked card two thirds empty.
 */
@Composable
private fun TotalGamesCard(totalGames: Int) {
    CardSurface(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "$totalGames",
                style = CourtType.DisplayScore,
                color = CourtColors.ShuttleCork,
            )
            Spacer(Modifier.width(Space.lg))
            Column {
                Text("GAMES PLAYED", style = CourtType.Eyebrow, color = CourtColors.Chalk60)
                Spacer(Modifier.height(Space.xxs))
                Text(
                    text = if (totalGames == 0) "Nothing was scored before the session ended."
                    else "completed and scored across the afternoon",
                    style = CourtType.Caption,
                    color = CourtColors.Chalk60,
                )
            }
        }
    }
}

/** One fact: what it is, the number, and who it belongs to. */
@Composable
private fun AnalysisCard(
    eyebrow: String,
    value: String,
    detail: String,
    modifier: Modifier = Modifier,
    valueStyle: TextStyle = CourtType.Score,
    valueColor: Color = CourtColors.CourtLine,
) {
    CardSurface(modifier) {
        Text(eyebrow, style = CourtType.Eyebrow, color = CourtColors.Chalk60)
        Spacer(Modifier.height(Space.sm))
        Text(value, style = valueStyle, color = valueColor)
        Spacer(Modifier.height(Space.xs))
        Text(
            text = detail,
            style = CourtType.Caption,
            color = CourtColors.Chalk60,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * The pair who won most often together. Gets a meter rather than a big number, because a
 * partnership is a ratio — "4 of 5" means nothing without the 5 next to it.
 */
@Composable
private fun PartnershipCard(record: PartnershipRecord?) {
    CardSurface(Modifier.fillMaxWidth()) {
        Text("BEST PARTNERSHIP", style = CourtType.Eyebrow, color = CourtColors.Chalk60)
        Spacer(Modifier.height(Space.sm))

        // No big dash here: a lone "—" at 40sp reads as a rendering fault rather than an absence.
        if (record == null) {
            Text("No repeat pairs", style = CourtType.SectionTitle, color = CourtColors.Chalk60)
            Spacer(Modifier.height(Space.xs))
            Text(
                text = "Nobody was drawn with the same partner twice, so there is no pair to " +
                    "compare.",
                style = CourtType.Caption,
                color = CourtColors.Chalk60,
            )
            return@CardSurface
        }

        Text(
            text = "${record.playerA.name} & ${record.playerB.name}",
            style = CourtType.SectionTitle,
            color = CourtColors.CourtLine,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(Space.md))
        Row(verticalAlignment = Alignment.CenterVertically) {
            WinRateMeter(record.winRate, Modifier.weight(1f))
            Spacer(Modifier.width(Space.md))
            Text(
                text = "${record.wins}/${record.games}",
                style = CourtType.NumericSmall,
                color = CourtColors.FairGreen,
            )
        }
        Spacer(Modifier.height(Space.sm))
        Text(
            text = "won ${record.wins} of ${record.games} games played together",
            style = CourtType.Caption,
            color = CourtColors.Chalk60,
        )
    }
}

/** Decorative — the fraction beside it carries the same information as text. */
@Composable
private fun WinRateMeter(winRate: Float, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(Sizes.meterHeight)
            .background(CourtColors.LineFaint, RoundedCornerShape(Radius.pill))
    ) {
        val filled = winRate.coerceIn(0f, 1f)
        if (filled > 0f) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(filled)
                    .background(CourtColors.FairGreen, RoundedCornerShape(Radius.pill))
            )
        }
    }
}

@Composable
private fun CardSurface(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .background(CourtColors.ServiceBox, RoundedCornerShape(Radius.md))
            .border(Sizes.hairline, CourtColors.LineFaint, RoundedCornerShape(Radius.md))
            .padding(Space.lg),
        content = content,
    )
}

// ---------------------------------------------------------------------------------------------
// Words
// ---------------------------------------------------------------------------------------------

/**
 * "Ali", "Ali and Sam", "Ali, Sam and Jo" — or the fallback when the stat has no holder.
 *
 * A well-rotated session ties a lot: every player finishing on the same games played is the engine
 * working, not a special case. Past three names the list stops being readable and starts being a
 * paragraph, so it counts instead.
 */
private fun nameList(names: List<String>, fallback: String): String = when (names.size) {
    0 -> fallback
    1 -> names[0]
    2, 3 -> names.dropLast(1).joinToString(", ") + " and " + names.last()
    else -> "${names[0]}, ${names[1]} and ${names.size - 2} others"
}

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
    return "$names finished level on points, point difference and win rate. Tied, so " +
        "they share First place."
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
