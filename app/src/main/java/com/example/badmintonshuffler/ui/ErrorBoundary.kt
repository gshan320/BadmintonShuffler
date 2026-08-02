package com.example.badmintonshuffler.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.badmintonshuffler.ui.component.PrimaryButton
import com.example.badmintonshuffler.ui.component.Screen
import com.example.badmintonshuffler.ui.theme.CourtColors
import com.example.badmintonshuffler.ui.theme.CourtType
import com.example.badmintonshuffler.ui.theme.Space

/**
 * Catches a render crash and offers to carry on instead of taking the session down with it.
 *
 * This matters more here than in most apps: the session exists only in memory, so a crash that
 * kills the process is an afternoon of scores gone. The state itself lives in the ViewModel and
 * survives a failed composition, so recomposing really can recover.
 *
 * Compose has no `componentDidCatch`, so this catches in the composable body. It cannot catch
 * failures thrown during layout or draw — those still reach the default handler.
 */
@Composable
fun ErrorBoundary(content: @Composable () -> Unit) {
    var failure by remember { mutableStateOf<Throwable?>(null) }
    var attempt by remember { mutableStateOf(0) }

    val current = failure
    if (current == null) {
        // `attempt` is read so that bumping it re-enters the try and re-runs the content.
        @Suppress("UNUSED_EXPRESSION")
        attempt
        runCatching { content() }.onFailure { failure = it }
    } else {
        Screen(verticalArrangement = Arrangement.Center) {
            Text("Something broke", style = CourtType.Title, color = CourtColors.CourtLine)
            Spacer(Modifier.height(Space.md))
            Text(
                text = "The screen failed to draw, but your session is still here — rounds, " +
                    "scores and all. Carrying on should bring it back.",
                style = CourtType.Body17,
                color = CourtColors.Chalk60,
            )
            Spacer(Modifier.height(Space.lg))
            Text(
                text = current.message ?: current::class.simpleName ?: "Unknown error",
                style = CourtType.Caption,
                color = CourtColors.Inert,
            )
            Spacer(Modifier.height(Space.xl))
            PrimaryButton(
                text = "Continue session",
                onClick = { failure = null; attempt++ },
            )
        }
    }
}
