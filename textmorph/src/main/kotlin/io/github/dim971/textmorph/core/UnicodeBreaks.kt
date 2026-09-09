package io.github.dim971.textmorph.core

// The UAX #29 break rules, over the tables in UnicodeBreakTables.kt.
//
// Why these are ported rather than delegated: this library has to segment a
// value the way Intl.Segmenter does, and the two platforms' own ICU do not
// agree with each other about it. android.icu.text.BreakIterator is a genuine
// ICU4J and breaks a CJK run by dictionary; Foundation's `.byWords` gives a
// different answer again and stops reporting spaces as gaps once a CJK
// character appears. Both also move with the OS version. Owning the rules is
// the only way the two ports can be held to the same fixture.
//
// Dictionary breaking is deliberately not implemented. For Japanese, Chinese,
// Thai, Khmer and Lao a run of letters stays one word here, where ICU would cut
// it into lexical words. Upstream already segments those languages by grapheme
// whenever the value has no space, which is the common case for this library,
// so the difference is confined to spaced CJK and is at least self-consistent.

/** One scalar of a value, with the properties every rule needs. */
internal class ScalarInfo(
    val codePoint: Int,
    /** Where this scalar starts, in UTF-16 code units. */
    val offset: Int,
    val grapheme: GraphemeBreakProperty,
    val word: WordBreakProperty,
    val incb: IndicConjunctBreak,
    val isExtendedPictographic: Boolean,
)

/** Boundaries in a value, as UTF-16 offsets, following UAX #29. */
internal object UnicodeBreaks {
    /** Decodes a value once, so no rule pays for a table lookup twice. */
    fun decode(value: String): List<ScalarInfo> {
        val out = ArrayList<ScalarInfo>(value.length)
        var offset = 0
        while (offset < value.length) {
            val codePoint = value.codePointAt(offset)
            out.add(
                ScalarInfo(
                    codePoint = codePoint,
                    offset = offset,
                    grapheme = graphemeProperty(codePoint),
                    word = wordProperty(codePoint),
                    incb = indicConjunctBreakProperty(codePoint),
                    isExtendedPictographic = isExtendedPictographic(codePoint),
                ),
            )
            offset += Character.charCount(codePoint)
        }
        return out
    }

    /**
     * Extended grapheme cluster boundaries, including 0 and the value's length.
     *
     * Returns a single boundary for an empty value, so callers can always read
     * consecutive elements as a range.
     */
    fun graphemeBoundaries(value: String): List<Int> {
        val scalars = decode(value)
        return boundaries(value, scalars) { index -> graphemeBreaks(scalars, index) }
    }

    /** Word boundaries, including 0 and the value's length. */
    fun wordBoundaries(value: String): List<Int> {
        val scalars = decode(value)
        // The folding rule WB4 is resolved once for the whole value, not per
        // boundary, so this is linear rather than quadratic.
        val breaker = WordBreaker(scalars)
        return boundaries(value, scalars) { index -> breaker.breaks(index) }
    }

    /** GB1, GB2, WB1 and WB2: a value always breaks at both ends. */
    private inline fun boundaries(
        value: String,
        scalars: List<ScalarInfo>,
        breaksBefore: (Int) -> Boolean,
    ): List<Int> {
        if (scalars.isEmpty()) return listOf(0)
        val offsets = ArrayList<Int>(scalars.size + 1)
        offsets.add(0)
        for (index in 1 until scalars.size) {
            if (breaksBefore(index)) offsets.add(scalars[index].offset)
        }
        offsets.add(value.length)
        return offsets
    }

