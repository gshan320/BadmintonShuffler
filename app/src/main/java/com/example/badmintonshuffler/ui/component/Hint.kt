package com.example.badmintonshuffler.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.badmintonshuffler.ui.theme.CourtColors
import com.example.badmintonshuffler.ui.theme.CourtType
import com.example.badmintonshuffler.ui.theme.Radius
import com.example.badmintonshuffler.ui.theme.Space

/**
 * A quiet block of explanation.
 *
 * This is where the app talks: the live consequence of a setting, or the fairness rule stated in
 * plain words. Used on the setup screens, the score sheet and the roster sheet, so it belongs here
 * rather than in whichever screen happened to need it first.
 */
@Composable
fun Hint(text: String, modifier: Modifier = Modifier, isError: Boolean = false) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                if (isError) CourtColors.FaultRed.copy(alpha = 0.12f) else CourtColors.ServiceBox,
                RoundedCornerShape(Radius.md),
            )
            .padding(Space.lg),
    ) {
        Text(
            text = text,
            style = CourtType.Body17,
            color = if (isError) CourtColors.FaultRed else CourtColors.Chalk60,
        )
    }
}
