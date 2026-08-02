package com.example.badmintonshuffler.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.badmintonshuffler.BuildConfig
import com.example.badmintonshuffler.ui.component.PrimaryButton
import com.example.badmintonshuffler.ui.component.Screen
import com.example.badmintonshuffler.ui.component.SecondaryButton
import com.example.badmintonshuffler.ui.theme.CourtColors
import com.example.badmintonshuffler.ui.theme.CourtType
import com.example.badmintonshuffler.ui.theme.Space

/**
 * The start screen. One thing to do, so exactly one button does it.
 */
@Composable
fun HomeScreen(
    onNewSession: () -> Unit,
    onSeedDemo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Screen(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        bottomBar = {
            PrimaryButton(text = "New session", onClick = onNewSession)
            if (BuildConfig.DEBUG) {
                Spacer(Modifier.height(Space.sm))
                SecondaryButton(text = "Demo session (debug)", onClick = onSeedDemo)
            }
        },
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column {
                Text(
                    text = "COURT",
                    style = CourtType.Eyebrow,
                    color = CourtColors.ShuttleCork,
                )
                Spacer(Modifier.height(Space.sm))
                Text(
                    text = "Shuffler",
                    style = CourtType.DisplayScore,
                    color = CourtColors.CourtLine,
                )
                Spacer(Modifier.height(Space.lg))
                Text(
                    text = "Set up an afternoon of doubles, and let it decide who plays with whom. " +
                        "Everyone gets the same number of games.",
                    style = CourtType.Body17,
                    color = CourtColors.Chalk60,
                )
                Spacer(Modifier.height(Space.xl))
                Text(
                    text = "Nothing is saved. Close the app and the session is gone — it is one " +
                        "afternoon, not a database.",
                    style = CourtType.Caption,
                    color = CourtColors.Inert,
                )
            }
        }
    }
}
