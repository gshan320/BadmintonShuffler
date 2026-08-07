package com.example.badmintonshuffler.ui.screen

import androidx.compose.foundation.background
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextOverflow

import com.example.badmintonshuffler.model.LeaderboardEntry
import com.example.badmintonshuffler.ui.component.EmptyState
import com.example.badmintonshuffler.ui.component.SecondaryButton
import com.example.badmintonshuffler.ui.component.Screen
import com.example.badmintonshuffler.ui.theme.CourtColors
import com.example.badmintonshuffler.ui.theme.CourtType
import com.example.badmintonshuffler.ui.theme.Radius
import com.example.badmintonshuffler.ui.theme.Sizes
import com.example.badmintonshuffler.ui.theme.Space

/**
 * The live standings. Updates as scores come in, so it can be handed round mid-session.
 */
@Composable
fun LeaderboardScreen(
    entries: List<LeaderboardEntry>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Screen(
        modifier = modifier,
        bottomBar = { SecondaryButton("Back to the session", onBack) },
    ) {
        Spacer(Modifier.height(Space.md))
        Text("Standings", style = CourtType.Title, color = CourtColors.CourtLine)
        Spacer(Modifier.height(Space.xl))

        if (entries.isEmpty()) {
            EmptyState(
                title = "No games yet",
                body = "Play a round and record a score. The table fills itself in.",
            )
        } else {
            StandingsTable(entries)
        }

        Spacer(Modifier.height(Space.xxl))
    }
}

/**
 * Three zones, left to right: the index in its own gutter, the name with everything it earned
 * underneath it, and the points that decide the order.
 *
 * The index sits hard against the left edge rather than right-aligned beside the name, so a long
 * name starts at the same x on every row and reads as a name rather than as the tail of a number.
 */
@Composable
fun StandingsTable(entries: List<LeaderboardEntry>, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Space.md, vertical = Space.sm),
        ) {
            HeaderCell("#", Modifier.width(Sizes.rankColumn), TextAlign.Start)
            Spacer(Modifier.width(Space.sm))
            HeaderCell("PLAYER", Modifier.weight(1f), TextAlign.Start)
            HeaderCell("PTS", Modifier.width(Sizes.pointsColumn))
        }

        Column(verticalArrangement = Arrangement.spacedBy(Space.xs)) {
            entries.forEach { entry -> StandingsRow(entry) }
        }
    }
}

/**
 * Gold, silver and bronze on the rank, and nothing else.
 *
 * The table is a list someone scans for their own name, so the top three are marked by a shade
 * rather than by a box that makes every row below it look like an also-ran.
 */
private fun medalColor(rank: Int): Color = when (rank) {
    1 -> CourtColors.Gold
    2 -> CourtColors.Silver
    3 -> CourtColors.Bronze
    else -> CourtColors.Chalk60
}

@Composable
private fun StandingsRow(entry: LeaderboardEntry) {
    val player = entry.player
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Sizes.minTapTarget)
            .background(CourtColors.ServiceBox, RoundedCornerShape(Radius.sm))
            .padding(horizontal = Space.md, vertical = Space.sm)
            .alpha(if (player.isActive) 1f else 0.55f)
            .semantics(mergeDescendants = true) {
                contentDescription = "Rank ${entry.rank}, ${player.name}, " +
                    "${entry.sessionPoints} points, ${player.wins} wins ${player.losses} losses, " +
                    "${player.gamesPlayed} games played" +
                    if (!player.isActive) ", left early" else ""
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "${entry.rank}",
            style = CourtType.NumericSmall,
            color = medalColor(entry.rank),
            textAlign = TextAlign.Start,
            modifier = Modifier.width(Sizes.rankColumn),
        )
        Spacer(Modifier.width(Space.sm))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = player.name,
                style = CourtType.PlayerName,
                color = CourtColors.CourtLine,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(Space.xxs))
            Text(
                text = recordLine(entry),
                style = CourtType.Caption,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(Space.sm))
        Text(
            text = "${entry.sessionPoints}",
            style = CourtType.Numeric,
            color = CourtColors.ShuttleCork,
            textAlign = TextAlign.End,
            modifier = Modifier.width(Sizes.pointsColumn),
        )
    }
}

/**
 * "6 GP · 4W–2L · +11", as one string so it ellipsises from the end on a narrow screen instead of
 * dropping a column. The differential keeps its umpire colour; everything else is secondary text.
 */
@Composable
private fun recordLine(entry: LeaderboardEntry): AnnotatedString {
    val player = entry.player
    val diff = if (entry.pointDifferential > 0) "+${entry.pointDifferential}"
    else "${entry.pointDifferential}"
    val diffColor = when {
        entry.pointDifferential > 0 -> CourtColors.FairGreen
        entry.pointDifferential < 0 -> CourtColors.FaultRed
        else -> CourtColors.Chalk60
    }

    return buildAnnotatedString {
        withStyle(SpanStyle(color = CourtColors.Chalk60)) {
            append("${player.gamesPlayed} GP")
            append(SEPARATOR)
            append("${player.wins}W–${player.losses}L")
            append(SEPARATOR)
        }
        withStyle(SpanStyle(color = diffColor)) { append(diff) }
        if (!player.isActive) {
            withStyle(SpanStyle(color = CourtColors.Chalk60)) {
                append(SEPARATOR)
                append("left early")
            }
        }
    }
}

private const val SEPARATOR = "  ·  "

@Composable
private fun HeaderCell(
    text: String,
    modifier: Modifier = Modifier,
    align: TextAlign = TextAlign.End,
) {
    Box(modifier = modifier) {
        Text(
            text = text,
            style = CourtType.Eyebrow,
            color = CourtColors.Inert,
            textAlign = align,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
