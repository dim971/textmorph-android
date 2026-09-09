package io.github.dim971.textmorph.engine

// Shaping each line once and keeping it.
//
// A morph reshapes nothing per frame. The value changes at most a few times a
// second and the frames come at up to a hundred and twenty, so shaping belongs
// behind a cache keyed on everything that could change the answer.

import android.graphics.Paint
import android.graphics.Typeface
import io.github.dim971.textmorph.core.LineLayout
import io.github.dim971.textmorph.core.MorphAlignment
import io.github.dim971.textmorph.core.MorphLayout
import io.github.dim971.textmorph.core.Segment
import io.github.dim971.textmorph.core.ShapedLineMetrics

/**
 * Measuring a line, which is all the layout needs.
 *
 * An interface so the plan can be tested against a made-up monospace metric
 * where a displacement is a whole number, rather than against whatever face the
 * machine running the tests happens to have.
 */
internal interface LineMeasuring {
    fun metrics(text: String): ShapedLineMetrics
}

/**
 * Lays a value's segments out, measuring each line once.
 *
 * Passing a container width, rather than letting the natural width stand, is how
 * the first frame of a morph is laid out: it pins the box to the width it had a
 * moment ago, which is the only thing that keeps a centred or trailing value
 * still while it changes.
 */
internal fun LineMeasuring.layout(
    segments: List<Segment>,
    alignment: MorphAlignment,
    containerWidth: Double? = null,
): MorphLayout {
    val lines = LineLayout.lines(segments)
    return LineLayout.layout(
        lines = lines,
        metrics = lines.map { metrics(it.text) },
        alignment = alignment,
        containerWidth = containerWidth,
    )
}

/**
 * Shaped lines, kept until the face or the size changes.
 *
 * Not thread safe, and deliberately so: it belongs to one composable's engine,
 * and every read of it happens on the composition or in the draw pass, which are
 * the same thread.
 */
internal class LayoutStore(
    typeface: Typeface = Typeface.DEFAULT,
    textSize: Float = 0f,
    letterSpacingEm: Float = 0f,
) : LineMeasuring {
    private var paint: Paint = TextShaper.paint(typeface, textSize, letterSpacingEm)
    private val lines = LinkedHashMap<String, ShapedLine>()

    /**
     * How many lines to keep. A morph touches at most a handful at a time, and a
     * ticker walking through values would otherwise grow this without bound.
     */
    private val capacity = 256

    /** The paint every line is shaped with, which the canvas tints and draws with. */
    val shapingPaint: Paint get() = paint

    /** Points the store at a different face or size, dropping what it knew. */
    fun use(
        typeface: Typeface,
        textSize: Float,
        letterSpacingEm: Float,
    ) {
        if (paint.typeface == typeface &&
            paint.textSize == textSize &&
            paint.letterSpacing == letterSpacingEm
        ) {
            return
        }
        paint = TextShaper.paint(typeface, textSize, letterSpacingEm)
        lines.clear()
    }

    /** One line, shaped. */
    fun shapedLine(text: String): ShapedLine {
        lines[text]?.let { return it }

        // Emptied rather than evicted one at a time. A morph reshapes a whole
        // value at once, so a least-recently-used order would be thrown away by
        // the next update anyway, and the cost of being wrong is one reshape.
        if (lines.size >= capacity) lines.clear()

        val shaped = TextShaper.shape(text, paint)
        lines[text] = shaped
        return shaped
    }

    override fun metrics(text: String): ShapedLineMetrics = shapedLine(text).metrics
}
