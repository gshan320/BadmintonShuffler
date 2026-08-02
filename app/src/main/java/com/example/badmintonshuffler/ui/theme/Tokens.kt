package com.example.badmintonshuffler.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.DeviceFontFamilyName
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The design tokens. Screens use these and nothing else — no literal colours, sizes or font sizes.
 *
 * Design brief: a phone held in one sweaty hand in a noisy sports hall, glanced at between games,
 * often from an arm's length away across a court. Legibility and big targets beat density. The
 * visual language comes from the sport — court markings, the chalk-white lines on the mat, the
 * geometry of the service boxes, the feel of a hall scoreboard — not from a dashboard.
 */

// ---------------------------------------------------------------------------------------------
// Palette
// ---------------------------------------------------------------------------------------------

/**
 * Six colours, each taken from something you can point at in a badminton hall.
 *
 * Every foreground/background pairing used in the app clears WCAG AA (4.5:1); the numbers in the
 * comments are measured, not estimated.
 */
object CourtColors {
    /** The deep teal of a PVC court mat under hall lighting. The app's floor — the base surface. */
    val CourtDeep = Color(0xFF0E3538)

    /** One step up from the mat: the panel inside the doubles boundary. Cards and sheets sit here. */
    val ServiceBox = Color(0xFF16494C)

    /** The near-black of net tape. The recess behind everything, and text on bright accents. */
    val NetTape = Color(0xFF08211F)

    /** Chalk-white court line paint. Primary text and every boundary stroke. 12.2:1 on CourtDeep. */
    val CourtLine = Color(0xFFF3F6F1)

    /** A court line seen from the far end — faded, still legible. Secondary text. 6.8:1. */
    val Chalk60 = Color(0xFFA9BEBB)

    /** The tan cork base of a shuttlecock: the one warm thing on court. Primary action. 6.1:1. */
    val ShuttleCork = Color(0xFFE8A251)

    // Umpire's signals — used only to say something is or isn't right.
    /** Fairness is holding. 6.6:1 on CourtDeep. */
    val FairGreen = Color(0xFF5FCB8B)

    /** Fairness is drifting. 8.0:1. */
    val WarnAmber = Color(0xFFF0C34C)

    /** A fault: destructive actions and rejected scores. 6.3:1. */
    val FaultRed = Color(0xFFF59C8E)

    /** Hairline court markings and dividers. */
    val LineFaint = Color(0x33F3F6F1)
    val LineStrong = Color(0x66F3F6F1)

    /** Disabled foreground — present, clearly inert. */
    val Inert = Color(0xFF5C7A79)
}

// ---------------------------------------------------------------------------------------------
// Type
// ---------------------------------------------------------------------------------------------

/**
 * Two faces, two jobs.
 *
 * Scores and counts get a condensed face with tabular figures, because a scoreboard is condensed and
 * because a "1" and a "7" must occupy the same width — otherwise the score visibly jitters as it is
 * stepped up and down, which looks broken. Everything else gets the plain system face.
 *
 * Both are device fonts, so there is no font download, no network call and no licence to track.
 */
object CourtType {
    val Scoreboard = FontFamily(Font(DeviceFontFamilyName("sans-serif-condensed")))
    val Body = FontFamily.SansSerif

    /** Fixed-width digits. Without this, stepping 19 -> 20 shifts the whole number sideways. */
    private const val TABULAR = "tnum"

    /** The winning score on a completed card, and the podium. */
    val DisplayScore = TextStyle(
        fontFamily = Scoreboard,
        fontWeight = FontWeight.Bold,
        fontSize = 56.sp,
        lineHeight = 58.sp,
        letterSpacing = (-1).sp,
        fontFeatureSettings = TABULAR,
    )

    /** Score steppers and court-card scorelines. */
    val Score = TextStyle(
        fontFamily = Scoreboard,
        fontWeight = FontWeight.Bold,
        fontSize = 40.sp,
        lineHeight = 44.sp,
        fontFeatureSettings = TABULAR,
    )

