package io.github.dim971.textmorph.core

// A port of torph's packages/torph/src/lib/utils/easing.ts, and of the `carry`
// half of packages/torph/src/lib/utils/animate.ts.
//
// The CSS-facing parts of easing.ts are deliberately not ported: `parseEasing`
// reads a curve back out of a string, `linearEasing` and `fillStops` understand
// the `linear()` syntax, and `sampleEasing` writes a curve back into one. This
// API is typed, so a curve never becomes a string, and this port drives its own
// clock, so a curve is never sampled into a stylesheet. What is left is the
// evaluation, which is the part the engine actually needs.

/**
 * A cubic bezier easing, in CSS's parameterisation.
 *
 * The first and last control points are fixed at (0, 0) and (1, 1), so a curve
 * is the two in between, exactly as `cubic-bezier()` takes them.
 */
public data class CubicBezier(
    val x1: Double,
    val y1: Double,
    val x2: Double,
    val y2: Double,
) {
    /**
     * The curve's value at a normalised time.
     *
     * Twenty-four steps of bisection on the x axis, then the y axis read at the
     * midpoint. Upstream's count, kept: it is well past the precision of a pixel
     * and it costs nothing, and changing it would move every sample the fixtures
     * record. There is no transcendental arithmetic in here, only multiplies and
     * adds, so the result is identical on every platform.
     */
    public fun value(t: Double): Double {
        if (t <= 0) return 0.0
        if (t >= 1) return 1.0

        var low = 0.0
        var high = 1.0
        repeat(24) {
            val mid = (low + high) / 2
            if (axis(x1, x2, mid) < t) low = mid else high = mid
        }
        return axis(y1, y2, (low + high) / 2)
    }

    private fun axis(
        p1: Double,
        p2: Double,
        t: Double,
    ): Double {
        val u = 1 - t
        return 3 * u * u * t * p1 + 3 * u * t * t * p2 + t * t * t
    }

    public companion object {
        /**
         * The curve torph uses unless told otherwise: a long, decelerating ease
         * out, which is what makes a morph read as one gesture rather than as a
         * set of characters each doing its own thing.
         */
        public val Default: CubicBezier = CubicBezier(0.19, 1.0, 0.22, 1.0)

        /** CSS `linear`. */
        public val Linear: CubicBezier = CubicBezier(0.0, 0.0, 1.0, 1.0)

        /** CSS `ease`. */
        public val Ease: CubicBezier = CubicBezier(0.25, 0.1, 0.25, 1.0)

        /** CSS `ease-in`. */
        public val EaseIn: CubicBezier = CubicBezier(0.42, 0.0, 1.0, 1.0)

        /** CSS `ease-out`. */
        public val EaseOut: CubicBezier = CubicBezier(0.0, 0.0, 0.58, 1.0)

        /** CSS `ease-in-out`. */
        public val EaseInOut: CubicBezier = CubicBezier(0.42, 0.0, 0.58, 1.0)
    }
}

/**
 * A curve the engine can sample, whatever it was built from.
 *
 * A value type rather than a lambda, so a plan is comparable and cheap to carry
 * across a frame.
 */
public sealed interface EasingCurve {
    /** The curve's value at a normalised time. */
    public fun value(t: Double): Double

    /** A cubic bezier, as the author wrote it. */
    public data class Bezier(
        val bezier: CubicBezier,
    ) : EasingCurve {
        override fun value(t: Double): Double = bezier.value(t)
    }

    /**
     * A damped spring, evaluated from its own physics rather than sampled.
     *
     * [durationMs] is what [Spring] computed the settling time to be, and is what
     * turns the spring's own seconds into the normalised time every other curve
     * here speaks.
     */
    public data class SpringCurve(
        val omega0: Double,
        val zeta: Double,
        val durationMs: Double,
    ) : EasingCurve {
        override fun value(t: Double): Double = Spring.position(t * durationMs / 1000, omega0, zeta)
    }

    /** The author's curve, leaving at the speed the box is already travelling. */
    public data class Carried(
        val base: EasingCurve,
        val k: Double,
        val bump: Double,
    ) : EasingCurve {
        override fun value(t: Double): Double {
            if (t >= 1) return 1.0
            return base.value(t) + k * t * JsMath.pow(1 - t, bump)
        }
    }
}

/** The slope of a curve, and the momentum an interrupted one carries into the next. */
public object Easing {
    /**
     * The step the forward difference uses. Upstream's, kept: a different step
     * gives a different slope, and the slope decides how much momentum is
     * carried.
     */
    internal const val SLOPE_STEP: Double = 1e-4

    /**
     * Progress per unit of normalised time.
     *
     * Forward rather than centred, so a knot reports the segment ahead of it
     * rather than an average of the two either side.
     */
    public fun slope(
        curve: EasingCurve,
        t: Double,
    ): Double {
        val clamped = minOf(maxOf(t, 0.0), 1 - SLOPE_STEP)
        return (curve.value(clamped + SLOPE_STEP) - curve.value(clamped)) / SLOPE_STEP
    }

    /** A curve that starts at the speed the box is already travelling, and the k it used. */
    public data class Carry(
        val curve: EasingCurve,
        val k: Double,
    )

    /**
     * A curve that starts at the speed the box is already travelling.
     *
     * A morph arriving mid-transition restarts the curve at zero, so a fast run
     * of them only ever plays each curve's opening sliver and the box crawls
     * while the value races ahead. Adding `k * t * (1 - t)^bump` fixes the start
     * slope to the carried velocity without moving either endpoint, and `bump`
     * narrows as `k` grows so the peak stays inside the overshoot allowance.
     *
     * Momentum is only ever added, never subtracted. A curve that starts faster
     * than the box was moving is the author's business, and slowing it to match
     * would make a value that had settled start sluggishly.
     */
    public fun carry(
        base: EasingCurve,
        normalisedVelocity: Double,
    ): Carry {
        // Upstream leans on JavaScript arithmetic here: `Math.max(0, NaN)` is
        // NaN and `NaN > 0` is false, so a velocity that could not be measured
        // falls through to the base curve. Kotlin's `minOf` and `maxOf` return
        // the other operand instead of propagating, so the guard is explicit.
        if (!normalisedVelocity.isFinite()) return Carry(base, 0.0)

        val k =
            maxOf(
                0.0,
                minOf(MorphTiming.CARRY_MAXIMUM, normalisedVelocity) - slope(base, 0.0),
            )
        if (k <= 0) return Carry(base, 0.0)

        val bump =
            maxOf(
                3.0,
                JsMath.ceil(k / (JsMath.E * MorphTiming.CARRY_OVERSHOOT)) - 1,
            )
        return Carry(EasingCurve.Carried(base, k, bump), k)
    }
}
