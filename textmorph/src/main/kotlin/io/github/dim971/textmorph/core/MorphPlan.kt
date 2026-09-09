package io.github.dim971.textmorph.core

// What a morph does, as a pure function of elapsed time.
//
// This has no upstream counterpart, because upstream hands its work to the web
// animation API: it writes keyframes and lets the browser interpolate them. This
// port drives its own clock, so it has to say what every segment looks like at
// any moment, and that is what a plan is.
//
// It is also the parity contract. `frame(atElapsed:)` is the one function both
// ports sample, so if the two agree about a plan and agree about that function,
// they agree about the morph. Everything above it is layout and everything below
// it is drawing.

/** Why a segment is moving, which decides how it moves. */
public enum class SegmentRole {
    /** It survived the change and is being carried to its new place. */
    PERSIST,

    /** It is new, and arrives from near whatever survived beside it. */
    ENTER,

    /** It is going, and recedes towards whatever is taking its place. */
    EXIT,

    /**
     * It is one of at least six adjacent segments all arriving together, so the
     * run grows from its own centre instead of each segment travelling.
     */
    GROUP_ENTER,

    /** The same run leaving, collapsing towards its own centre. */
    GROUP_EXIT,

    /** A character of a number that survived, whose slot takes the displacement. */
    NUMBER_PERSIST,

    /** A character of a number that arrived, sliding in along the block axis. */
    NUMBER_ENTER,

    /** A character of a number that is going, sliding out along the block axis. */
    NUMBER_EXIT,
    ;

    /** Whether this role slides inside a clipped slot rather than moving as a whole. */
    public val isNumber: Boolean
        get() = this == NUMBER_PERSIST || this == NUMBER_ENTER || this == NUMBER_EXIT

    /** Whether this role scales about a shared centre rather than its own. */
    public val isGroup: Boolean get() = this == GROUP_ENTER || this == GROUP_EXIT

    /**
     * Whether the segment is on its way out, and so should be dropped once its
     * fade closes.
     */
    public val isLeaving: Boolean get() = this == EXIT || this == GROUP_EXIT || this == NUMBER_EXIT
}

/**
 * Which layout a segment's resting box and glyphs come from.
 *
 * A segment that is leaving has no place in the new value, so it is drawn from
 * where it was; everything else is drawn from where it is going.
 */
public enum class LayoutSource { OLD, NEW }

/** Everything about a segment that moves. */
public data class SegmentState(
    /** Displacement from the segment's resting box. */
    val dx: Double = 0.0,
    val dy: Double = 0.0,
    /** About the segment's own centre, or a run's shared centre for a group. */
    val scale: Double = 1.0,
    val opacity: Double = 1.0,
    /**
     * The inner slide of a number's character inside its slot, which is separate
     * from the slot's own displacement so a digit can cross a whole line box
     * without the next morph measuring it as moved.
     */
    val moverDy: Double = 0.0,
) {
    public companion object {
        /** At rest. */
        public val Resting: SegmentState = SegmentState()
    }
}

/**
 * When something happens, as a share of the morph.
 *
 * Opacity is the only thing that runs on a window rather than the whole
 * duration, and it always runs linearly inside it. Upstream animates the
 * transform with the author's curve and the opacity with `linear` over a
 * fraction of the duration, which is exactly why the clock this samples has to
 * stay linear: if the clock itself were eased, recovering linear time inside the
 * window would need the curve's inverse.
 */
public data class TimeWindow(
    /** A share of the duration, from zero. */
    val start: Double,
    /** A share of the duration, to one. */
    val end: Double,
) {
    /** How far through the window a given elapsed time is, linearly. */
    internal fun progress(
        elapsed: Double,
        duration: Double,
    ): Double {
        val startMs = start * duration
        val endMs = end * duration
        if (elapsed <= startMs) return 0.0
        if (elapsed >= endMs) return 1.0
        val span = endMs - startMs
        return if (span <= 0) 1.0 else (elapsed - startMs) / span
    }

    public companion object {
        /** A window covering the whole morph. */
        public val Whole: TimeWindow = TimeWindow(0.0, 1.0)

        /** A window of [fraction] of the duration, starting after [delay]. */
        public fun fraction(
            fraction: Double,
            delay: Double = 0.0,
        ): TimeWindow = TimeWindow(delay, delay + fraction)
    }
}

/** One segment of a morph. */
public data class SegmentAnimation(
    /** The segment's identity, which is what makes it the same segment as last time. */
    val id: String,
    /** The text it draws. */
    val string: String,
    /** Set for a character of a number. */
    val kind: SegmentKind?,
    /** Why it is moving. */
    val role: SegmentRole,
    /** Which layout to draw it from. */
    val source: LayoutSource,
    /** Where it rests in that layout. */
    val box: SegmentBox,
    /** Where it starts. */
    val from: SegmentState,
    /** Where it ends. */
    val to: SegmentState,
    /** When its opacity changes. */
    val fadeWindow: TimeWindow,
    /** What its scale is measured about: its own centre, or a run's. */
    val scaleOrigin: MorphPoint,
)

