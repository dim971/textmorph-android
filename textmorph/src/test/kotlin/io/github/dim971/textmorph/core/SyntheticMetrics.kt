package io.github.dim971.textmorph.core

/**
 * A made-up monospace font, so a plan can be asserted in whole numbers.
 *
 * Every UTF-16 unit advances by the same amount, so a displacement is a multiple
 * of it and a failure reads as "expected 30, got 20" rather than as two similar
 * decimals. It also means these tests say nothing about any real font, which is
 * the point: the shaping is tested against pixels elsewhere, and what is being
 * tested here is the arithmetic of the plan.
 *
 * The iOS twin builds the same metric and replays the same assertions, which is
 * the only way a claim about the two agreeing is checkable.
 */
internal object SyntheticMetrics {
    const val ADVANCE: Double = 10.0
    const val ASCENT: Double = 8.0
    const val DESCENT: Double = 2.0
    const val LEADING: Double = 0.0

    /** The metrics of one line under the synthetic font. */
    fun metrics(text: String): ShapedLineMetrics {
        val length = text.length
        return ShapedLineMetrics(
            width = length * ADVANCE,
            ascent = ASCENT,
            descent = DESCENT,
            leading = LEADING,
            offsets = (0..length).map { it * ADVANCE },
        )
    }

    /** Lays a value's segments out under the synthetic font. */
    fun layout(
        segments: List<Segment>,
        alignment: MorphAlignment = MorphAlignment.LEADING,
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
}

/** One morph, set up the way the engine will set it up. */
internal class SyntheticMorph(
    from: String,
    to: String,
    durationMs: Double = 400.0,
    curve: EasingCurve = EasingCurve.Bezier(CubicBezier.Default),
    scale: Boolean = true,
    numbers: Boolean = true,
    alignment: MorphAlignment = MorphAlignment.LEADING,
    carried: Map<String, SegmentState> = emptyMap(),
) {
    val oldSegments: List<Segment>
    val newSegments: List<Segment>
    val plan: MorphPlan

    init {
        val minter = MintedIds()
        val previous = TextSegmenter.segmentText(from, defaultMorphLocale, numbers, minter)
        val result =
            SegmentDiff.diffSegments(
                previous,
                to,
                defaultMorphLocale,
                DiffOptions(numbers = numbers),
                minter,
            )

        // The old segments are cut finer before being measured, exactly as
        // upstream splits the spans before reading their boxes.
        val split = applySplits(previous, result.splits)

        val oldLayout = SyntheticMetrics.layout(split, alignment)
        val newLayout = SyntheticMetrics.layout(result.segments, alignment)
        val firstFrame = SyntheticMetrics.layout(result.segments, alignment, oldLayout.width)

        oldSegments = split
        newSegments = result.segments
        plan =
            MorphPlanner.plan(
                MorphPlanInput(
                    oldSegments = split,
                    newSegments = result.segments,
                    oldLayout = oldLayout,
                    newLayout = newLayout,
                    firstFrameLayout = firstFrame,
                    durationMs = durationMs,
                    curve = curve,
                    scale = scale,
                    carried = carried,
                ),
            )
    }

    /** The animation for one identity, if the plan has one. */
    fun animation(id: String): SegmentAnimation? = plan.segments.firstOrNull { it.id == id }

    /** The animations in one role. */
    fun animations(role: SegmentRole): List<SegmentAnimation> = plan.segments.filter { it.role == role }

    /** The state of one identity at a moment. */
    fun state(
        id: String,
        elapsed: Double,
    ): SegmentState? {
        val index = plan.segments.indexOfFirst { it.id == id }
        if (index < 0) return null
        return plan.frame(elapsed).states[index]
    }

    companion object {
        /** Substitutes the finer spans the diff asked for. */
        fun applySplits(
            segments: List<Segment>,
            splits: Map<String, List<Segment>>,
        ): List<Segment> {
            if (splits.isEmpty()) return segments
            return segments.flatMap { splits[it.id] ?: listOf(it) }
        }
    }
}
