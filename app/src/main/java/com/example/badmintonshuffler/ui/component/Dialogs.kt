package com.example.badmintonshuffler.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.badmintonshuffler.ui.theme.CourtColors
import com.example.badmintonshuffler.ui.theme.CourtType
import com.example.badmintonshuffler.ui.theme.Radius
import com.example.badmintonshuffler.ui.theme.Sizes
import com.example.badmintonshuffler.ui.theme.Space

/**
 * The one confirmation pattern in the app.
 *
 * [destructive] is not decoration — nothing in this app is saved anywhere, so a destructive
 * confirmation is the only thing standing between an afternoon of results and nothing at all. The
 * body text is expected to say so plainly.
 */
@Composable
fun ConfirmDialog(
    title: String,
    body: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    dismissText: String = "Cancel",
    destructive: Boolean = false,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .background(CourtColors.CourtDeep, RoundedCornerShape(Radius.lg))
                .border(Sizes.hairline, CourtColors.LineFaint, RoundedCornerShape(Radius.lg))
                .padding(Space.xl),
        ) {
            Text(text = title, style = CourtType.SectionTitle, color = CourtColors.CourtLine)
            Spacer(Modifier.height(Space.md))
            Text(text = body, style = CourtType.Body17, color = CourtColors.Chalk60)
            Spacer(Modifier.height(Space.xl))

            if (destructive) {
                DangerButton(text = confirmText, onClick = onConfirm)
            } else {
                PrimaryButton(text = confirmText, onClick = onConfirm)
            }
            Spacer(Modifier.height(Space.sm))
            SecondaryButton(text = dismissText, onClick = onDismiss)
        }
    }
}
