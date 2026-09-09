package io.github.dim971.textmorph.showcase.shared

// The physics two of upstream's cards are actually about.
//
// A bubble hangs off its thumb on a spring, so it trails the travel and leans
// into it, and where two bubbles meet they pivot apart about their tail tips
// rather than overlapping. Both are transcribed from torph's own
// site/src/surfaces/demos/slider.tsx, constants included, because on those two
// cards the motion is the demo: a pill that merely slides says nothing that a
// label would not.
//
// The rest of that file, the autoplay glide and the resize observer, is not
// here; Compose has its own answers for both.

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.tanh

/** How hard the bubble is pulled back over its thumb, and how fast it gives up. */
private const val STIFFNESS = 0.16f
private const val DAMPING = 0.67f

/** Degrees of lean at full trail. */
private const val MAX_TILT = 28f

/** Pixels of trail at one radian of tanh. Past it the lean saturates. */
private const val SOFT = 30f

/** How much a fast bubble stretches along its travel, at most. */
private const val MAX_STRETCH = 0.13f

/**
 * A bubble on a spring, pinned to a thumb.
 *
 * How far it trails is how far it leans, which is the whole trick: the lean is
 * not animated, it is read off the distance between where the bubble is and
 * where it should be.
 */
internal class Bob {
    /** Where the thumb is, in pixels along the track. */
    var x: Float = 0f

    /** Where the bubble has got to. */
    var lag: Float = 0f

    private var velocity: Float = 0f

    /** Snaps the bubble onto its thumb, for a reflow rather than a drag. */
    fun carry(to: Float) {
        x = to
        lag = to
        velocity = 0f
    }

    /** One frame of the spring. */
    fun swing() {
        velocity = (velocity + (x - lag) * STIFFNESS) * DAMPING
        lag += velocity
    }

    val isSettled: Boolean
        get() = abs(velocity) < 0.02f && abs(x - lag) < 0.05f

    /** Degrees, positive leaning back the way it came. */
    val tilt: Float get() = MAX_TILT * tanh((lag - x) / SOFT)

    /** How much it is drawn out along its travel. */
    val stretch: Float get() = min(abs(velocity) * 0.006f, MAX_STRETCH)
}

/** Horizontal scale, which shrinks as the bubble stretches and as it is squashed. */
internal fun scaleXOf(
    stretch: Float,
    squash: Float,
): Float = (1 - stretch * 0.7f) * (1 - squash)

// MARK: two bubbles in each other's way

/** Points of closeness at which the pair start to squash. */
private const val SHOVE_PAD = 10f

/** Points of daylight they hold once they meet. */
private const val SHOVE_CLEAR = 2f

/** Degrees, however far it takes, up to lying flat on the tail. */
internal const val SHOVE_LEAN = 90f

internal const val SHOVE_STIFFNESS = 0.2f
internal const val SHOVE_DAMPING = 0.62f

/** How much the pair squash into each other at full shove. */
internal const val SHOVE_SQUASH = 0.16f

/** Points the tail hangs below the body. Its tip is the pivot. */
internal const val TAIL = 9f

/** Points of corner rounding on the body. */
internal const val BUBBLE_RADIUS = 14f

/** A bubble's body, measured from its tail tip. */
internal class BubbleBox(
    width: Float,
    height: Float,
    scaleX: Float,
    scaleY: Float,
) {
    val half: Float = (width / 2) * scaleX
    val top: Float = -(TAIL + height) * scaleY
    val bottom: Float = -TAIL * scaleY
}

private class Point(
    val x: Float,
    val y: Float,
)

/**
 * How pressed together a pair are, 0 to 1.
 *
 * What the lean and the squash both ride on. Zero once there is room for both
 * bodies plus a pad; one when the thumbs are on top of each other.
 */
internal fun shoveTarget(
    loWidth: Float,
    hiWidth: Float,
    gap: Float,
): Float {
    val need = (loWidth + hiWidth) / 2 + SHOVE_PAD
    if (need <= 0) return 0f
    return maxOf(0f, need - gap) / need
}