    /**
     * Whether a cluster boundary falls immediately before [index].
     *
     * One branch per named rule of UAX #29, in the standard's own order. The
     * three rules that need more than the two adjacent scalars walk backwards
     * from [index] rather than carrying state, so this stays a pure function of
     * the value. Splitting the chain to satisfy a complexity threshold would put
     * a function boundary inside a rule sequence, and the sequence is the thing
     * a reader has to check against the standard.
     */
    private fun graphemeBreaks(
        scalars: List<ScalarInfo>,
        index: Int,
    ): Boolean {
        val prev = scalars[index - 1]
        val next = scalars[index]

        // GB3
        if (prev.grapheme == GraphemeBreakProperty.CR &&
            next.grapheme == GraphemeBreakProperty.LF
        ) {
            return false
        }
        // GB4 and GB5
        if (isControlLike(prev.grapheme) || isControlLike(next.grapheme)) return true
        // GB6, GB7 and GB8: the Hangul syllable shapes
        if (prev.grapheme == GraphemeBreakProperty.L &&
            next.grapheme in
            setOf(
                GraphemeBreakProperty.L,
                GraphemeBreakProperty.V,
                GraphemeBreakProperty.LV,
                GraphemeBreakProperty.LVT,
            )
        ) {
            return false
        }
        if (prev.grapheme in setOf(GraphemeBreakProperty.LV, GraphemeBreakProperty.V) &&
            next.grapheme in setOf(GraphemeBreakProperty.V, GraphemeBreakProperty.T)
        ) {
            return false
        }
        if (prev.grapheme in setOf(GraphemeBreakProperty.LVT, GraphemeBreakProperty.T) &&
            next.grapheme == GraphemeBreakProperty.T
        ) {
            return false
        }
        // GB9, GB9a and GB9b
        if (next.grapheme == GraphemeBreakProperty.EXTEND ||
            next.grapheme == GraphemeBreakProperty.ZWJ
        ) {
            return false
        }
        if (next.grapheme == GraphemeBreakProperty.SPACING_MARK) return false
        if (prev.grapheme == GraphemeBreakProperty.PREPEND) return false
        // GB9c: an Indic conjunct, which is one cluster across its linker
        if (next.incb == IndicConjunctBreak.CONSONANT &&
            hasIndicLinkerRun(scalars, index)
        ) {
            return false
        }
        // GB11: an emoji ZWJ sequence
        if (next.isExtendedPictographic && hasPictographicZwj(scalars, index)) return false
        // GB12 and GB13: regional indicators pair up from the start of the run
        if (prev.grapheme == GraphemeBreakProperty.REGIONAL_INDICATOR &&
            next.grapheme == GraphemeBreakProperty.REGIONAL_INDICATOR
        ) {
            return regionalIndicatorRun(scalars, index) % 2 == 0
        }
        // GB999
        return true
    }

    private fun isControlLike(property: GraphemeBreakProperty): Boolean =
        property == GraphemeBreakProperty.CONTROL ||
            property == GraphemeBreakProperty.CR ||
            property == GraphemeBreakProperty.LF

    /**
     * GB9c's left side: a consonant, then only Indic extends and linkers, with
     * at least one linker among them.
     */
    private fun hasIndicLinkerRun(
        scalars: List<ScalarInfo>,
        index: Int,
    ): Boolean {
        var sawLinker = false
        var cursor = index - 1
        while (cursor >= 0) {
            when (scalars[cursor].incb) {
                IndicConjunctBreak.LINKER -> sawLinker = true
                IndicConjunctBreak.EXTEND -> Unit
                IndicConjunctBreak.CONSONANT -> return sawLinker
                IndicConjunctBreak.NONE -> return false
            }
            cursor -= 1
        }
        return false
    }

    /** GB11's left side: a pictograph, then any number of extends, then a ZWJ. */
    private fun hasPictographicZwj(
        scalars: List<ScalarInfo>,
        index: Int,
    ): Boolean {
        if (index < 1 || scalars[index - 1].grapheme != GraphemeBreakProperty.ZWJ) return false
        var cursor = index - 2
        while (cursor >= 0 && scalars[cursor].grapheme == GraphemeBreakProperty.EXTEND) {
            cursor -= 1
        }
        return cursor >= 0 && scalars[cursor].isExtendedPictographic
    }

    /**
     * How many regional indicators run consecutively up to, and including,
     * `index - 1`. An even count means the pair before this one closed, so the
     * next indicator starts a fresh flag.
     */
    private fun regionalIndicatorRun(
        scalars: List<ScalarInfo>,
        index: Int,
    ): Int {
        var count = 0
        var cursor = index - 1
        while (cursor >= 0 &&
            scalars[cursor].grapheme == GraphemeBreakProperty.REGIONAL_INDICATOR
        ) {
            count += 1
            cursor -= 1
        }
        return count
    }
}

/**
 * The grapheme clusters of a value, as substrings.
 *
 * Both ports split a "character" this way rather than on UTF-16 code units,
 * which is upstream's unit: Swift cannot hold an unpaired surrogate, so the
 * deviation is forced there and matched here. Using the ported rules rather
 * than the platform's own means the two ports cut the same value the same way
 * even where their ICU tables would disagree.
 */
internal fun String.graphemes(): List<String> {
    if (isEmpty()) return emptyList()
    val boundaries = UnicodeBreaks.graphemeBoundaries(this)
    return (0 until boundaries.size - 1).map { substring(boundaries[it], boundaries[it + 1]) }
}
