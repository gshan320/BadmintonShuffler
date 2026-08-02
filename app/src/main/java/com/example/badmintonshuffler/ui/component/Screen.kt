package com.example.badmintonshuffler.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.badmintonshuffler.ui.theme.CourtColors
import com.example.badmintonshuffler.ui.theme.Space

/**
 * Every screen's outer shell: the court-mat background, safe-area insets and one consistent gutter.
 *
 * [bottomBar] is pinned above the navigation bar and never scrolls, because the primary action has
 * to stay under a thumb while the organiser is holding a racket in the other hand.
 */
@Composable
fun Screen(
    modifier: Modifier = Modifier,
    scrollable: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = Space.screenGutter),
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    bottomBar: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CourtColors.CourtDeep)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            val bodyModifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .statusBarsPadding()
                .then(if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                .padding(contentPadding)

            Column(
                modifier = bodyModifier,
                verticalArrangement = verticalArrangement,
                content = content,
            )

            if (bottomBar != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CourtColors.NetTape)
                        .navigationBarsPadding()
                        .padding(
                            start = Space.screenGutter,
                            end = Space.screenGutter,
                            top = Space.md,
                            bottom = Space.md,
                        )
                ) {
                    bottomBar()
                }
            } else {
                Box(Modifier.navigationBarsPadding().fillMaxWidth())
            }
        }
    }
}
