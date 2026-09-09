package io.github.dim971.textmorph.core

// Everything a morph can be told, and the value it is told to show.

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import java.util.Locale

/**
 * What a morph is showing.
 *
 * A number is kept as a number rather than formatted by the caller, because the
 * formatting is part of the morph: the locale decides the decimal separator, and
 * the decimal separator is the pivot every digit alignment is measured from.
 */
@Immutable
public sealed interface MorphValue {
    /** A string, shown as it is. */
    @Immutable
    public data class Text(
        public val value: String,
    ) : MorphValue

    /** A number, formatted by the options. */
    @Immutable
    public data class Number(
        public val value: Double,
    ) : MorphValue
}

/**
 * How a morph behaves.
 *
 * Upstream's options, with the callbacks deliberately left out. Upstream keeps
 * them out of the key it compares to decide whether to tear the morph down and
 * start again, and for the same reason they are not part of this: a composable
 * that passes a fresh lambda on every recomposition should not restart a morph
 * in flight.
 */
@Immutable
public data class TextMorphOptions(
    /**
     * How long a morph takes, in milliseconds. Ignored when [ease] is a spring,
     * which settles on its own physics.
     */
    public val duration: Double = 400.0,
    /** What the morph moves like. */
    public val ease: TextMorphEase = TextMorphEase.Default,
    /** Whether a leaving segment shrinks as it goes. */
    public val scale: Boolean = true,
    /**
     * Whether a numeric word morphs by place value. Off falls back to the
     * character-level morph, which is what a version number or a code wants.
     */
    public val numbers: Boolean = true,
    /**
     * Fraction digits for a numeric value, setting both the minimum and the
     * maximum. Ignored for a string.
     */
    public val decimals: Int? = null,
    /**
     * The locale that decides the decimal separator, and how a numeric value is
     * formatted.
     */
    public val locale: Locale = defaultMorphLocale,
    /** Draws the segment boxes, coloured by what each one is doing. */
    public val debug: Boolean = false,
    /**
     * Turns the morphing off. The value still changes, it just arrives already
     * in place.
     */
    public val disabled: Boolean = false,
    /** Whether the system's reduce-motion setting turns the morphing off. */
    public val respectReducedMotion: Boolean = true,
) {
    /** The string a value shows, under these options. */
    public fun formatted(value: MorphValue): String =
        when (value) {
            is MorphValue.Text -> value.value
            is MorphValue.Number -> NumberFormatting.format(value.value, decimals, locale)
        }

    public companion object {
        /** Upstream's defaults. */
        public val Default: TextMorphOptions = TextMorphOptions()
    }
}

/**
 * What to do when a morph ends.
 *
 * Exactly one of [onComplete] and [onCancel] runs per morph, which is upstream's
 * contract and is enforced by a single-shot token rather than by care.
 */
@Stable
public class MorphCallbacks(
    /** Fired when a morph begins, and never on the first value. */
    public val onStart: (() -> Unit)? = null,
    /** Fired when a morph runs its course. */
    public val onComplete: (() -> Unit)? = null,
    /** Fired when a morph is replaced before it finished. */
    public val onCancel: (() -> Unit)? = null,
)
