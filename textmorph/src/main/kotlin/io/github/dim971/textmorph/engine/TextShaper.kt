package io.github.dim971.textmorph.engine

// Shaping a line once, and drawing any part of it.
//
// This is the approach the rendering probe settled on, and the reasoning is
// worth keeping next to the code.
//
// Positions and glyphs come from the same paint. The obvious alternative was to
// take positions out of a `TextLayoutResult` measured by Compose and glyphs out
// of a `TextPaint` built from the same `TextStyle`, so a value at rest would sit
// exactly where a `BasicText` would. Measured, that loses 2.35px on a tracked
// value: the two do not resolve the font, the letter spacing and the padding
// identically, and every segment ends up slightly off. Two sources of truth for
// one line is the mistake.
//
// So both come from `getRunAdvance` and `drawTextRun`, which take a shaping
// *context* wider than the range they act on. The whole line is the context and
// the segment is the range, which is the same guarantee CoreText gives the iOS
// twin: a segment drawn on its own is the segment the whole-line shaping put
// there, kerning included.

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import io.github.dim971.textmorph.core.ShapedLineMetrics

/** One line of a value, shaped. */
internal class ShapedLine(
    /** The line's text, so a cache can tell two shapings apart. */
    val text: String,
    /** What the line measured. */
    val metrics: ShapedLineMetrics,
    private val paint: Paint,
) {
    /**
     * Draws part of the line.
     *
     * [x] is where the *range* begins, which for a segment is its own box, and
     * [baseline] is the baseline in the canvas's coordinates. The context passed
     * to `drawTextRun` is the whole line whatever the range is, so the glyphs are
     * the ones the whole-line shaping produced.
     */
    fun draw(
        canvas: Canvas,
        start: Int,
        end: Int,
        x: Float,
        baseline: Float,
        paint: Paint,
    ) {
        if (start >= end) return
        canvas.drawTextRun(text, start, end, 0, text.length, x, baseline, false, paint)
    }

    /** A paint carrying this line's face and size, for a caller that will tint it. */
    fun paintCopy(): Paint = Paint(paint)
}

/** Shaping with the platform's text engine. */
internal object TextShaper {
    /**
     * The features to turn off.
     *
     * Unlike the system face on Darwin, Android's default face does form fi and
     * fl ligatures. A ligature straddling a segment boundary would then be drawn
     * by both segments or by neither, and the advance of the two halves would not
     * add up to the advance of the whole. Turning them off costs a little
     * typographic polish and buys the property the whole design rests on: a value
     * drawn segment by segment is the value drawn whole.
     */
    const val LIGATURES_OFF: String = "'liga' 0, 'clig' 0, 'dlig' 0, 'hlig' 0"

    /** A paint for a face and size, with the features this port needs. */
    fun paint(
        typeface: Typeface,
        textSize: Float,
        letterSpacingEm: Float = 0f,
    ): Paint {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.typeface = typeface
        paint.textSize = textSize
        paint.letterSpacing = letterSpacingEm
        paint.fontFeatureSettings = LIGATURES_OFF
        return paint
    }

    /** Shapes one line. */
    fun shape(
        text: String,
        paint: Paint,
    ): ShapedLine {
        val length = text.length
        val metrics = paint.fontMetrics

        // Every UTF-16 boundary, measured against the whole line as context, so
        // an offset is where the whole-line shaping puts that boundary rather
        // than the sum of some prefix measured on its own.
        val offsets = ArrayList<Double>(length + 1)
        for (boundary in 0..length) {
            offsets.add(
                paint.getRunAdvance(text, 0, length, 0, length, false, boundary).toDouble(),
            )
        }

        return ShapedLine(
            text = text,
            metrics =
                ShapedLineMetrics(
                    width = offsets.lastOrNull() ?: 0.0,
                    // The paint reports the ascent above the baseline as a negative
                    // number, and the layout wants a height.
                    ascent = -metrics.ascent.toDouble(),
                    descent = metrics.descent.toDouble(),
                    leading = metrics.leading.toDouble(),
                    offsets = offsets,
                ),
            paint = paint,
        )
    }
}
