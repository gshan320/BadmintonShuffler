package com.example.badmintonshuffler.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

/**
 * One theme, always. No light mode and no dynamic colour.
 *
 * This is deliberate: the app should look like the court it is standing on, and a hall is bright
 * enough that a dark, high-contrast surface is the easiest thing to read at arm's length. Letting
 * the system repaint it in the user's wallpaper colours would throw away the only visual idea the
 * app has.
 */
private val CourtScheme = darkColorScheme(
    primary = CourtColors.ShuttleCork,
    onPrimary = CourtColors.NetTape,
    primaryContainer = CourtColors.ShuttleCork,
    onPrimaryContainer = CourtColors.NetTape,

    secondary = CourtColors.CourtLine,
    onSecondary = CourtColors.NetTape,
    secondaryContainer = CourtColors.ServiceBox,
    onSecondaryContainer = CourtColors.CourtLine,

    tertiary = CourtColors.FairGreen,
    onTertiary = CourtColors.NetTape,

    background = CourtColors.CourtDeep,
    onBackground = CourtColors.CourtLine,

    surface = CourtColors.CourtDeep,
    onSurface = CourtColors.CourtLine,
    surfaceVariant = CourtColors.ServiceBox,
    onSurfaceVariant = CourtColors.Chalk60,
    surfaceContainer = CourtColors.ServiceBox,
    surfaceContainerHigh = CourtColors.ServiceBox,
    surfaceContainerLow = CourtColors.NetTape,
    inverseSurface = CourtColors.CourtLine,
    inverseOnSurface = CourtColors.NetTape,

    error = CourtColors.FaultRed,
    onError = CourtColors.NetTape,
    errorContainer = CourtColors.FaultRed,
    onErrorContainer = CourtColors.NetTape,

    outline = CourtColors.LineStrong,
    outlineVariant = CourtColors.LineFaint,
    scrim = CourtColors.NetTape,
)

/** Maps our roles onto Material's slots, so stock components inherit the language for free. */
private val CourtTypography = Typography(
    displayLarge = CourtType.DisplayScore,
    displayMedium = CourtType.Score,
    headlineLarge = CourtType.Title,
    headlineMedium = CourtType.RoundTitle,
    titleLarge = CourtType.SectionTitle,
    titleMedium = CourtType.PlayerName,
    bodyLarge = CourtType.Body17,
    bodyMedium = CourtType.Body17,
    bodySmall = CourtType.Caption,
    labelLarge = CourtType.Label,
    labelMedium = CourtType.Label,
    labelSmall = CourtType.Eyebrow,
)

@Composable
fun CourtShufflerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CourtScheme,
        typography = CourtTypography,
        content = content,
    )
}
