package io.github.dim971.textmorph.core

// A port of torph's packages/torph/src/lib/text-morph/utils/diff.ts.
//
// Deciding which old word each new word continues, and what that makes of it.

/**
 * Which old word each new word continues.
 *
 * Two maps, not one, because the distinction decides what happens to the word. A
 * word the subsequence or the reordering pass paired is the *same* word, so it
 * keeps its segments whole. A word the similarity pass paired is a *different*
 * word that resembles it, so it is cut into characters and they pair among
 * themselves.
 */
internal class Pairing(
    /** Paired by the subsequence, or by the reordering pass: the same word. */
    val sameWord: Map<Int, Int>,
    /** Paired by similarity: a different word, near enough to morph from. */
    val similarWord: Map<Int, Int>,
)

/** Three passes, in this order, each only looking at what the one before it left. */
internal fun pairWords(
    oldWordStrings: List<String>,
    newWordStrings: List<String>,
    numbersOn: Boolean,
): Pairing {
    // Numbers share too few characters to pair with each other, so they all wear
    // the same token while the words are being aligned.
    val tokenise = { word: String ->
        if (numbersOn && NumberRules.isNumericWord(word)) SegmentDiff.NUMBER_TOKEN else word
    }
    val oldTokens = oldWordStrings.map(tokenise)
    val newTokens = newWordStrings.map(tokenise)

    // Pass one: the words that stayed in order.
    val (oldLcs, newLcs) = lcsIndices(oldTokens, newTokens)
    val oldMatched = oldLcs.toSet()
    val newMatched = newLcs.toSet()

    val sameWord = HashMap<Int, Int>()
    for (position in newLcs.indices) sameWord[newLcs[position]] = oldLcs[position]

    var oldUnmatched = oldWordStrings.indices.filter { it !in oldMatched }
    var newUnmatched = newWordStrings.indices.filter { it !in newMatched }

    // Pass two: the words that moved.
    val reordered = pairReordered(oldTokens, newTokens, oldUnmatched, newUnmatched)
    if (reordered.isNotEmpty()) {
        // Into the same map as the subsequence pairs, so a word that moved keeps
        // its segments whole rather than being cut into characters that then
        // travel separately.
        sameWord.putAll(reordered)
        val usedOld = reordered.values.toSet()
        oldUnmatched = oldUnmatched.filter { it !in usedOld }
        newUnmatched = newUnmatched.filter { it !in sameWord }
    }

    // Pass three: the words that changed into other words.
    val similarWord =
        pairSimilar(
            oldWordStrings = oldWordStrings,
            newWordStrings = newWordStrings,
            oldUnmatched = oldUnmatched,
            newUnmatched = newUnmatched,
            // From the subsequence alone: the reordering pass is free to move a
            // word, so its pairs anchor nothing.
            oldGaps = gapIndices(oldWordStrings.size, oldMatched),
            newGaps = gapIndices(newWordStrings.size, newMatched),
        )

    return Pairing(sameWord, similarWord)
}

/**
 * Words that moved rather than changed, which a subsequence cannot capture
 * because it only ever runs forwards. Order-preserving: the first free old word
 * wins.
 */
private fun pairReordered(
    oldTokens: List<String>,
    newTokens: List<String>,
    oldUnmatched: List<Int>,
    newUnmatched: List<Int>,
): Map<Int, Int> {
    val pairs = HashMap<Int, Int>()
    val used = HashSet<Int>()
    for (newIndex in newUnmatched) {
        for (oldIndex in oldUnmatched) {
            if (oldIndex in used) continue
            if (newTokens[newIndex] == oldTokens[oldIndex]) {
                pairs[newIndex] = oldIndex
                used.add(oldIndex)
                break
            }
        }
    }
    return pairs
}

/**
 * Words near enough to each other to read as one becoming the other.
 *
 * Skipped entirely past the pairing cap, which leaves the words to arrive and
 * leave rather than blocking the frame on a quadratic comparison.
 */
