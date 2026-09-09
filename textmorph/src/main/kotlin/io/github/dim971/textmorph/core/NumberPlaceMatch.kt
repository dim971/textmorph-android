package io.github.dim971.textmorph.core

// Place-value matching, from torph's text-morph/utils/number.ts.

/** What is left of two numbers once the affixes at either end have paired off. */
private class TrimmedRange(
    /** The affix pairings, which the column walks then add to. */
    val matches: MutableMap<Int, Int>,
    /** The first index of the magnitude, in both values. */
    val start: Int,
    /** One past the magnitude's last index in the old value. */
    val oldEnd: Int,
    /** One past the magnitude's last index in the new value. */
    val newEnd: Int,
)

/**
 * Pairs characters by distance from the decimal separator, not left to right,
 * because a digit's identity is its column.
 *
 * Both walks skip a mismatch rather than stopping at it, so a changed character
 * in the middle of a number does not cost the ones past it.
 *
 * Returns a map from an index in the new value to the index in the old value it
 * continues.
 */
internal fun placeMatch(
    oldCharacters: List<Char>,
    newCharacters: List<Char>,
    decimalCharacter: Char,
): Map<Int, Int> {
    val trimmed = trimAffixes(oldCharacters, newCharacters)
    val start = trimmed.start
    val oldEnd = trimmed.oldEnd
    val newEnd = trimmed.newEnd

    val oldPivot = findPivot(oldCharacters, start, oldEnd, decimalCharacter)
    val newPivot = findPivot(newCharacters, start, newEnd, decimalCharacter)

    val oldDigits = integerDigits(oldCharacters, start, oldPivot)
    val newDigits = integerDigits(newCharacters, start, newPivot)

    // A side with no digits is a field being typed into or emptied, not a
    // magnitude, so the jump test does not apply to it.
    if (oldDigits > 0 &&
        newDigits > 0 &&
        kotlin.math.abs(oldDigits - newDigits) >= NumberSegmenter.MAGNITUDE_JUMP
    ) {
        return trimmed.matches
    }

    val context = MatchContext(oldCharacters, newCharacters, trimmed.matches)

    // A separator holds its distance from the pivot, which is what slides the
    // comma one group along on 999,999 to 1,000,000. After a reshape it would
    // have to cross the digits that carried, the two passing in opposite
    // directions, so it leaves instead.
    val reshaped = context.matchDigits(start, oldPivot, start, newPivot, towardsPivot = true)
    if (!reshaped) {
        var k = 1
        while (oldPivot - k >= start && newPivot - k >= start) {
            context.matchSeparator(oldPivot - k, newPivot - k)
            k += 1
        }
    }

    // Absent from either value, the pivot is that value's end, and there is no
    // fraction to align.
    if (oldPivot < oldEnd && newPivot < newEnd) {
        context.matches[newPivot] = oldPivot

        var k = 1
        while (oldPivot + k < oldEnd && newPivot + k < newEnd) {
            context.matchSeparator(oldPivot + k, newPivot + k)
            k += 1
        }
        context.matchDigits(oldPivot + 1, oldEnd, newPivot + 1, newEnd, towardsPivot = false)
    }

    return context.matches
}

/**
 * Pairs off the affixes at either end, which belong to no column, and reports
 * the range of the magnitude that is left.
 *
 * A digit stops either walk: it belongs to the magnitude, and the magnitude is
 * what columns are for.
 */
private fun trimAffixes(
    oldCharacters: List<Char>,
    newCharacters: List<Char>,
): TrimmedRange {
    val matches = HashMap<Int, Int>()

    var start = 0
    while (start < oldCharacters.size &&
        start < newCharacters.size &&
        oldCharacters[start] == newCharacters[start] &&
        !NumberRules.isDigit(oldCharacters[start])
    ) {
        matches[start] = start
        start += 1
    }

    var oldEnd = oldCharacters.size
    var newEnd = newCharacters.size
    while (oldEnd > start &&
        newEnd > start &&
        oldCharacters[oldEnd - 1] == newCharacters[newEnd - 1] &&
        !NumberRules.isDigit(oldCharacters[oldEnd - 1])
    ) {
        matches[newEnd - 1] = oldEnd - 1
        oldEnd -= 1
        newEnd -= 1
    }

    return TrimmedRange(matches, start, oldEnd, newEnd)
}

/**
 * The last decimal separator inside the affix-trimmed range, or the range end
 * when the value has no fraction.
 */
internal fun findPivot(
    characters: List<Char>,
    start: Int,
    end: Int,
    decimalCharacter: Char,
): Int {
    var index = end - 1
    while (index >= start) {
        if (characters[index] == decimalCharacter) return index
        index -= 1
    }
    return end
}

/** How many digits sit on the integer side of the pivot. */
internal fun integerDigits(
    characters: List<Char>,
    start: Int,
    pivot: Int,
): Int {
    if (start >= pivot) return 0
    return (start until pivot).count { NumberRules.isDigit(characters[it]) }
}

/** The indices of the digits in a range. */
internal fun digitIndices(
    characters: List<Char>,
    start: Int,
    end: Int,
): List<Int> {
    if (start >= end) return emptyList()
    return (start until end).filter { NumberRules.isDigit(characters[it]) }
}

/**
 * The two values and the pairing being built, so the column walks can be written
 * as the small mutating steps upstream writes them as closures.
 */
private class MatchContext(
    val oldCharacters: List<Char>,
    val newCharacters: List<Char>,
    val matches: MutableMap<Int, Int>,
) {
    /** Pairs one separator with the one holding the same distance from the pivot. */
    fun matchSeparator(
        oldIndex: Int,
        newIndex: Int,
    ) {
        val character = oldCharacters[oldIndex]
        if (NumberRules.isDigit(character)) return
        if (character == newCharacters[newIndex]) matches[newIndex] = oldIndex
    }

    /**
     * Pairs the digits on one side of the pivot.
     *
     * By column where the count is unchanged, and by subsequence where it
     * changed. Reshaping is integer-side only: a fraction's columns are fixed by
     * the decimal point, so 1.5 becoming 1.25 gains a hundredths place rather
     * than sliding the 5 along. [towardsPivot] reverses the runs before the
     * subsequence walk so its ties resolve from the units column, which is the
     * column a reader is watching.
     *
     * Returns whether any digit survived a reshape.
     */
    fun matchDigits(
        oldFrom: Int,
        oldTo: Int,
        newFrom: Int,
        newTo: Int,
        towardsPivot: Boolean,
    ): Boolean {
        val oldIndices = digitIndices(oldCharacters, oldFrom, oldTo)
        val newIndices = digitIndices(newCharacters, newFrom, newTo)

        if (oldIndices.size == newIndices.size || !towardsPivot) {
            for (k in 0 until minOf(oldIndices.size, newIndices.size)) {
                val oldIndex = oldIndices[k]
                val newIndex = newIndices[k]
                if (oldCharacters[oldIndex] == newCharacters[newIndex]) {
                    matches[newIndex] = oldIndex
                }
            }
            return false
        }

        val oldRun = oldIndices.map { oldCharacters[it] }.reversed()
        val newRun = newIndices.map { newCharacters[it] }.reversed()
        val (a, b) = lcsIndices(oldRun, newRun)

        for (k in a.indices) {
            matches[newIndices[newIndices.size - 1 - b[k]]] = oldIndices[oldIndices.size - 1 - a[k]]
        }

        return a.isNotEmpty()
    }
}
