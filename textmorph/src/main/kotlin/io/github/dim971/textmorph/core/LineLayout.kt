package io.github.dim971.textmorph.core

// Where every segment of a value sits, once the text has been shaped.
//
// This has no upstream counterpart. Upstream lets the browser lay the spans out
// and then reads their boxes back; this port shapes each line once and computes
// the boxes, which is exact rather than a reflow it has to trust. What it must
// not lose is that there is no automatic line breaking: upstream's root is
// `white-space: nowrap`, so a line exists only where the value put one, and the
// container is free to overflow its parent.

/**
 * What one line of a value measures, as the platform's text engine reports it.
 *
 * [offsets] holds the x position of every UTF-16 boundary in the line, so it is
 * one longer than the line's UTF-16 length. Those come from shaping the whole
 * line once, never from measuring pieces of it: measuring a segment on its own
 * loses the kerning between it and its neighbours, and the error accumulates to
 * several percent of the line.
 */
public data class ShapedLineMetrics(
    /** The line's advance width. */
    val width: Double,
    /** Above the baseline. */
    val ascent: Double,
    /** Below the baseline. */
    val descent: Double,
    /** The gap the font asks for between this line and the next. */
    val leading: Double,
    /** The x position of each UTF-16 boundary, from 0 to [width]. */
    val offsets: List<Double>,
) {
    /** The height of a line box: everything the font asks for. */
    public val lineHeight: Double get() = ascent + descent + leading
}

/** Where one segment sits in a laid-out value. */
public data class SegmentBox(
    /** The segment's identity, which is how a box is found across two layouts. */
    val id: String,
    /** The left edge, in the container's coordinates. */
    val x: Double,
    /** The top of the line box, in the container's coordinates. */
    val y: Double,
    /** The segment's advance width. */
    val width: Double,
    /** The height of the line box the segment sits in. */
    val height: Double,
    /** Which line, counting from zero. */
    val line: Int,
    /** The segment's UTF-16 range within its own line. */
    val range: IntRange,
    /**
     * The whole line's text.
     *
     * Carried on the box so a plan is drawable on its own, without the layouts it
     * came from. A segment that is leaving belongs to a line that no longer
     * exists in the new value, so there would otherwise be nothing to shape it
     * against.
     */
    val lineText: String,
    /**
     * Where this segment's line begins, in the container's coordinates.
     *
     * Not the same as [x], and the difference matters when drawing: a shaper
     * reports a glyph's position relative to the *line* it shaped, so a segment's
     * glyphs have to be drawn from the line's origin. Drawing them from the
     * segment's own [x] counts the offset twice and throws everything but the
     * first word off the end.
     */
    val lineOrigin: Double,
) {
    /** The centre, which is what a run collapsing as one shape scales about. */
    public val centre: MorphPoint get() = MorphPoint(x + width / 2, y + height / 2)
}

/** A size in the container's own coordinates. */
public data class MorphSize(
    val width: Double,
    val height: Double,
) {
    public companion object {
        /** Nothing. */
        public val Zero: MorphSize = MorphSize(0.0, 0.0)
    }
}

/** How a value is laid out, and how big it turned out to be. */
public class MorphLayout internal constructor(
    /**
     * Every drawable segment, in order. Line breaks are not here: they decide
     * where the boxes go rather than being drawn themselves.
     */
    public val boxes: List<SegmentBox>,
    /** The container's own width, which is the widest line. */
    public val width: Double,
    /** The container's own height: one line box per line. */
    public val height: Double,
    /**
     * Where the first line's baseline sits, so a morph can be aligned with an
     * ordinary piece of text beside it.
     */
    public val firstBaseline: Double,
    /** How many lines the value has. One more than the number of line breaks. */
    public val lineCount: Int,
    /** The height of one line box, which is what a digit slides by. */
    public val lineHeight: Double,
) {
    private val index: Map<String, Int> =
        boxes.withIndex().associate { (position, box) -> box.id to position }

    /** The box for a segment, by identity. */
    public operator fun get(id: String): SegmentBox? = index[id]?.let { boxes[it] }

    /** Every segment's top-left corner, which is what a displacement is measured between. */
    internal val positions: SegmentPositions
        get() = SegmentPositions(boxes.associate { it.id to MorphPoint(it.x, it.y) })

    /**
     * How far a digit slides when it enters or leaves a number.
     *
     * One line's worth. Upstream derives it by dividing the container's height by
     * the line count, which is the same number by construction here.
     */
    public val slideDistance: Double get() = lineHeight
}

