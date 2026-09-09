package io.github.dim971.textmorph.core

// UAX #29 word boundaries.
//
// Word breaking needs more context than grapheme breaking: rules WB6, WB7b,
// WB11 and WB12 look one significant character past the boundary, and WB7, WB7c
// and WB11 look one before it. "Significant" is the catch, and it is what rule
// WB4 defines: an extend, a format character or a ZWJ following a base
// character is absorbed into that base and is invisible to every rule after
// WB4. Resolving that folding once, for the whole value, is what keeps this
// linear.

/** Decides word boundaries for one decoded value. */
internal class WordBreaker(
    private val scalars: List<ScalarInfo>,
) {
    /** Indices of the scalars rules WB5 and later can see, in order. */
    private val significant = ArrayList<Int>(scalars.size)

    /**
     * For each scalar, its position in [significant], or the position of the
     * base it folded into.
     */
    private val significantIndex = IntArray(scalars.size)

    init {
        // A base is what an extend, format or ZWJ can attach to. A mandatory
        // break resets it: WB4 explicitly does not fold across one, so an
        // extend at the start of a value, or just after a line break, is a
        // character in its own right.
        var hasBase = false
        for ((index, info) in scalars.withIndex()) {
            val property = info.word
            if (isIgnorable(property) && hasBase) {
                significantIndex[index] = significant.size - 1
                continue
            }
            significantIndex[index] = significant.size
            significant.add(index)
            hasBase = !isMandatoryBreak(property)
        }
    }

    /** Whether a word boundary falls immediately before [index]. */
    fun breaks(index: Int): Boolean {
        val previous = scalars[index - 1]
        val next = scalars[index]

        // WB3, WB3a and WB3b: a line break is a boundary on both sides, and CR
        // LF is one break rather than two.
        if (previous.word == WordBreakProperty.CR && next.word == WordBreakProperty.LF) return false
        if (isMandatoryBreak(previous.word)) return true
        if (isMandatoryBreak(next.word)) return true

        // WB3c and WB3d see raw adjacency, because they come before WB4.
        if (previous.word == WordBreakProperty.ZWJ && next.isExtendedPictographic) return false
        if (previous.word == WordBreakProperty.WSEG_SPACE &&
            next.word == WordBreakProperty.WSEG_SPACE
        ) {
            return false
        }

        // WB4: an extend, format or ZWJ attaches to whatever it follows.
        if (isIgnorable(next.word)) return false

        return breaksBetweenSignificant(index)
    }

    /**
     * One branch per named rule of UAX #29, in the standard's own order.
     *
     * Splitting the chain to satisfy a complexity threshold would put a function
     * boundary in the middle of a rule sequence, and the sequence is the thing a
     * reader has to check against the standard.
     */
    private fun breaksBetweenSignificant(index: Int): Boolean {
        val position = significantIndex[index]
        val prev = propertyAt(position - 1)
        val next = propertyAt(position)
        val beforePrev = propertyAt(position - 2)
        val afterNext = propertyAt(position + 1)

        // WB5: a letter run stays together.
        if (isAhLetter(prev) && isAhLetter(next)) return false
        // WB6 and WB7: one mid-word punctuation mark between two letters.
        if (isAhLetter(prev) && isMidLetterOrQ(next) && isAhLetter(afterNext)) return false
        if (isAhLetter(beforePrev) && isMidLetterOrQ(prev) && isAhLetter(next)) return false
        // WB7a, WB7b and WB7c: Hebrew quoting.
        if (prev == WordBreakProperty.HEBREW_LETTER &&
            next == WordBreakProperty.SINGLE_QUOTE
        ) {
            return false
        }
        if (prev == WordBreakProperty.HEBREW_LETTER &&
            next == WordBreakProperty.DOUBLE_QUOTE &&
            afterNext == WordBreakProperty.HEBREW_LETTER
        ) {
            return false
        }
        if (beforePrev == WordBreakProperty.HEBREW_LETTER &&
            prev == WordBreakProperty.DOUBLE_QUOTE &&
            next == WordBreakProperty.HEBREW_LETTER
        ) {
            return false
        }
        // WB8, WB9 and WB10: digits stay with digits and with letters.
        if (prev == WordBreakProperty.NUMERIC && next == WordBreakProperty.NUMERIC) return false
        if (isAhLetter(prev) && next == WordBreakProperty.NUMERIC) return false
        if (prev == WordBreakProperty.NUMERIC && isAhLetter(next)) return false
        // WB11 and WB12: one numeric separator between two digit runs.
        if (beforePrev == WordBreakProperty.NUMERIC &&
            isMidNumOrQ(prev) &&
            next == WordBreakProperty.NUMERIC
        ) {
            return false
        }
        if (prev == WordBreakProperty.NUMERIC &&
            isMidNumOrQ(next) &&
            afterNext == WordBreakProperty.NUMERIC
        ) {
            return false
        }
        // WB13: katakana stays together.
        if (prev == WordBreakProperty.KATAKANA && next == WordBreakProperty.KATAKANA) return false
        // WB13a and WB13b: an extender joins on either side.
        if (isExtenderLeft(prev) && next == WordBreakProperty.EXTEND_NUM_LET) return false
        if (prev == WordBreakProperty.EXTEND_NUM_LET && isExtenderRight(next)) return false
        // WB15 and WB16: regional indicators pair up from the start of the run.
        if (prev == WordBreakProperty.REGIONAL_INDICATOR &&
            next == WordBreakProperty.REGIONAL_INDICATOR
        ) {
            return regionalIndicatorRun(position) % 2 == 0
        }
        // WB999
        return true
    }

    /**
     * The property at a position in the folded sequence, or Other outside it,
     * which is what "no such character" means to every rule here.
     */
    private fun propertyAt(position: Int): WordBreakProperty {
        if (position < 0 || position >= significant.size) return WordBreakProperty.OTHER
        return scalars[significant[position]].word
    }

    private fun regionalIndicatorRun(position: Int): Int {
        var count = 0
        var cursor = position - 1
        while (cursor >= 0 && propertyAt(cursor) == WordBreakProperty.REGIONAL_INDICATOR) {
            count += 1
            cursor -= 1
        }
        return count
    }

    // The rule macros, spelled out as UAX #29 defines them.

    private fun isIgnorable(property: WordBreakProperty): Boolean =
        property == WordBreakProperty.EXTEND ||
            property == WordBreakProperty.FORMAT ||
            property == WordBreakProperty.ZWJ

    private fun isMandatoryBreak(property: WordBreakProperty): Boolean =
        property == WordBreakProperty.NEWLINE ||
            property == WordBreakProperty.CR ||
            property == WordBreakProperty.LF

    /** AHLetter */
    private fun isAhLetter(property: WordBreakProperty): Boolean =
        property == WordBreakProperty.ALETTER || property == WordBreakProperty.HEBREW_LETTER

    /** MidLetter | MidNumLetQ */
    private fun isMidLetterOrQ(property: WordBreakProperty): Boolean =
        property == WordBreakProperty.MID_LETTER ||
            property == WordBreakProperty.MID_NUM_LET ||
            property == WordBreakProperty.SINGLE_QUOTE

    /** MidNum | MidNumLetQ */
    private fun isMidNumOrQ(property: WordBreakProperty): Boolean =
        property == WordBreakProperty.MID_NUM ||
            property == WordBreakProperty.MID_NUM_LET ||
            property == WordBreakProperty.SINGLE_QUOTE

    /** WB13a's left side: AHLetter | Numeric | Katakana | ExtendNumLet */
    private fun isExtenderLeft(property: WordBreakProperty): Boolean =
        isExtenderRight(property) || property == WordBreakProperty.EXTEND_NUM_LET

    /** WB13b's right side: AHLetter | Numeric | Katakana */
    private fun isExtenderRight(property: WordBreakProperty): Boolean =
        isAhLetter(property) ||
            property == WordBreakProperty.NUMERIC ||
            property == WordBreakProperty.KATAKANA
}
