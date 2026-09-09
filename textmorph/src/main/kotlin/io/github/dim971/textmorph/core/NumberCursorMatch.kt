package io.github.dim971.textmorph.core

// Caret matching, from torph's text-morph/utils/number.ts.

/**
 * Pairs characters around a caret, for a field being typed into.
 *
 * The caret says where the edit was, so both sides of it map across unchanged
 * and only the edit itself is new. That is a better answer than place matching
 * for a field, where the reader's attention is on the keystroke rather than on
 * the magnitude.
 *
 * The walk is over everything *but* the grouping separators. A comma reflows
 * with the magnitude rather than with the keystroke, so counting it into the
 * edit would shear every match past the caret: carrying `123` to `1,234` is a
 * two-character delta of which the reader typed one, and the two are not
 * adjacent.
 */
internal fun cursorMatch(
    oldCharacters: List<Char>,
    newCharacters: List<Char>,
    cursor: Int,
    decimalCharacter: Char,
): Map<Int, Int> {
    val matches = HashMap<Int, Int>()

    val oldKept = keptIndices(oldCharacters, decimalCharacter)
    val newKept = keptIndices(newCharacters, decimalCharacter)

    var keptCursor = 0
    while (keptCursor < newKept.size && newKept[keptCursor] < cursor) keptCursor += 1

    val lengthDifference = newKept.size - oldKept.size
    val pairs =
        when {
            lengthDifference > 0 ->
                pairAfterInsertion(
                    caret = keptCursor,
                    arrived = lengthDifference,
                    oldCount = oldKept.size,
                    newCount = newKept.size,
                )
            lengthDifference < 0 ->
                pairAfterDeletion(
                    caret = keptCursor,
                    left = -lengthDifference,
                    oldCount = oldKept.size,
                    newCount = newKept.size,
                )
            else -> pairAfterReplacement(oldCharacters, newCharacters, oldKept, newKept)
        }
    for ((newPosition, oldPosition) in pairs) {
        matches[newKept[newPosition]] = oldKept[oldPosition]
    }

    // Paired from the units end, so the thousands comma stays the thousands
    // comma however the magnitude moved.
    val oldSeparators = groupingIndices(oldCharacters, decimalCharacter)
    val newSeparators = groupingIndices(newCharacters, decimalCharacter)
    var k = 1
    while (k <= oldSeparators.size && k <= newSeparators.size) {
        val oldIndex = oldSeparators[oldSeparators.size - k]
        val newIndex = newSeparators[newSeparators.size - k]
        if (oldCharacters[oldIndex] == newCharacters[newIndex]) {
            matches[newIndex] = oldIndex
        }
        k += 1
    }

    return matches
}

/**
 * Characters arrived. Everything before the edit holds its place, and everything
 * after it shifts by however many arrived.
 */
private fun pairAfterInsertion(
    caret: Int,
    arrived: Int,
    oldCount: Int,
    newCount: Int,
): List<Pair<Int, Int>> {
    val pairs = ArrayList<Pair<Int, Int>>()
    val editStart = caret - arrived
    for (index in 0 until maxOf(0, minOf(editStart, oldCount))) pairs.add(index to index)
    for (index in caret until newCount) {
        val oldIndex = index - arrived
        if (oldIndex in 0 until oldCount) pairs.add(index to oldIndex)
    }
    return pairs
}

/**
 * Characters left. Everything before the caret holds its place, and everything
 * after it closes up.
 */
private fun pairAfterDeletion(
    caret: Int,
    left: Int,
    oldCount: Int,
    newCount: Int,
): List<Pair<Int, Int>> {
    val pairs = ArrayList<Pair<Int, Int>>()
    for (index in 0 until minOf(caret, newCount)) pairs.add(index to index)
    for (index in caret until newCount) {
        val oldIndex = index + left
        if (oldIndex in 0 until oldCount) pairs.add(index to oldIndex)
    }
    return pairs
}

/**
 * The same length, so a character was replaced. Only the ones that did not
 * change carry over; the caret plays no part.
 */
private fun pairAfterReplacement(
    oldCharacters: List<Char>,
    newCharacters: List<Char>,
    oldKept: List<Int>,
    newKept: List<Int>,
): List<Pair<Int, Int>> =
    newKept.indices
        .filter { newCharacters[newKept[it]] == oldCharacters[oldKept[it]] }
        .map { it to it }

/**
 * A separator that groups digits, as opposed to the one that divides the
 * fraction off.
 */
internal fun isGrouping(
    character: Char,
    decimalCharacter: Char,
): Boolean = character != decimalCharacter && character in NumberRules.coreSeparators

/** The indices the caret walk counts: everything but the grouping separators. */
internal fun keptIndices(
    characters: List<Char>,
    decimalCharacter: Char,
): List<Int> = characters.indices.filter { !isGrouping(characters[it], decimalCharacter) }

/** The indices of the grouping separators. */
internal fun groupingIndices(
    characters: List<Char>,
    decimalCharacter: Char,
): List<Int> = characters.indices.filter { isGrouping(characters[it], decimalCharacter) }
