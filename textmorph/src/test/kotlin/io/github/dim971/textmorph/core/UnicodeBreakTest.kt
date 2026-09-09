package io.github.dim971.textmorph.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The ported UAX #29 rules, replayed against Unicode's own conformance files.
 *
 * These are not fixtures of upstream behaviour; they are the standard's own test
 * suite. If one of these fails, the rules are wrong, independently of anything
 * torph does. The iOS twin replays the same file.
 */
class UnicodeBreakTest {
    private val fixture = Fixtures.json("unicode-break-tests")

    private class Case(
        val clusters: List<List<Int>>,
        val note: String,
    ) {
        val value: String
            get() =
                buildString {
                    for (cluster in clusters) {
                        for (codePoint in cluster) appendCodePoint(codePoint)
                    }
                }

        val expectedOffsets: List<Int>
            get() {
                val offsets = arrayListOf(0)
                var running = 0
                for (cluster in clusters) {
                    running += cluster.sumOf { Character.charCount(it) }
                    offsets.add(running)
                }
                return offsets
            }
    }

    private fun cases(section: String): List<Case> =
        fixture.getAsJsonArray(section).map { element ->
            val obj = element.asJsonObject
            Case(
                clusters =
                    obj.getAsJsonArray("clusters").map { cluster ->
                        cluster.asJsonArray.map { it.asInt }
                    },
                note = obj.get("note").asString,
            )
        }

    @Test
    fun `the tables were generated from the version the rules were written for`() {
        assertEquals(
            fixture.get("unicodeVersion").asString,
            UnicodeBreakTables.UNICODE_VERSION,
        )
    }

    @Test
    fun `every grapheme cluster break case`() {
        val cases = cases("grapheme")
        assertTrue("the conformance file looks truncated", cases.size > 700)
        check(cases) { UnicodeBreaks.graphemeBoundaries(it) }
    }

    @Test
    fun `every word break case`() {
        val cases = cases("word")
        assertTrue("the conformance file looks truncated", cases.size > 1800)
        check(cases) { UnicodeBreaks.wordBoundaries(it) }
    }

    private fun check(
        cases: List<Case>,
        boundaries: (String) -> List<Int>,
    ) {
        val failures =
            cases.mapNotNull { case ->
                val actual = boundaries(case.value)
                if (actual == case.expectedOffsets) {
                    null
                } else {
                    val codePoints =
                        case.clusters.joinToString(" | ") { cluster ->
                            cluster.joinToString(" ") { "%04X".format(it) }
                        }
                    "  $codePoints\n    expected ${case.expectedOffsets}, got $actual\n" +
                        "    ${case.note}"
                }
            }
        assertTrue(report(failures, cases.size), failures.isEmpty())
    }
}
