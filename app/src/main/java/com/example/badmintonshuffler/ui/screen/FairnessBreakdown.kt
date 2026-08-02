package com.example.badmintonshuffler.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.badmintonshuffler.engine.FairnessReport
import com.example.badmintonshuffler.model.LeaderboardEntry
import com.example.badmintonshuffler.ui.component.SecondaryButton
import com.example.badmintonshuffler.ui.theme.CourtColors
import com.example.badmintonshuffler.ui.theme.CourtType
import com.example.badmintonshuffler.ui.theme.Radius
import com.example.badmintonshuffler.ui.theme.Sizes
import com.example.badmintonshuffler.ui.theme.Space

/**
 * Who has played how many games — the answer to "why is that pill amber".
 *
 * A bar per player rather than a column of numbers, because the question is comparative and a
 * length is faster to compare than a digit.
 */
@Composable
fun FairnessBreakdownDialog(
    entries: List<LeaderboardEntry>,
    report: FairnessReport,
    onDismiss: () -> Unit,
) {
    val spread = report.spread
    val active = entries.filter { it.player.isActive }.sortedBy { it.player.gamesPlayed }
    val maxGames = active.maxOfOrNull { it.player.gamesPlayed } ?: 0

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .background(CourtColors.CourtDeep, RoundedCornerShape(Radius.lg))
                .border(Sizes.hairline, CourtColors.LineFaint, RoundedCornerShape(Radius.lg))
                .padding(Space.xl),
        ) {
            Text("Games played", style = CourtType.SectionTitle, color = CourtColors.CourtLine)
            Spacer(Modifier.height(Space.sm))
            Text(
                text = when {
                    active.isEmpty() -> "Nobody is on the roster yet."
                    spread <= 1 && report.hasLateArrivals ->
                        "Court time is being shared evenly. The totals below differ because some " +
                            "people joined after the session started — they are getting the same " +
                            "number of games as everyone else from the round they arrived."
                    spread <= 1 -> "Everyone is within one game of everyone else. That is as even " +
                        "as doubles gets."
                    else -> "There is a $spread game gap. The shuffler puts the least-played " +
                        "people on first, so this closes on its own over the next round or two."
                },
                style = CourtType.Body17,
                color = CourtColors.Chalk60,
            )

            Spacer(Modifier.height(Space.lg))

            Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                active.forEach { entry ->
                    Row(
                        modifier = Modifier.fillMaxWidth().heightIn(min = Space.xxl),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = entry.player.name,
                            style = CourtType.Caption,
                            color = CourtColors.CourtLine,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(0.9f),
                        )
                        Box(
                            modifier = Modifier
                                .weight(1.6f)
                                .height(Space.md)
                                .background(CourtColors.NetTape, RoundedCornerShape(Radius.pill)),
                        ) {
                            val fraction = if (maxGames == 0) 0f
                            else entry.player.gamesPlayed.toFloat() / maxGames
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction)
                                    .height(Space.md)
                                    .background(
                                        CourtColors.ShuttleCork,
                                        RoundedCornerShape(Radius.pill),
                                    )
                            )
                        }
                        Spacer(Modifier.height(Space.sm))
                        Text(
                            text = "  ${entry.player.gamesPlayed}",
                            style = CourtType.Numeric,
                            color = CourtColors.CourtLine,
                            modifier = Modifier.width(Sizes.gamesColumn),
                        )
                    }
                }
            }

            Spacer(Modifier.height(Space.xl))
            SecondaryButton("Close", onDismiss)
        }
    }
}