    /** "Round 4" — read at a glance from across the court. */
    val RoundTitle = TextStyle(
        fontFamily = Scoreboard,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 34.sp,
        fontFeatureSettings = TABULAR,
    )

    /** Any standalone number: court number, games played, player counts. */
    val Numeric = TextStyle(
        fontFamily = Scoreboard,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 26.sp,
        fontFeatureSettings = TABULAR,
    )

    /** Table cells. Still tabular, sized so a six-column standings row fits a 375dp screen. */
    val NumericSmall = TextStyle(
        fontFamily = Scoreboard,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 20.sp,
        fontFeatureSettings = TABULAR,
    )

    /** Screen questions. One per screen, so it can afford to be large. */
    val Title = TextStyle(
        fontFamily = Body,
        fontWeight = FontWeight.Bold,
        fontSize = 27.sp,
        lineHeight = 33.sp,
    )

    val SectionTitle = TextStyle(
        fontFamily = Body,
        fontWeight = FontWeight.SemiBold,
        fontSize = 19.sp,
        lineHeight = 24.sp,
    )

    /** Names on a court card — the thing people are actually looking for. */
    val PlayerName = TextStyle(
        fontFamily = Body,
        fontWeight = FontWeight.Medium,
        fontSize = 17.sp,
        lineHeight = 21.sp,
    )

    val Body17 = TextStyle(
        fontFamily = Body,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 24.sp,
    )

    val Label = TextStyle(
        fontFamily = Body,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.2.sp,
    )

    val Caption = TextStyle(
        fontFamily = Body,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    )

    /** Small, wide and shouty — section eyebrows like "RESTING THIS ROUND". */
    val Eyebrow = TextStyle(
        fontFamily = Body,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 1.4.sp,
    )
}

// ---------------------------------------------------------------------------------------------
// Metrics
// ---------------------------------------------------------------------------------------------

/** 4dp base scale. If a screen needs a value that isn't here, the screen is wrong. */
object Space {
    val xxs = 2.dp
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
    val xxxl = 48.dp

    /** Consistent horizontal inset for every screen. */
    val screenGutter = 20.dp
}

object Radius {
    val sm = 8.dp
    val md = 14.dp
    val lg = 22.dp
    val pill = 999.dp
}

/** Two levels, no more. Depth is for separating a sheet from the court, nothing else. */
object Elevation {
    val raised = 2.dp
    val floating = 12.dp
}

object Sizes {
    /** Nothing interactive is ever smaller than this. Sweaty hands, no reading glasses. */
    val minTapTarget = 48.dp
    val controlHeight = 56.dp
    val stepperButton = 56.dp
    val netThickness = 2.dp
    val courtStroke = 2.dp

    /** The faint one-pixel rule used on chips, dialogs and empty states. */
    val hairline = 1.dp

    val iconDefault = 24.dp
    val iconSmall = 20.dp

    /** Court-card geometry: a half court, its centre service line, and the scoreboard column. */
    val halfCourtMinHeight = 72.dp
    val centreLineHeight = 40.dp
    val courtScoreColumn = 72.dp

    /** A name on a chip truncates past this rather than pushing the remove button off screen. */
    val chipNameMaxWidth = 180.dp

    /**
     * Standings table columns. Fixed widths so the numbers line up down the page.
     *
     * These total 176dp. On the narrowest common phone (375dp) that leaves roughly 135dp for the
     * name after gutters and row padding — enough for a first name plus an initial before it
     * ellipsises, which is what these lists actually contain.
     */
    val rankColumn = 26.dp
    val gamesColumn = 30.dp
    val recordColumn = 50.dp
    val diffColumn = 40.dp
    val pointsColumn = 40.dp

    /** The "Add" button next to a name field. */
    val inlineActionWidth = 96.dp
}
