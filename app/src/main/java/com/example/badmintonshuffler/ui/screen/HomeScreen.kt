package com.example.badmintonshuffler.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.badmintonshuffler.R
import com.example.badmintonshuffler.ui.component.PrimaryButton
import com.example.badmintonshuffler.ui.component.Screen
import com.example.badmintonshuffler.ui.theme.CourtColors
import com.example.badmintonshuffler.ui.theme.CourtType
import com.example.badmintonshuffler.ui.theme.Ratios
import com.example.badmintonshuffler.ui.theme.Space

/**
 * The start screen. One thing to do, so exactly one button does it.
 *
 * The photograph owns the top of the screen and the words own the bottom, with the scrim between
 * them doing the work: nothing is set over the busy part of the image, so every line of type is
 * still on a flat dark ground and still clears AA.
 */
@Composable
fun HomeScreen(
    onNewSession: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Screen(
        modifier = modifier,
        // The copy is short and fixed; letting it sit on the floor of the screen is what keeps it
        // clear of the shuttle above.
        scrollable = false,
        verticalArrangement = Arrangement.Bottom,
        background = { HomeBackdrop() },
        bottomBarColor = Color.Transparent,
        bottomBar = { PrimaryButton(text = "New session", onClick = onNewSession) },
    ) {
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
            text = "Set up an session of doubles, and let it decide who plays with whom. " +
                "Everyone gets the same number of games.",
            style = CourtType.Body17,
            color = CourtColors.Chalk60,
        )
        Spacer(Modifier.height(Space.xl))
        Text(
            text = "Nothing is saved. Close the app and the session is gone.",
            style = CourtType.Caption,
            color = CourtColors.Chalk60,
        )
        Spacer(Modifier.height(Space.xl))
    }
}

/**
 * The photograph, and the gradient that makes it safe to write on.
 *
 * The image is square and the phone is not, so it is given the top of the screen rather than the
 * whole of it: stretched to full height it would be magnified past three times and crop to a narrow
 * strip through the middle. The scrim starts dark at the status bar, opens up over the subject, and
 * closes to solid court-mat before the wordmark begins.
 */
@Composable
private fun BoxScope.HomeBackdrop() {
    Image(
        painter = painterResource(R.drawable.racket_shuttle),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        alignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(Ratios.homeImageHeight)
            .align(Alignment.TopCenter),
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to CourtColors.NetTape.copy(alpha = 0.62f),
                    0.16f to CourtColors.CourtDeep.copy(alpha = 0.20f),
                    // At a large font scale the copy grows up into this band, so it carries more
                    // scrim than the composition alone would need.
                    0.40f to CourtColors.CourtDeep.copy(alpha = 0.68f),
                    Ratios.homeImageHeight to CourtColors.CourtDeep,
                    1f to CourtColors.CourtDeep,
                )
            )
    )
}