/**
 * The body's box inset by its corner radius, swung about the tail tip at [x].
 *
 * A rounded rectangle is that box swept by a disc, so two of them meet arc to
 * arc once the boxes are two radii apart. That is what lets the whole test be
 * done on plain rectangles.
 */
private fun cornersOf(
    box: BubbleBox,
    tilt: Float,
    x: Float,
): List<Point> {
    val radians = tilt * Math.PI.toFloat() / 180
    val s = sin(radians)
    val c = cos(radians)
    val half = maxOf(box.half - BUBBLE_RADIUS, 0f)

    fun at(
        px: Float,
        py: Float,
    ) = Point(x + px * c - py * s, px * s + py * c)
    return listOf(
        at(-half, box.top + BUBBLE_RADIUS),
        at(half, box.top + BUBBLE_RADIUS),
        at(half, box.bottom - BUBBLE_RADIUS),
        at(-half, box.bottom - BUBBLE_RADIUS),
    )
}

private fun spanOn(
    poly: List<Point>,
    nx: Float,
    ny: Float,
): Pair<Float, Float> {
    var low = Float.MAX_VALUE
    var high = -Float.MAX_VALUE
    for (p in poly) {
        val d = p.x * nx + p.y * ny
        low = minOf(low, d)
        high = maxOf(high, d)
    }
    return low to high
}

private fun overlaps(
    a: List<Point>,
    b: List<Point>,
): Boolean {
    for (poly in listOf(a, b)) {
        for (i in 0 until 2) {
            val p = poly[i]
            val q = poly[i + 1]
            val length = hypot(q.x - p.x, q.y - p.y).takeIf { it != 0f } ?: 1f
            val nx = (q.y - p.y) / length
            val ny = (p.x - q.x) / length
            val (aLow, aHigh) = spanOn(a, nx, ny)
            val (bLow, bHigh) = spanOn(b, nx, ny)
            if (bLow > aHigh || aLow > bHigh) return false
        }
    }
    return true
}

private fun edgeDistance(
    v: Point,
    p: Point,
    q: Point,
): Float {
    val ex = q.x - p.x
    val ey = q.y - p.y
    val along = ex * ex + ey * ey
    val t =
        if (along != 0f) {
            (((v.x - p.x) * ex + (v.y - p.y) * ey) / along).coerceIn(0f, 1f)
        } else {
            0f
        }
    return hypot(v.x - p.x - t * ex, v.y - p.y - t * ey)
}

/**
 * Daylight between two leaning bodies, negative once they cross.
 *
 * Their closest approach, not their horizontal extents: bodies tilted into a V
 * meet on their near corners, which the extents pass long before the corners
 * are anywhere near each other. Upstream's note, and its reason for doing this
 * the expensive way.
 */
private fun gapBetween(
    a: List<Point>,
    b: List<Point>,
): Float {
    var near = Float.MAX_VALUE
    for ((poly, other) in listOf(a to b, b to a)) {
        for (v in poly) {
            for (i in other.indices) {
                near = minOf(near, edgeDistance(v, other[i], other[(i + 1) % other.size]))
            }
        }
    }
    return (if (overlaps(a, b)) -near else near) - 2 * BUBBLE_RADIUS
}

/**
 * The shallowest lean that still leaves daylight between the pair.
 *
 * Both tails stay pinned to their thumbs, so leaning further is the only way
 * out of an overlap, which makes the gap monotonic in the lean and a bisection
 * the right way to find it.
 */
internal fun leanApart(
    loBox: BubbleBox,
    hiBox: BubbleBox,
    loTilt: Float,
    hiTilt: Float,
    loX: Float,
    hiX: Float,
): Float {
    fun gapAt(lean: Float) =
        gapBetween(
            cornersOf(loBox, loTilt - lean, loX),
            cornersOf(hiBox, hiTilt + lean, hiX),
        )

    if (gapAt(0f) >= SHOVE_CLEAR) return 0f

    var under = 0f
    var over = SHOVE_LEAN
    repeat(12) {
        val mid = (under + over) / 2
        if (gapAt(mid) < SHOVE_CLEAR) under = mid else over = mid
    }
    return over
}
