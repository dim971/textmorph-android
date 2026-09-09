package io.github.dim971.textmorph.core

// A port of torph's packages/torph/src/lib/text-morph/utils/diff.ts.
//
// Building the new segmentation from the plans.

/**
 * Assembles the new segmentation, one word at a time.
 *
 * A type rather than a long function with nested closures, because the walk
 * carries four pieces of state that every step touches: the segments so far, the
 * running offset the separator identities are derived from, the identity
 * allocator, and the splits map.
 */
internal class Builder(
    private val oldWords: List<WordGroup>,
    private val numbersOn: Boolean,
    /** Already narrowed to null unless the value holds exactly one number. */
    private val cursorIndex: UTF16Offset?,
    private val decimalCharacter: Char,
    private val minter: MintedIds,
) {
    private val segments = ArrayList<Segment>()
    private val allocator = IdAllocator()
    private val splits = HashMap<String, List<Segment>>()

    /**
     * Indexes the new value, in UTF-16 code units, and is what a separator's
     * identity is derived from.
     */
    private var charOffset = 0

    fun build(
        plans: List<WordPlan>,
        scan: SegmentDiff.WordScan,
    ): DiffResult {
        // Reserved up front: an identity inherited later would otherwise be
        // handed to an earlier segment that only wanted its text.
        reserveInheritedIds(plans)

        for (newIndex in scan.words.indices) {
            // Includes the edges: a fresh segmentation keeps leading and
            // trailing whitespace, so the diff has to as well.
            pushSeparators(scan.separatorsBefore[newIndex])
            pushWord(scan.words[newIndex], plans[newIndex])
        }

        pushSeparators(scan.trailing)

        return DiffResult(segments, splits)
    }

    private fun reserveInheritedIds(plans: List<WordPlan>) {
        for (plan in plans) {
            val oldIndex: Int
            val willSplit: Boolean
            when (plan) {
                is WordPlan.Fresh -> continue
                is WordPlan.Reuse -> {
                    oldIndex = plan.oldIndex
                    willSplit = false
                }
                is WordPlan.Morph -> {
                    oldIndex = plan.oldIndex
                    willSplit = true
                }
                is WordPlan.Number -> {
                    oldIndex = plan.oldIndex
                    willSplit = true
                }
            }

            val oldWord = oldWords[oldIndex]
            if (willSplit && oldWord.segments.size == 1) {
                // About to be cut into per-character spans.
                val identity = oldWord.segments[0].id
                for (position in oldWord.word.graphemes().indices) {
                    allocator.reserve("$identity:$position")
                }
            } else {
                for (segment in oldWord.segments) allocator.reserve(segment.id)
            }
        }
    }

    /**
     * One separator per element, each taking its identity from where it sits in
     * the new value.
     */
    private fun pushSeparators(separators: List<Char>) {
        for (separator in separators) {
            if (separator == '\n') {
                segments.add(Segment(id = allocator.take("newline-$charOffset"), string = "\n"))
            } else {
                segments.add(Segment(id = allocator.take("space-$charOffset"), string = "\u00A0"))
            }
            charOffset += 1
        }
    }

    /** One word, by whichever of the four routes its plan chose. */
    private fun pushWord(
        newWord: String,
        plan: WordPlan,
    ) {
        when (plan) {
            is WordPlan.Reuse -> segments.addAll(oldWords[plan.oldIndex].segments)

            is WordPlan.Number -> {
                val previous = asNumberSegments(splitIfWhole(oldWords[plan.oldIndex]))
                // The caret indexes the whole value; segmentNumber wants it
                // relative to the number it is inside.
                val wordCursor = cursorIndex?.let { UTF16Offset(it.value - charOffset) }
                segments.addAll(
                    NumberSegmenter
                        .segmentNumber(
                            newWord,
                            previous = previous,
                            cursor = wordCursor,
                            decimalCharacter = decimalCharacter,
                            minter = minter,
                        ).map { it.segment },
                )
            }

            is WordPlan.Morph -> segments.addAll(morphWord(newWord, oldWords[plan.oldIndex]))

            is WordPlan.Fresh -> {
                if (numbersOn && NumberRules.isNumericWord(newWord)) {
                    segments.addAll(
                        NumberSegmenter
                            .segmentNumber(newWord, minter = minter)
                            .map { it.segment },
                    )
                } else {
                    segments.add(Segment(id = allocator.take(newWord), string = newWord))
                }
            }
        }

        charOffset += newWord.length
    }

    /**
     * Per-character segments of an old word, cutting it up first if it is still a
     * single span.
     *
     * Cutting a one-character word would mint a new identity for a character that
     * never moved, so it is left alone.
     */
    private fun splitIfWhole(oldWord: WordGroup): List<Segment> {
        val characters = oldWord.word.graphemes()
        if (oldWord.segments.size != 1 || characters.size <= 1) return oldWord.segments

        val identity = oldWord.segments[0].id
        val characterSegments =
            characters.mapIndexed { position, character ->
                Segment(id = "$identity:$position", string = character)
            }
        splits[identity] = characterSegments
        return characterSegments
    }

    /**
     * A word becoming another word: its characters pair by subsequence, and the
     * ones that pair keep their identity.
     */
    private fun morphWord(
        newWord: String,
        oldWord: WordGroup,
    ): List<Segment> {
        val oldCharacterSegments = splitIfWhole(oldWord)

        val oldCharacters = oldWord.word.graphemes()
        val newCharacters = newWord.graphemes()
        val (oldLcs, newLcs) = lcsIndices(oldCharacters, newCharacters)

        // Upstream indexes the per-character subsequence result into the
        // per-segment array, which are not the same length when the old word was
        // already several segments, as "km/h" is. It guards the result rather
        // than the index, so a pairing simply does not happen. Indexing out of
        // range would throw here, so the guard is explicit.
        val newCharacterToOldSegment = HashMap<Int, Segment>()
        for (position in newLcs.indices) {
            val oldPosition = oldLcs[position]
            if (oldPosition >= oldCharacterSegments.size) continue
            newCharacterToOldSegment[newLcs[position]] = oldCharacterSegments[oldPosition]
        }

        val out = ArrayList<Segment>(newCharacters.size)
        for ((position, character) in newCharacters.withIndex()) {
            val inherited = newCharacterToOldSegment[position]
            if (inherited != null) {
                out.add(Segment(id = inherited.id, string = character))
            } else {
                out.add(Segment(id = allocator.take("$newWord~$position"), string = character))
            }
        }
        return out
    }

    /**
     * Fills in the kinds an older, non-numeric segmentation of the same word
     * lacked.
     */
    private fun asNumberSegments(segments: List<Segment>): List<NumberSegment> =
        segments.map { segment ->
            NumberSegment(
                id = segment.id,
                string = segment.string,
                kind = segment.kind ?: NumberRules.classifyKind(segment.string),
            )
        }
}
