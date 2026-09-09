package io.github.dim971.textmorph.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SegmentTest {
    @Test
    fun `a value's length is counted in UTF-16 code units, not in characters`() {
        // Free on this platform, where a `String` is already UTF-16, and the
        // reason the iOS twin has a whole type for it. The assertions are kept
        // identical so a change to either side shows up as a difference here.
        assertEquals(UTF16Offset(3), "abc".utf16Length)

        // One grapheme cluster, one scalar, two code units.
        assertEquals(UTF16Offset(2), "\uD83D\uDE00".utf16Length)

        // One grapheme cluster, five scalars, eight code units.
        val family = "\uD83D\uDC68\u200D\uD83D\uDC69\u200D\uD83D\uDC67"
        assertEquals(UTF16Offset(8), family.utf16Length)

        // A combining mark: one cluster, two code units.
        assertEquals(UTF16Offset(2), "e\u0301".utf16Length)
    }

    @Test
    fun `offsets add, subtract and order as numbers`() {
        var offset = UTF16Offset(4)
        offset += 3
        assertEquals(UTF16Offset(7), offset)
        assertEquals(UTF16Offset(5), offset - 2)
        assertTrue(UTF16Offset(1) < UTF16Offset(2))
        assertEquals("7", offset.toString())
        assertEquals(9, UTF16Offset(9).value)
    }

    @Test
    fun `only a normalised space separates words`() {
        assertTrue(Segment(id = "space-0", string = "\u00A0").isWordSeparator)
        // Upstream keeps a run of ordinary spaces as one non-separating segment.
        assertFalse(Segment(id = "gap", string = "  ").isWordSeparator)
        assertFalse(Segment(id = "gap", string = " ").isWordSeparator)
        assertTrue(Segment(id = "newline-3", string = "\n").isNewline)
    }
}