/** Which edge the lines of a multi-line value line up on. */
public enum class MorphAlignment { LEADING, CENTRE, TRAILING }

/** Assembling a laid-out value from its segments and its measured lines. */
public object LineLayout {
    /** One line of a value: the segments on it, and the text they make. */
    public class Line internal constructor(
        /** The segments on this line, in order, line breaks excluded. */
        public val segments: List<Segment>,
        /** The text of the line, which is what gets shaped. */
        public val text: String,
        /**
         * The UTF-16 offset of each segment within the line, one per segment plus
         * a final one at the line's length.
         */
        public val offsets: List<Int>,
    )

    /**
     * Splits a value's segments into lines.
     *
     * Only on the line-break segments, and never on anything else: there is no
     * automatic wrapping here, deliberately. A value with no break is one line,
     * however wide it is, and the container overflows rather than reflowing. That
     * is upstream's behaviour, and reflowing instead would change which segments
     * are adjacent and so change the whole morph.
     */
    public fun lines(segments: List<Segment>): List<Line> {
        val lines = ArrayList<Line>()
        var current = ArrayList<Segment>()

        fun flush() {
            val text = StringBuilder()
            val offsets = ArrayList<Int>(current.size + 1)
            for (segment in current) {
                offsets.add(text.length)
                text.append(segment.string)
            }
            offsets.add(text.length)
            lines.add(Line(current, text.toString(), offsets))
            current = ArrayList()
        }

        for (segment in segments) {
            if (segment.isNewline) flush() else current.add(segment)
        }
        flush()

        return lines
    }

    /**
     * Places every segment, given what each line measured.
     *
     * [containerWidth] is what the alignment is measured against. Passing null,
     * so the natural width is used, is what an unconstrained morph does; passing
     * the width the container had a moment ago is how the first frame of a morph
     * is laid out, which is the only thing that makes a centred or trailing value
     * hold still while it changes.
     */
    @JvmOverloads
    public fun layout(
        lines: List<Line>,
        metrics: List<ShapedLineMetrics>,
        alignment: MorphAlignment = MorphAlignment.LEADING,
        containerWidth: Double? = null,
    ): MorphLayout {
        require(lines.size == metrics.size) { "a line and its metrics come in pairs" }

        // One height for every line box, so a digit's slide is the same distance
        // wherever it is and a line's position is a multiple of it.
        val lineHeight = metrics.maxOfOrNull { it.lineHeight } ?: 0.0
        val ascent = metrics.firstOrNull()?.ascent ?: 0.0
        val naturalWidth = metrics.maxOfOrNull { it.width } ?: 0.0
        val width = containerWidth ?: naturalWidth

        val boxes = ArrayList<SegmentBox>()
        for ((lineIndex, line) in lines.withIndex()) {
            val lineMetrics = metrics[lineIndex]
            val origin = originX(lineMetrics.width, width, alignment)
            val top = lineIndex * lineHeight

            for ((position, segment) in line.segments.withIndex()) {
                val start = line.offsets[position]
                val end = line.offsets[position + 1]
                val left = offset(lineMetrics, start)
                val right = offset(lineMetrics, end)
                boxes.add(
                    SegmentBox(
                        id = segment.id,
                        x = origin + left,
                        y = top,
                        width = right - left,
                        height = lineHeight,
                        line = lineIndex,
                        range = start until end,
                        lineText = line.text,
                        lineOrigin = origin,
                    ),
                )
            }
        }

        return MorphLayout(
            boxes = boxes,
            width = maxOf(width, naturalWidth),
            height = lines.size * lineHeight,
            firstBaseline = ascent,
            lineCount = lines.size,
            lineHeight = lineHeight,
        )
    }

    private fun originX(
        lineWidth: Double,
        containerWidth: Double,
        alignment: MorphAlignment,
    ): Double =
        when (alignment) {
            MorphAlignment.LEADING -> 0.0
            MorphAlignment.CENTRE -> (containerWidth - lineWidth) / 2
            MorphAlignment.TRAILING -> containerWidth - lineWidth
        }

    /**
     * A boundary's x position, clamped to what the metrics actually carry.
     *
     * The offsets should always reach the line's length, and a mismatch would be a
     * bug in the shaper rather than in the value; clamping means such a bug shows
     * up as a segment of the wrong width rather than as a crash in a draw pass.
     */
    private fun offset(
        metrics: ShapedLineMetrics,
        boundary: Int,
    ): Double {
        if (metrics.offsets.isEmpty()) return 0.0
        return metrics.offsets[boundary.coerceIn(0, metrics.offsets.size - 1)]
    }
}
