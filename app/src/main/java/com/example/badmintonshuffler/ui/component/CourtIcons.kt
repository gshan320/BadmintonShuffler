package com.example.badmintonshuffler.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The five glyphs this app needs, drawn rather than imported.
 *
 * `material-icons-core` is deprecated and no longer arrives with the Compose BOM, and pulling it
 * back in for five shapes is not worth a dependency. Drawing them also lets the strokes be heavier
 * than Material's default, which matters when the phone is being read at arm's length in a hall.
 */
enum class CourtGlyph { PLUS, MINUS, CLOSE, BACK, OVERFLOW, CHECK }

@Composable
fun CourtIcon(
    glyph: CourtGlyph,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    size: Dp = 24.dp,
) {
    Canvas(
        modifier = modifier
            .size(size)
            .then(
                if (contentDescription != null) {
                    Modifier.semantics { this.contentDescription = contentDescription }
                } else {
                    Modifier
                }
            )
    ) {
        val stroke = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
        val c = this.size.width / 2f
        val arm = this.size.width * 0.29f

        when (glyph) {
            CourtGlyph.PLUS -> {
                line(c - arm, c, c + arm, c, tint, stroke)
                line(c, c - arm, c, c + arm, tint, stroke)
            }

            CourtGlyph.MINUS -> line(c - arm, c, c + arm, c, tint, stroke)

            CourtGlyph.CLOSE -> {
                line(c - arm, c - arm, c + arm, c + arm, tint, stroke)
                line(c + arm, c - arm, c - arm, c + arm, tint, stroke)
            }

            CourtGlyph.BACK -> {
                line(c + arm, c, c - arm, c, tint, stroke)
                line(c - arm, c, c - arm + arm * 0.75f, c - arm * 0.75f, tint, stroke)
                line(c - arm, c, c - arm + arm * 0.75f, c + arm * 0.75f, tint, stroke)
            }

            CourtGlyph.OVERFLOW -> {
                val r = 1.9.dp.toPx()
                listOf(c - arm, c, c + arm).forEach { y ->
                    drawCircle(tint, radius = r, center = androidx.compose.ui.geometry.Offset(c, y))
                }
            }

            CourtGlyph.CHECK -> {
                line(c - arm, c, c - arm * 0.2f, c + arm * 0.7f, tint, stroke)
                line(c - arm * 0.2f, c + arm * 0.7f, c + arm, c - arm * 0.7f, tint, stroke)
            }
        }
    }
}

private fun DrawScope.line(
    x1: Float,
    y1: Float,
    x2: Float,
    y2: Float,
    color: Color,
    stroke: Stroke,
) = drawLine(
    color = color,
    start = androidx.compose.ui.geometry.Offset(x1, y1),
    end = androidx.compose.ui.geometry.Offset(x2, y2),
    strokeWidth = stroke.width,
    cap = stroke.cap,
)
