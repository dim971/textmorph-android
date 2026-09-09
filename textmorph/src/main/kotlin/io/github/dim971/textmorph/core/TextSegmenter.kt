package io.github.dim971.textmorph.core

// A port of torph's packages/torph/src/lib/text-morph/utils/segment.ts.

import java.util.Locale

/** One word of a value: its text, and the segments that make it. */
internal class WordGroup(
    val word: String,
    val segments: List<Segment>,
)

/** Cutting a value into the segments a morph is expressed in. */
public object TextSegmenter {
    /**
     * Splits a value into segments.
     *
     * The unit depends on the value. One that holds a space or a line break is
     * cut into words, because words are what a reader tracks across a change of
     * sentence. One that does not is cut into grapheme clusters, because a single
     * word changing is a change of letters. That is upstream's rule and it is why
     * a counter, a price or a label morphs per character.
     *
     * With [numbers] on, a second pass re-cuts every numeric word into
     * per-character segments carrying a kind, so it can morph by place value. It
     * is a pass over the finished segmentation rather than part of it, because
     * the word segmenter splits `$1,234` on its own terms and regrouping on
     * whitespace is what keeps this and the diff agreeing.
     *
     * [locale] is accepted for parity with upstream's signature, and because the
     * rest of the engine needs it, but it has no effect on where the boundaries
     * fall. Upstream hands segmentation to `Intl.Segmenter`, whose answer is
     * locale-dependent for the languages ICU carries a dictionary for; these
     * rules are UAX #29 without dictionary breaking and are the same for every
     * locale. Where the locale does decide something is the decimal separator and
     * the formatting of a numeric value.
     */
    @JvmOverloads
    public fun segmentText(
        value: String,
        locale: Locale = defaultMorphLocale,
        numbers: Boolean = true,
    ): List<Segment> = segmentText(value, locale, numbers, MintedIds())

    internal fun segmentText(
        value: String,
        @Suppress("UNUSED_PARAMETER") locale: Locale,
        numbers: Boolean,
        minter: MintedIds,
    ): List<Segment> {
        val hasNewlines = value.contains("\n")
        val byWord = value.contains(" ") || hasNewlines
        val allocator = IdAllocator()

        if (!hasNewlines) {
            val segments = segmentLine(value, byWord, 0, allocator)
            return if (numbers) expandNumbers(segments, minter) else segments
        }

        // The offset indexes the whole value, not the line, so identities
        // derived from it stay unique across lines.
        val segments = ArrayList<Segment>()
        var offset = 0
        val lines = value.split("\n")

        for ((index, line) in lines.withIndex()) {
            if (index > 0) {
                segments.add(Segment(id = allocator.take("newline-$offset"), string = "\n"))
                offset += 1
            }
            if (line.isNotEmpty()) {
                // A value with a line break is cut into words on every line,
                // even a line that holds no space. So "a\nb" and "ab" segment
                // differently, which is upstream behaviour and is pinned.
                segments.addAll(segmentLine(line, byWord = true, offset = offset, allocator))
            }
            offset += line.length
        }

        return if (numbers) expandNumbers(segments, minter) else segments
    }

    /**
     * Whitespace-delimited words: the unit the diff aligns on, and so a number's
     * bounds.
     *
     * Only a normalised space or a line break separates. A run of two or more
     * spaces is one segment holding ordinary spaces, and therefore does not
     * separate, so `"hello  double"` is a single group here while the diff splits
     * the new value on single spaces. That asymmetry is upstream's.
     */
    internal fun groupIntoWords(segments: List<Segment>): List<WordGroup> {
        val groups = ArrayList<WordGroup>()
        var current = ArrayList<Segment>()

        fun flush() {
            if (current.isEmpty()) return
            groups.add(WordGroup(current.joinToString("") { it.string }, current))
            current = ArrayList()
        }

        for (segment in segments) {
            if (segment.isWordSeparator || segment.isNewline) flush() else current.add(segment)
        }
        flush()

        return groups
    }

    private fun segmentLine(
        line: String,
        byWord: Boolean,
        offset: Int,
        allocator: IdAllocator,
    ): List<Segment> {
        val boundaries =
            if (byWord) {
                UnicodeBreaks.wordBoundaries(line)
            } else {
                UnicodeBreaks.graphemeBoundaries(line)
            }
        if (boundaries.size <= 1) return emptyList()

        val segments = ArrayList<Segment>(boundaries.size - 1)
        for (position in 0 until boundaries.size - 1) {
            val start = boundaries[position]
            val text = line.substring(start, boundaries[position + 1])
            val index = offset + start

            if (text == " ") {
                segments.add(Segment(id = allocator.take("space-$index"), string = "\u00A0"))
            } else {
                segments.add(Segment(id = allocateId(text, index, allocator), string = text))
            }
        }

        return segments
    }

    /**
     * A segment's own text is its identity, and only a collision brings the index
     * into it.
     */
    private fun allocateId(
        text: String,
        index: Int,
        allocator: IdAllocator,
    ): String = if (allocator.has(text)) allocator.take("$text-$index") else allocator.take(text)

    /** Re-cuts every numeric word into per-character segments carrying a kind. */
    private fun expandNumbers(
        segments: List<Segment>,
        minter: MintedIds,
    ): List<Segment> {
        val out = ArrayList<Segment>(segments.size)
        var run = ArrayList<Segment>()

        fun flush() {
            if (run.isEmpty()) return
            val word = run.joinToString("") { it.string }
            if (NumberRules.isNumericWord(word)) {
                out.addAll(
                    NumberSegmenter.segmentNumber(word, minter = minter).map { it.segment },
                )
            } else {
                out.addAll(run)
            }
            run = ArrayList()
        }

        for (segment in segments) {
            if (segment.isWordSeparator || segment.isNewline) {
                flush()
                out.add(segment)
            } else {
                run.add(segment)
            }
        }
        flush()

        return out
    }
}
