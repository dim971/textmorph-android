package io.github.dim971.textmorph.core

// A port of the alignment half of torph's
// packages/torph/src/lib/text-morph/utils/number.ts.

/** A segment of a numeric word, which always carries a kind. */
internal data class NumberSegment(
    val id: String,
    val string: String,
    val kind: SegmentKind,
) {
    /** The segment as the rest of the engine sees it. */
    val segment: Segment get() = Segment(id = id, string = string, kind = kind)
}

/**
 * Cutting a numeric word into per-character segments, and deciding which
 * character of the old value each character of the new one continues.
 */
internal object NumberSegmenter {
    /**
     * Past this many digits of difference the columns overlap into a smear and
     * nothing should carry across. Three is where upstream's corpus divides: the
     * cases that need their slide sit at nought or one, the ones that read better
     * as a replacement at three or more.
     */
    const val MAGNITUDE_JUMP: Int = 3

    /**
     * Per-character segments for a numeric word.
     *
     * With no previous segmentation to carry from, every character gets a minted
     * identity. With one, characters are paired either by caret, when [cursor] is
     * given and the value holds a single number, or by place value, which is the
     * default and the interesting case: a digit's identity is its column, not its
     * position in the string.
     */
    fun segmentNumber(
        value: String,
        previous: List<NumberSegment>? = null,
        cursor: UTF16Offset? = null,
        decimalCharacter: Char = '.',
        minter: MintedIds,
    ): List<NumberSegment> {
        val characters = value.graphemeChars()

        if (previous.isNullOrEmpty()) {
            return characters.map { character ->
                NumberSegment(
                    id = minter.take(emptySet()),
                    string = display(character),
                    kind = NumberRules.classifyKind(character),
                )
            }
        }

        // Upstream reads a normalised space back as an ordinary one, so the two
        // compare equal. It does not do the reverse, so a value whose group
        // separator really is U+00A0 never matches a stored one. That asymmetry
        // is upstream's, and the fixtures pin it.
        val oldCharacters = previous.map { if (it.string == "\u00A0") ' ' else it.string[0] }

        val matches =
            if (cursor != null) {
                cursorMatch(oldCharacters, characters, cursor.value, decimalCharacter)
            } else {
                placeMatch(oldCharacters, characters, decimalCharacter)
            }

        val used = matches.values.map { previous[it].id }.toMutableSet()
        val result = ArrayList<NumberSegment>(characters.size)

        for ((index, character) in characters.withIndex()) {
            val kind = NumberRules.classifyKind(character)
            val oldIndex = matches[index]
            if (oldIndex != null) {
                result.add(
                    NumberSegment(
                        id = previous[oldIndex].id,
                        string = display(character),
                        kind = kind,
                    ),
                )
            } else {
                val id = minter.take(used)
                used.add(id)
                result.add(NumberSegment(id = id, string = display(character), kind = kind))
            }
        }

        return result
    }

    /**
     * A space inside a number is stored normalised, so it keeps its width and
     * never becomes a word separator by accident.
     */
    private fun display(character: Char): String = if (character == ' ') "\u00A0" else character.toString()
}

/**
 * A quantity's characters, one code unit each.
 *
 * Upstream splits on UTF-16 code units, and so does this. That is not a
 * shortcut: a quantity is digits, separators and affixes, all of which are
 * single code units, so this is the same list a grapheme walk would give and
 * the same list the Swift side builds. Where the unit does matter, in a word
 * morphing into another word, both ports walk grapheme clusters instead.
 */
internal fun String.graphemeChars(): List<Char> = toList()
