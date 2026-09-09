package io.github.dim971.textmorph.core

// A port of torph's packages/torph/src/lib/text-morph/utils/diff.ts.

import java.util.Locale

/** What a change of value does to the segments already on screen. */
public data class DiffResult(
    /**
     * The new value's segments. One carrying an identity that appears in the old
     * segmentation survives the change and moves; one carrying a fresh identity
     * arrives; an old identity absent from here leaves.
     */
    val segments: List<Segment>,
    /**
     * Old segments that were cut finer to make the match.
     *
     * A word that survived as a single span has to be split into per-character
     * spans before any of its characters can move independently. The key is the
     * identity of the span that was split, and the value is what it became.
     */
    val splits: Map<String, List<Segment>>,
)

/** How to match a new value against the segments already on screen. */
public data class DiffOptions(
    /** Numeric words morph by place value. Off falls back to a character diff. */
    val numbers: Boolean = true,
    /** The caret, honoured only when the value holds a single number. */
    val cursorIndex: UTF16Offset? = null,
)

/** Matching a new value against the segments already on screen. */
public object SegmentDiff {
    /**
     * Numbers share too few characters to pair with each other, so for the
     * purposes of aligning words they all collapse to one token. A NULL cannot
     * occur in a value, so the token cannot collide with a real word.
     */
    internal const val NUMBER_TOKEN: String = "\u0000#"

    /**
     * Below this share of characters in common, two words are not the same word
     * wearing a change. It is also what keeps a number off a real word, since the
     * two pair freely otherwise.
     */
    internal const val MINIMUM_SIMILARITY: Double = 0.4

    /**
     * The diff runs before the first frame, so past these it degrades to a fresh
     * segmentation rather than blocking.
     */
    internal const val MAXIMUM_MORPH_PAIRINGS: Int = 2500
    internal const val MAXIMUM_LCS_CELLS: Int = 1_000_000

    /** Matches a new value against an existing segmentation. */
    @JvmOverloads
    public fun diffSegments(
        oldSegments: List<Segment>,
        newText: String,
        locale: Locale = defaultMorphLocale,
        options: DiffOptions = DiffOptions(),
    ): DiffResult = diffSegments(oldSegments, newText, locale, options, MintedIds())

    internal fun diffSegments(
        oldSegments: List<Segment>,
        newText: String,
        locale: Locale,
        options: DiffOptions,
        minter: MintedIds,
    ): DiffResult {
        val numbersOn = options.numbers
        val oldWords = TextSegmenter.groupIntoWords(oldSegments)

        // Text identities are derived from the text and survive a
        // re-segmentation; minted numeric ones do not, so a value with digits
        // anywhere cannot take the fast path.
        val digitsInvolved =
            numbersOn &&
                (NumberRules.hasDigit(newText) || oldWords.any { NumberRules.hasDigit(it.word) })
        val newHasSpaces = newText.contains(" ")
        val newHasNewlines = newText.contains("\n")

        if (oldWords.size <= 1 && !newHasSpaces && !newHasNewlines && !digitsInvolved) {
            return DiffResult(
                TextSegmenter.segmentText(newText, locale, numbersOn, minter),
                emptyMap(),
            )
        }

        val scan = scanWords(newText)
        val oldWordStrings = oldWords.map { it.word }

        if (oldWordStrings.size * scan.words.size > MAXIMUM_LCS_CELLS) {
            return DiffResult(
                TextSegmenter.segmentText(newText, locale, numbersOn, minter),
                emptyMap(),
            )
        }

        val pairing = pairWords(oldWordStrings, scan.words, numbersOn)
        val plans = planWords(scan.words, pairing, numbersOn)

        return Builder(
            oldWords = oldWords,
            numbersOn = numbersOn,
            // Meaningless once a value holds several figures: there is no telling
            // which of them the caret is in.
            cursorIndex = if (plans.count { it is WordPlan.Number } == 1) options.cursorIndex else null,
            decimalCharacter = NumberRules.decimalSeparator(locale),
            minter = minter,
        ).build(plans, scan)
    }

    /**
     * The new value's words, and the separators that precede each of them.
     *
     * This replaces upstream's `split(/( |\n)/)`. Splitting on a capturing group
     * does not mean the same thing here, and the sequence of empty parts it
     * yields between two separators is load bearing only in that upstream skips
     * them, so the walk is written out.
     */
    internal class WordScan(
        val words: List<String>,
        /** The separators immediately before each word, in order. */
        val separatorsBefore: List<List<Char>>,
        /** The separators after the last word, which have no word to attach to. */
        val trailing: List<Char>,
    )

    internal fun scanWords(newText: String): WordScan {
        val words = ArrayList<String>()
        val separatorsBefore = ArrayList<List<Char>>()
        var pending = ArrayList<Char>()
        val current = StringBuilder()

        for (character in newText) {
            if (character == ' ' || character == '\n') {
                if (current.isNotEmpty()) {
                    separatorsBefore.add(pending)
                    words.add(current.toString())
                    current.setLength(0)
                    pending = ArrayList()
                }
                pending.add(character)
            } else {
                current.append(character)
            }
        }
        if (current.isNotEmpty()) {
            separatorsBefore.add(pending)
            words.add(current.toString())
            pending = ArrayList()
        }

        return WordScan(words, separatorsBefore, pending)
    }
}
