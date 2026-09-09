package io.github.dim971.textmorph.core

/**
 * A position in a value, counted in UTF-16 code units.
 *
 * Upstream indexes strings the way JavaScript does, in UTF-16 code units, and
 * Kotlin's `String.length` counts the same way, so this is not the friction it
 * is on the Swift side. The type exists anyway, so the two ports have the same
 * signatures and a reader can put them side by side.
 */
@JvmInline
public value class UTF16Offset(
    public val value: Int,
) : Comparable<UTF16Offset> {
    override fun compareTo(other: UTF16Offset): Int = value.compareTo(other.value)

    public operator fun plus(other: Int): UTF16Offset = UTF16Offset(value + other)

    public operator fun minus(other: Int): UTF16Offset = UTF16Offset(value - other)

    override fun toString(): String = value.toString()
}

/** The length upstream sees: a count of UTF-16 code units. */
public val String.utf16Length: UTF16Offset get() = UTF16Offset(length)

/**
 * Why a segment moves the way it does.
 *
 * A segment with a kind belongs to a numeric word, and slides along the block
 * axis by place value rather than fading in place. Digits and the symbols
 * around them slide opposite ways, so each reads as its own event.
 */
public enum class SegmentKind(
    public val rawValue: String,
) {
    /** A decimal digit inside a numeric word. */
    DIGIT("digit"),

    /**
     * Anything else inside a numeric word: a group separator, a decimal
     * separator, a sign, a currency symbol, a bracket.
     */
    SYMBOL("symbol"),
}

/**
 * One indivisible piece of a value.
 *
 * A segment is a word, a grapheme cluster, a single character of a number, a
 * space or a line break. The morph is expressed entirely in terms of segments:
 * the diff decides which ones survive a change of value, and the plan decides
 * where each one travels.
 *
 * [id] is the identity that makes a morph possible, and it is not an index. It
 * is derived from the segment's own text where it can be, so that the same word
 * keeps the same identity across a re-segmentation, and it is unique across the
 * whole value, because two segments sharing an identity would fight over one
 * place on screen.
 */
public data class Segment(
    /** What this segment is, across values. Unique within a value. */
    val id: String,
    /** The text this segment draws. A space is U+00A0, a line break is "\n". */
    val string: String,
    /** Absent for ordinary text; set for every character of a numeric word. */
    val kind: SegmentKind? = null,
) {
    /** Whether this segment is a line break rather than something drawn. */
    public val isNewline: Boolean get() = string == "\n"

    /**
     * Whether this segment is the space upstream normalises to U+00A0.
     *
     * Only this exact segment separates words. A run of two or more spaces stays
     * one segment holding ordinary spaces, and so does not separate, which is
     * upstream behaviour the port keeps deliberately.
     */
    public val isWordSeparator: Boolean get() = string == "\u00A0"

    public companion object {
        /**
         * The identity upstream gives the zero-width stand-in it keeps in the
         * flow while a value empties out, so the line box does not collapse
         * mid-exit.
         */
        public const val EMPTY_ID: String = "empty"
    }
}
