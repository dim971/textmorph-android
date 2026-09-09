package io.github.dim971.textmorph.core

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The segmenter, against the JavaScript original.
 *
 * The same 100 cases the iOS twin replays, out of the same file. 96 agree
 * exactly; the four that do not are the spaced-CJK values, recorded in the
 * fixture with the reason, and a test asserts they still diverge so the day one
 * of them starts agreeing is a test result rather than a surprise.
 */
class SegmentTextGoldenTest {
    private val cases =
        Fixtures.json("goldens").getAsJsonArray("segmentText").map {
            it.asJsonObject
        }

    @Test
    fun `every value the corpus segments the same way`() {
        val failures =
            cases.filter { it.get("diverges").isJsonNull }.mapNotNull { case ->
                val value = case.get("value").asString
                val numbers = case.get("numbers").asBoolean
                val expected = case.getAsJsonArray("segments").map { goldenSegment(it.asJsonObject) }
                val actual =
                    TextSegmenter
                        .segmentText(value, defaultMorphLocale, numbers, MintedIds())
                        .asGolden()
                if (actual == expected) {
                    null
                } else {
                    "  ${escaped(value)} numbers=$numbers\n" +
                        "    expected ${expected.joinToString(" | ")}\n" +
                        "    got      ${actual.joinToString(" | ")}"
                }
            }
        assertTrue(report(failures, cases.size), failures.isEmpty())
    }

    @Test
    fun `the corpus reaches both segmentation paths and every cluster shape`() {
        val values = cases.map { it.get("value").asString }.toSet()
        assertTrue("the corpus looks truncated", values.size >= 50)
        assertTrue(values.any { it.contains(" ") && !it.contains("\n") })
        assertTrue(values.any { !it.contains(" ") && !it.contains("\n") && it.isNotEmpty() })
        assertTrue(values.any { it.contains("\n") })
        assertTrue(values.any { value -> value.any { Character.isHighSurrogate(it) } })
    }

    @Test
    fun `the cases marked as diverging are the only ones that do`() {
        // Recorded rather than skipped: if one of these ever starts agreeing, or
        // a new one appears, that is worth knowing rather than passing silently.
        val unexpected =
            cases.filter { !it.get("diverges").isJsonNull }.mapNotNull { case ->
                val value = case.get("value").asString
                val expected = case.getAsJsonArray("segments").map { goldenSegment(it.asJsonObject) }
                val actual =
                    TextSegmenter
                        .segmentText(value, defaultMorphLocale, case.get("numbers").asBoolean, MintedIds())
                        .asGolden()
                if (actual == expected) {
                    "  ${escaped(value)} now agrees: ${case.get("diverges").asString}"
                } else {
                    null
                }
            }
        assertTrue(report(unexpected), unexpected.isEmpty())
    }
}