private fun pairSimilar(
    oldWordStrings: List<String>,
    newWordStrings: List<String>,
    oldUnmatched: List<Int>,
    newUnmatched: List<Int>,
    oldGaps: List<Int>,
    newGaps: List<Int>,
): Map<Int, Int> {
    if (oldUnmatched.size * newUnmatched.size > SegmentDiff.MAXIMUM_MORPH_PAIRINGS) {
        return emptyMap()
    }

    val pairs = HashMap<Int, Int>()
    val used = HashSet<Int>()
    for (newIndex in newUnmatched) {
        var bestOld = -1
        var bestSimilarity = SegmentDiff.MINIMUM_SIMILARITY

        for (oldIndex in oldUnmatched) {
            if (oldIndex in used) continue
            // A pairing that crosses a surviving word would drag its characters
            // the width of the value.
            if (oldGaps[oldIndex] != newGaps[newIndex]) continue
            val similarity = affinity(oldWordStrings[oldIndex], newWordStrings[newIndex])
            if (similarity > bestSimilarity) {
                bestSimilarity = similarity
                bestOld = oldIndex
            }
        }

        if (bestOld >= 0) {
            pairs[newIndex] = bestOld
            used.add(bestOld)
        }
    }
    return pairs
}

/**
 * How many subsequence matches sit before each word: the index of the gap it
 * occupies. Two words in the same gap can pair without crossing a survivor.
 */
internal fun gapIndices(
    count: Int,
    matched: Set<Int>,
): List<Int> {
    val gaps = ArrayList<Int>(count)
    var anchors = 0
    for (index in 0 until count) {
        gaps.add(anchors)
        if (index in matched) anchors += 1
    }
    return gaps
}

/**
 * An old word's claim on a new one. A matching numeric skeleton beats shared
 * characters, so `$1,204` pairs with `$1,318` rather than with whatever it
 * happens to have letters in common with.
 */
internal fun affinity(
    a: String,
    b: String,
): Double {
    if ((NumberRules.hasDigit(a) || NumberRules.hasDigit(b)) &&
        NumberRules.numericSkeleton(a) == NumberRules.numericSkeleton(b)
    ) {
        return 1.0
    }
    return characterSimilarity(a, b)
}

internal fun characterSimilarity(
    a: String,
    b: String,
): Double {
    if (a.isEmpty() || b.isEmpty()) return 0.0
    val (matched, _) = lcsIndices(a.graphemes(), b.graphemes())
    return matched.size.toDouble() / maxOf(a.graphemes().size, b.graphemes().size)
}

/**
 * How a new word gets its segments. The identity reservation pass and the build
 * loop have to agree, so the decision is made once, up front.
 */
internal sealed interface WordPlan {
    /** Nothing to carry from. */
    data object Fresh : WordPlan

    /** The old word's segments, unchanged. */
    data class Reuse(
        val oldIndex: Int,
    ) : WordPlan

    /** The old word's characters, matched by subsequence. */
    data class Morph(
        val oldIndex: Int,
    ) : WordPlan

    /** The old number's characters, matched by place value or by caret. */
    data class Number(
        val oldIndex: Int,
    ) : WordPlan
}

internal fun planWords(
    newWordStrings: List<String>,
    pairing: Pairing,
    numbersOn: Boolean,
): List<WordPlan> =
    // Keyed on what the word is becoming, not on what it was.
    newWordStrings.mapIndexed { newIndex, newWord ->
        val same = pairing.sameWord[newIndex]
        val oldIndex = same ?: pairing.similarWord[newIndex] ?: return@mapIndexed WordPlan.Fresh
        when {
            numbersOn && NumberRules.isNumericWord(newWord) -> WordPlan.Number(oldIndex)
            same != null -> WordPlan.Reuse(oldIndex)
            else -> WordPlan.Morph(oldIndex)
        }
    }
