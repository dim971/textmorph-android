package io.github.dim971.textmorph.core

// The timing constants, gathered from torph's
// packages/torph/src/lib/utils/animate.ts and
// packages/torph/src/lib/text-morph/utils/{animate,number-animate,replace-animate}.ts.
//
// Every one of these is upstream's value under upstream's name. They are here
// rather than beside the code that uses them because the iOS twin carries the
// same table, and a number that lives in one place is a number that can be
// compared.

/** How long each part of a morph takes, as a share of the whole. */
public object MorphTiming {
    /**
     * A share of the morph, never a fixed length. A cap here would leave a
     * character opaque and motionless for the rest of a long duration.
     */
    public fun fadeDuration(
        duration: Double,
        fraction: Double,
    ): Double = duration * fraction

    /** An ordinary segment leaving fades over the first quarter. */
    public const val EXIT_FADE: Double = 0.25

    /**
     * An ordinary segment arriving fades over half the duration, starting a
     * quarter of the way in, so it does not appear before the segment it is
     * replacing has gone.
     */
    public const val ENTER_FADE: Double = 0.5

    /** How far into the morph an arriving segment starts to fade in. */
    public const val ENTER_FADE_DELAY: Double = 0.25

    /**
     * A segment that was already part way through a fade when the next morph
     * arrived finishes the fade over a quarter.
     */
    public const val PERSIST_FADE: Double = 0.25

    /**
     * A digit leaving fades over nearly half the morph. Larger than the ordinary
     * exit share on purpose: a digit that has already gone is a hole in the
     * number.
     */
    public const val NUMBER_EXIT_FADE: Double = 0.45

    /** A digit arriving fades over the first quarter. */
    public const val NUMBER_ENTER_FADE: Double = 0.25

    /**
     * The soft edge on a digit's slot, in ems, so a digit sliding in or out does
     * not appear at a hard line.
     */
    public const val SLOT_FADE: Double = 0.15

    /**
     * Where characters moving becomes one thing swapped for another. Past this,
     * nothing that survived is near enough to animate from and the run smears.
     */
    public const val GROUP_MINIMUM: Int = 6

    /**
     * Deeper than a character's own scale, so the run reads as receding rather
     * than as a glyph settling.
     */
    public const val GROUP_SCALE: Double = 0.8

    /** A replaced run fades out over nearly half the morph. */
    public const val GROUP_EXIT_FADE: Double = 0.45

    /** The run arriving in its place fades in over a third. */
    public const val GROUP_ENTER_FADE: Double = 0.35

    /** A single segment arrives from, and leaves towards, this scale. */
    public const val SEGMENT_SCALE: Double = 0.95

    /**
     * Normalised velocity is distance-relative, so a near-zero distance would
     * otherwise launch the box across the screen.
     */
    public const val CARRY_MAXIMUM: Double = 8.0

    /** How far past its target the carry is allowed to take the curve. */
    public const val CARRY_OVERSHOOT: Double = 0.1

    /** Below this many points of travel there is no momentum worth carrying. */
    public const val CARRY_MINIMUM_DELTA: Double = 0.5

    /**
     * A target this close to the one already in flight is measurement noise, not
     * a moved target, so the curve resumes rather than restarting. This is what
     * keeps a fast counter's box from crawling.
     */
    public const val SAME_TARGET: Double = 0.5
}
