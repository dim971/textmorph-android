package io.github.dim971.textmorph.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * What a "character" is, and where the candidate answers differ.
 *
 * The unit a value is cut into decides identities and so decides what moves, and
 * there are three plausible units: a UTF-16 code unit, which is upstream's; a
 * cluster by the platform's own ICU, which moves with the OS; and an extended
 * grapheme cluster by the rules in `core`, which is what both ports use.
 *
 * This suite is the iOS twin's, assertion for assertion, so a port drifting onto
 * a different unit fails here rather than in a golden with no explanation.
 */
class CharacterUnitTest {
    @Test
    fun `a word is cut by the ported rules, not by the platform's`() {
        // A family emoji is one cluster of five scalars and eight code units,
        // and a combining sequence is one cluster of two. Cutting on code units
        // would give eight pieces and one and a half emoji.
        val family = "\uD83D\uDC68\u200D\uD83D\uDC69\u200D\uD83D\uDC67"
        assertEquals(listOf(family), family.graphemes())
        assertEquals(listOf("e\u0301"), "e\u0301".graphemes())
        assertEquals(listOf("c", "a", "f", "\u00E9"), "caf\u00E9".graphemes())
        assertEquals(listOf("1", ",", "2", "0", "4"), "1,204".graphemes())
        assertTrue("".graphemes().isEmpty())

        // A regional indicator pair is one flag, which is GB12 and GB13 and is
        // the case a naive scalar walk gets wrong.
        assertEquals(1, "\uD83C\uDDEB\uD83C\uDDF7".graphemes().size)
    }

    @Test
    fun `a quantity is decided in code units, which is upstream's unit`() {
        assertTrue(NumberRules.isNumericWord("\$1,204"))
        assertTrue(NumberRules.isNumericWord("12%"))
        assertFalse(NumberRules.isNumericWord("COVID-19"))

        // U+1ECB0 INDIC SIYAQ RUPEE MARK is a currency symbol in general
        // category Sc, and it is astral. Taken whole it would be trimmed as an
        // affix and this would be a quantity; as the two surrogates upstream
        // sees, neither is in Sc, so it is not.
        assertFalse(NumberRules.isNumericWord("\uD83B\uDEB05"))
        assertFalse(
            "a lone surrogate is not a currency symbol",
            NumberRules.isCurrency('\uD83B'),
        )

        // A combining mark next to a digit, for the same reason in reverse:
        // taken whole it is not a digit at all.
        assertFalse(NumberRules.isNumericWord("1\u03012"))
        assertTrue("the code unit walk still sees the 1", NumberRules.hasDigit("1\u03012"))
    }
}