/**
 * One axis of the container's own size.
 *
 * The two axes are independent, each with its own curve and its own place in the
 * clock, because upstream animates them separately and for a reason: a value
 * updated faster than the morph settles restarts the curve every frame, and a
 * curve that is slow to leave never gets past its opening sliver. So an axis
 * whose target has not moved resumes at the phase it had reached instead of
 * starting over, which is what the offset carries.
 */
public data class ContainerAxis(
    val from: Double,
    val to: Double,
    val curve: EasingCurve,
    /** How far into its own curve this axis already was when the plan began. */
    val clockOffsetMs: Double = 0.0,
) {
    /** The axis's value at a given elapsed time. */
    public fun value(
        elapsed: Double,
        duration: Double,
    ): Double {
        if (duration <= 0) return to
        val t = ((elapsed + clockOffsetMs) / duration).coerceIn(0.0, 1.0)
        return from + (to - from) * curve.value(t)
    }
}

/** The container's own size, over the morph. */
public data class ContainerAnimation(
    val width: ContainerAxis,
    val height: ContainerAxis,
    /**
     * Set when the value emptied out.
     *
     * Upstream holds the old box on a timer rather than animating it, because a
     * container collapsing to nothing takes the line box with it and the segments
     * still leaving would jump.
     */
    val holdsOldSize: Boolean = false,
)

/** What every segment looks like at one moment. */
public class MorphFrame internal constructor(
    /** One state per segment of the plan, in the plan's own order. */
    public val states: List<SegmentState>,
    /** The container's width at this moment. */
    public val width: Double,
    /** The container's height at this moment. */
    public val height: Double,
    /** Whether the morph has run its course. */
    public val isFinished: Boolean,
)

/** A morph, as a function of elapsed time. */
public class MorphPlan(
    /** How long the whole morph takes, in milliseconds. */
    public val durationMs: Double,
    /** The curve every displacement and scale follows. */
    public val curve: EasingCurve,
    /** How many lines the new value has. */
    public val lineCount: Int,
    /** How far a digit slides when it enters or leaves. */
    public val slideDistance: Double,
    /** Every segment that is drawn, in the order it should be drawn. */
    public val segments: List<SegmentAnimation>,
    /** The container's own size. */
    public val container: ContainerAnimation,
) {
    /**
     * How far past its target the curve goes, as a share of the travel.
     *
     * Zero for a monotone bezier; about sixteen percent for the default spring.
     * It inflates the surface a morph is allowed to draw on, because a glyph
     * clipped at the peak of its bounce is a bug that only appears with springs.
     */
    public val overshoot: Double = overshootOf(curve)

    /**
     * The state of every segment, and the container, at one moment.
     *
     * Pure and total: any elapsed time answers, including a negative one and one
     * past the end. Transforms follow the curve, opacity follows raw linear time
     * inside its own window, and the container follows its own curve from its own
     * place in the clock.
     */
    public fun frame(elapsed: Double): MorphFrame {
        val t = if (durationMs > 0) (elapsed / durationMs).coerceIn(0.0, 1.0) else 1.0
        val eased = curve.value(t)

        val states =
            segments.map { animation ->
                val fade = animation.fadeWindow.progress(elapsed, durationMs)
                SegmentState(
                    dx = interpolate(animation.from.dx, animation.to.dx, eased),
                    dy = interpolate(animation.from.dy, animation.to.dy, eased),
                    scale = interpolate(animation.from.scale, animation.to.scale, eased),
                    opacity = interpolate(animation.from.opacity, animation.to.opacity, fade),
                    moverDy = interpolate(animation.from.moverDy, animation.to.moverDy, eased),
                )
            }

        return MorphFrame(
            states = states,
            width =
                if (container.holdsOldSize) {
                    container.width.from
                } else {
                    container.width.value(elapsed, durationMs)
                },
            height =
                if (container.holdsOldSize) {
                    container.height.from
                } else {
                    container.height.value(elapsed, durationMs)
                },
            isFinished = t >= 1,
        )
    }

    private fun interpolate(
        from: Double,
        to: Double,
        progress: Double,
    ): Double = if (from == to) from else from + (to - from) * progress

    public companion object {
        /**
         * A plan that does nothing, for the first render of a value.
         *
         * Upstream's initial render produces no animations and fires no
         * callbacks, which is not an optimisation: animating the first value in
         * would mean every list of morphing text flew in from nowhere on appear.
         */
        public fun still(
            segments: List<SegmentAnimation>,
            width: Double,
            height: Double,
            lineCount: Int,
            slideDistance: Double,
        ): MorphPlan {
            val curve = EasingCurve.Bezier(CubicBezier.Linear)
            return MorphPlan(
                durationMs = 0.0,
                curve = curve,
                lineCount = lineCount,
                slideDistance = slideDistance,
                segments = segments,
                container =
                    ContainerAnimation(
                        width = ContainerAxis(width, width, curve),
                        height = ContainerAxis(height, height, curve),
                    ),
            )
        }

        /**
         * How far past one the curve reaches, sampled once when the plan is built.
         *
         * Sampled rather than derived: a spring's peak has a closed form, but a
         * carried bezier's does not, and one sweep of a few dozen points at plan
         * time is cheaper than being wrong about it every frame.
         */
        private fun overshootOf(curve: EasingCurve): Double {
            var peak = 1.0
            for (step in 0..64) peak = maxOf(peak, curve.value(step / 64.0))
            return peak - 1
        }
    }
}
