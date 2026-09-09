package io.github.dim971.textmorph.core

// Property lookup over the generated tables.
//
// Each table is a flat, sorted, non-overlapping list of ranges: start, end and
// value for a table with values, start and end for a boolean one. A flat array
// of integers rather than an array of pairs is deliberate: it is compact, and a
// binary search over a stride is as cheap as the lookup gets without a
// two-stage trie, which this library's values are far too short to need.

private val GRAPHEME_VALUES = GraphemeBreakProperty.entries
private val WORD_VALUES = WordBreakProperty.entries
private val INCB_VALUES = IndicConjunctBreak.entries

/** Grapheme_Cluster_Break for a code point. Anything unlisted is Other. */
internal fun graphemeProperty(codePoint: Int): GraphemeBreakProperty =
    GRAPHEME_VALUES[valueIn(UnicodeBreakTables.GRAPHEME, codePoint)]

/** Word_Break for a code point. Anything unlisted is Other. */
internal fun wordProperty(codePoint: Int): WordBreakProperty =
    WORD_VALUES[valueIn(UnicodeBreakTables.WORD, codePoint)]

/** Indic_Conjunct_Break for a code point. Anything unlisted is None. */
internal fun indicConjunctBreakProperty(codePoint: Int): IndicConjunctBreak =
    INCB_VALUES[valueIn(UnicodeBreakTables.INDIC_CONJUNCT_BREAK, codePoint)]

/** Whether a code point is Extended_Pictographic, which GB11 and WB3c need. */
internal fun isExtendedPictographic(codePoint: Int): Boolean =
    rangeIndex(UnicodeBreakTables.EXTENDED_PICTOGRAPHIC, stride = 2, codePoint = codePoint) != null

private fun valueIn(
    table: IntArray,
    codePoint: Int,
): Int {
    val index = rangeIndex(table, stride = 3, codePoint = codePoint) ?: return 0
    return table[index + 2]
}

/** The start index of the range holding [codePoint], or null if unlisted. */
private fun rangeIndex(
    table: IntArray,
    stride: Int,
    codePoint: Int,
): Int? {
    var low = 0
    var high = table.size / stride - 1
    while (low <= high) {
        val mid = (low + high) / 2
        val base = mid * stride
        when {
            codePoint < table[base] -> high = mid - 1
            codePoint > table[base + 1] -> low = mid + 1
            else -> return base
        }
    }
    return null
}
