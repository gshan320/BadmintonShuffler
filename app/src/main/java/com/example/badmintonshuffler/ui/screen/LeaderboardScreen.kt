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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.Color
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
                body = "Play a round and record a score — the table fills itself in.",
            )
        } else {
            StandingsTable(entries)
        }

        Spacer(Modifier.height(Space.xxl))
    }
}

@Composable
fun StandingsTable(entries: List<LeaderboardEntry>, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Space.md, vertical = Space.sm),
        ) {
            HeaderCell("#", Modifier.width(Sizes.rankColumn))
            HeaderCell("PLAYER", Modifier.weight(1f), TextAlign.Start)
            HeaderCell("GP", Modifier.width(Sizes.gamesColumn))
            HeaderCell("W-L", Modifier.width(Sizes.recordColumn))
            HeaderCell("+/-", Modifier.width(Sizes.diffColumn))
            HeaderCell("PTS", Modifier.width(Sizes.pointsColumn))
        }

        Column(verticalArrangement = Arrangement.spacedBy(Space.xs)) {
            entries.forEach { entry -> StandingsRow(entry) }
        }
    }
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
        Cell("${entry.rank}", Modifier.width(Sizes.rankColumn), CourtColors.Chalk60)
        Column(modifier = Modifier.weight(1f).padding(end = Space.sm)) {
            Text(
                text = player.name,
                style = CourtType.PlayerName,
                color = CourtColors.CourtLine,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!player.isActive) {
                Text("Left early", style = CourtType.Caption, color = CourtColors.Chalk60)
            }
        }
        Cell("${player.gamesPlayed}", Modifier.width(Sizes.gamesColumn), CourtColors.Chalk60)
        Cell("${player.wins}-${player.losses}", Modifier.width(Sizes.recordColumn), CourtColors.Chalk60)
        Cell(
            text = if (entry.pointDifferential > 0) "+${entry.pointDifferential}"
            else "${entry.pointDifferential}",
            modifier = Modifier.width(Sizes.diffColumn),
            color = when {
                entry.pointDifferential > 0 -> CourtColors.FairGreen
                entry.pointDifferential < 0 -> CourtColors.FaultRed
                else -> CourtColors.Chalk60
            },
        )
        Cell("${entry.sessionPoints}", Modifier.width(Sizes.pointsColumn), CourtColors.ShuttleCork)
    }
}

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

@Composable
private fun Cell(text: String, modifier: Modifier = Modifier, color: Color) {
    Text(
        text = text,
        style = CourtType.Numeric,
        color = color,
        textAlign = TextAlign.End,
        modifier = modifier,
    )
}
