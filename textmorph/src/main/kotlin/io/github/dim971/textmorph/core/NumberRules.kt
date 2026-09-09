package io.github.dim971.textmorph.core

// A port of the classification half of torph's
// packages/torph/src/lib/text-morph/utils/number.ts.

import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Which characters a quantity is allowed to be made of, and how they are read.
 *
 * The sets are upstream's, character for character, with one addition. Widening
 * any of them changes what counts as a number, and therefore what morphs by
 * place value rather than by character.
 */
internal object NumberRules {
    /**
     * Separators that can appear *between* digits without ending the number.
     *
     * Upstream's set, plus U+2019. That addition is a deliberate deviation and
     * it is here because two ICU versions disagree about Swiss German: CLDR
     * groups de-CH with U+0027 in one version and U+2019 in another, and which
     * one a device produces depends on its OS. Without U+2019 in this set the
     * same number would roll by place value on one OS version and morph
     * character by character on the next, which is a worse outcome than
     * widening the set by one character that is a group separator in CLDR
     * either way.
     *
     * U+2019 is also an apostrophe, so it is already a trailing affix. That
     * costs nothing: a token has to begin and end with a digit after trimming,
     * so "don't" written with it is still not a quantity.
     */
    val coreSeparators: Set<Char> =
        setOf(
            '.',
            ',',
            '\'',
            '\u2019',
            '\u00A0',
            '\u202F',
            '\u2009',
            '\u2007',
        )

    /** Characters allowed before the first digit. */
    val prefixCharacters: Set<Char> = setOf('+', '-', '\u2212', '(', '#')

    /** Characters allowed after the last digit. */
    val suffixCharacters: Set<Char> =
        setOf(
            '%',
            '.',
            ',',
            '!',
            '?',
            ':',
            ';',
            ')',
            '"',
            '\'',
            '\u201D',
            '\u2019',
        )

    /**
     * An ASCII decimal digit, which is the only thing upstream counts as one.
     *
     * Upstream writes `char >= "0" && char <= "9"`, a comparison of UTF-16 code
     * units, which is what a Kotlin `Char` comparison already is.
     */
    fun isDigit(character: Char): Boolean = character in '0'..'9'

    /** Whether a value holds a digit anywhere. */
    fun hasDigit(value: String): Boolean = value.any(::isDigit)

    /** A currency symbol, general category Sc, which upstream matches with `\p{Sc}`. */
    fun isCurrency(character: Char): Boolean =
        Character.getType(character) == Character.CURRENCY_SYMBOL.toInt()

    /**
     * What is left of a token once its digits and separators go: `$`, `%`, `()`.
     *
     * Two tokens with the same skeleton are the same shape of quantity, which is
     * what lets the diff pair `$1,204` with `$1,318` in preference to anything
     * they happen to share characters with.
     */
    fun numericSkeleton(word: String): String = word.filter { !isDigit(it) && it !in coreSeparators }

    /**
     * Whether a token is a quantity.
     *
     * Strict on purpose, and on by default: merely holding a digit is not
     * enough, or `COVID-19` and `2024-01-01` would morph by place value. Affixes
     * are trimmed from both ends, and what is left must start and end with a
     * digit and hold nothing but digits and core separators.
     */
    fun isNumericWord(word: String): Boolean {
        var start = 0
        var end = word.length

        while (start < end && isAffix(word[start], prefixCharacters)) start += 1
        while (end > start && isAffix(word[end - 1], suffixCharacters)) end -= 1

        if (start >= end) return false
        if (!isDigit(word[start]) || !isDigit(word[end - 1])) return false

        for (index in start until end) {
            val character = word[index]
            if (!isDigit(character) && character !in coreSeparators) return false
        }

        return true
    }

    /**
     * Whether a character of a numeric word is a digit or one of the symbols
     * that travel with the places they belong to.
     */
    fun classifyKind(character: Char): SegmentKind =
        if (isDigit(character)) SegmentKind.DIGIT else SegmentKind.SYMBOL

    /**
     * The same classification for a segment whose text may be more than one
     * character, which is what an older non-numeric segmentation of the same word
     * can hand over.
     *
     * Upstream calls the single-character test on that string, and JavaScript
     * compares strings lexicographically rather than rejecting the call: `"12"`
     * reads as a digit because `"1"` sorts between `"0"` and `"9"`, while `"km"`
     * does not because `"k"` sorts after `"9"`. Reproduced rather than corrected,
     * because it decides which segments slide.
     */
    fun classifyKind(text: String): SegmentKind =
        if (isDigitLexicographically(text)) SegmentKind.DIGIT else SegmentKind.SYMBOL

    /**
     * JavaScript's `text >= "0" && text <= "9"`, on UTF-16 code units.
     *
     * A code-unit-wise comparison where, if one value is a prefix of the other,
     * the shorter sorts first. Both bounds are a single unit, so `text >= "0"`
     * holds when the value is not empty and its first unit is not below `"0"`,
     * and `text <= "9"` holds when it is empty, or its first unit is below `"9"`,
     * or it is exactly `"9"`. So `"9x"` fails while `"12"` and even `"0x1f"`
     * pass.
     */
    private fun isDigitLexicographically(text: String): Boolean {
        val first = text.firstOrNull() ?: return false
        if (first < '0') return false
        if (first < '9') return true
        return first == '9' && text.length == 1
    }

    private fun isAffix(
        character: Char,
        set: Set<Char>,
    ): Boolean = character in set || isCurrency(character)

    /**
     * The locale's decimal separator, the pivot every place alignment is
     * measured from.
     *
     * Upstream reads it out of `Intl.NumberFormat(locale).formatToParts(1.1)` and
     * memoises it, because constructing an `Intl.NumberFormat` is expensive.
     * `DecimalFormatSymbols` is the same value from the same CLDR data and is
     * read once per morph rather than once per segment, so there is no cache here
     * to keep in step with anything.
     */
    fun decimalSeparator(locale: Locale): Char = DecimalFormatSymbols.getInstance(locale).decimalSeparator
}
