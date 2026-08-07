package com.example.badmintonshuffler.ui.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.example.badmintonshuffler.ui.rememberReduceMotion
import com.example.badmintonshuffler.ui.theme.CourtColors
import com.example.badmintonshuffler.ui.theme.CourtType
import com.example.badmintonshuffler.ui.theme.Sizes
import com.example.badmintonshuffler.ui.theme.Space

/**
 * A badminton court chalking itself in, holding for a beat, then wiping itself out.
 *
 * Nothing in this app is genuinely slow — there is no network and no disk — so this is not a
 * spinner standing in for latency. It is the beat between asking for a draw and seeing it, and it
 * is deliberately the length of the animation rather than the length of the work.
 *
 * The proportions below are the real court, in metres, expressed as fractions of its own length and
 * width. They are not design values and do not belong in the token file: a court is 13.4m by 6.1m
 * whatever this app decides to look like.
 */
private const val COURT_LENGTH_M = 13.4f
private const val COURT_WIDTH_M = 6.1f

/** 0.42m in from each doubles sideline. */
private const val SINGLES_INSET = 0.42f / COURT_WIDTH_M

/** The short service line, 1.98m from the net. */
private const val SHORT_SERVICE = 1.98f / COURT_LENGTH_M

/** The doubles long service line, 0.76m in from the back boundary. */
private const val LONG_SERVICE = 0.76f / COURT_LENGTH_M

private val COURT_ASPECT = COURT_WIDTH_M / COURT_LENGTH_M

private data class CourtLine(
    val x1: Float,
    val y1: Float,
    val x2: Float,
    val y2: Float,
    val isNet: Boolean = false,
)

/**
 * In the order someone would actually chalk them: the box, then the sidelines, then the service
 * lines, then the centre lines, and the net last because that is the bit that makes it a court.
 */
private val COURT_LINES = listOf(
    CourtLine(0f, 0f, 0f, 1f),
    CourtLine(1f, 0f, 1f, 1f),
    CourtLine(0f, 0f, 1f, 0f),
    CourtLine(0f, 1f, 1f, 1f),
    CourtLine(SINGLES_INSET, 0f, SINGLES_INSET, 1f),
    CourtLine(1f - SINGLES_INSET, 0f, 1f - SINGLES_INSET, 1f),
    CourtLine(0f, 0.5f - SHORT_SERVICE, 1f, 0.5f - SHORT_SERVICE),
    CourtLine(0f, 0.5f + SHORT_SERVICE, 1f, 0.5f + SHORT_SERVICE),
    CourtLine(0f, LONG_SERVICE, 1f, LONG_SERVICE),
    CourtLine(0f, 1f - LONG_SERVICE, 1f, 1f - LONG_SERVICE),
    CourtLine(0.5f, 0f, 0.5f, 0.5f - SHORT_SERVICE),
    CourtLine(0.5f, 0.5f + SHORT_SERVICE, 0.5f, 1f),
    CourtLine(0f, 0.5f, 1f, 0.5f, isNet = true),
)

/**
 * One full cycle: chalked in, held, wiped out, rest.
 *
 * Nothing is being waited for, so the only thing this length has to respect is the person holding
 * the phone. It is set so the court is complete and read in well under a second.
 */
private const val CYCLE_MS = 1350

// Where each stage of the cycle ends. The rest at the end is what stops a loop from feeling like a
// machine.
private const val DRAW_END = 0.44f
private const val HOLD_END = 0.60f
private const val ERASE_END = 0.94f

/**
 * How long to leave the loader up for a one-shot transition: the court fully chalked, held just
 * long enough to register, and gone.
 *
 * Derived rather than written down, so a change to the animation cannot leave a caller waiting on a
 * court that finished drawing half a second ago. Held longer than this, the loader wipes itself out
 * and starts again on its own.
 */
const val COURT_DRAWN_MS: Long = (HOLD_END * CYCLE_MS).toLong()

@Composable
fun CourtLoader(modifier: Modifier = Modifier) {
    val reduceMotion = rememberReduceMotion()

    val animated by rememberInfiniteTransition(label = "court").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(CYCLE_MS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "phase",
    )

    // Reduce motion gets the finished court and no movement at all. The point of the thing is that
    // a court is there, and that survives being held still.
    val phase = if (reduceMotion) HOLD_PHASE else animated

    Canvas(modifier = modifier.aspectRatio(COURT_ASPECT)) {
        COURT_LINES.forEachIndexed { index, line ->
            val extent = lineExtent(phase, index, COURT_LINES.size)
            if (extent != null) drawCourtLine(line, extent.first, extent.second)
        }
    }
}

/**
 * How much of a line is on the mat right now, as a (from, to) pair along its own length.
 *
 * Null while the line has not been started or has been fully wiped. Lines are staggered so the
 * court builds up rather than appearing all at once, and they are wiped in the same order they were
 * drawn, retracting from the end they started at.
 */
private fun lineExtent(phase: Float, index: Int, count: Int): Pair<Float, Float>? {
    val span = 2f / (count + 1)
    val start = index * (1f - span) / (count - 1)

    return when {
        phase < DRAW_END -> {
            val local = ease(((phase / DRAW_END - start) / span).coerceIn(0f, 1f))
            if (local <= 0f) null else 0f to local
        }

        phase < HOLD_END -> 0f to 1f

        phase < ERASE_END -> {
            val wipe = (phase - HOLD_END) / (ERASE_END - HOLD_END)
            val local = ease(((wipe - start) / span).coerceIn(0f, 1f))
            if (local >= 1f) null else local to 1f
        }

        else -> null
    }
}

/** Ease out, so a line arrives on the mat rather than stopping dead. */
private fun ease(t: Float): Float {
    val inverted = 1f - t
    return 1f - inverted * inverted * inverted
}

private fun DrawScope.drawCourtLine(line: CourtLine, from: Float, to: Float) {
    val start = Offset(line.x1 * size.width, line.y1 * size.height)
    val end = Offset(line.x2 * size.width, line.y2 * size.height)

    drawLine(
        color = if (line.isNet) CourtColors.ShuttleCork else CourtColors.CourtLine,
        start = lerp(start, end, from),
        end = lerp(start, end, to),
        strokeWidth = if (line.isNet) Sizes.netLineStroke.toPx() else Sizes.courtStroke.toPx(),
        cap = StrokeCap.Round,
        alpha = if (line.isNet) 1f else COURT_LINE_ALPHA,
    )
}

/** Court markings are painted lines under hall light, not lasers. */
private const val COURT_LINE_ALPHA = 0.85f

private fun lerp(a: Offset, b: Offset, t: Float) =
    Offset(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t)

/** Any point inside the hold window: every line drawn, nothing yet wiped. */
private const val HOLD_PHASE = (DRAW_END + HOLD_END) / 2f

/**
 * The full-screen version: the court in the middle of an otherwise empty mat, with a word for what
 * is being worked out.
 *
 * Swallows taps, because the thing underneath is mid-change and a tap landing on a stale court
 * number would be worse than a tap doing nothing.
 */
@Composable
fun CourtLoadingOverlay(label: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CourtColors.CourtDeep)
            .pointerInput(Unit) { detectTapGestures { } }
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            CourtLoader(Modifier.height(Sizes.loaderCourtHeight))
            Text(
                text = label,
                style = CourtType.Label,
                color = CourtColors.Chalk60,
                modifier = Modifier.padding(top = Space.xl),
            )
        }
    }
}
