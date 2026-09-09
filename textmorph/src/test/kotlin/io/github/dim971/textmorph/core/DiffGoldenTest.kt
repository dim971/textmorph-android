package io.github.dim971.textmorph.core

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The segment diff, against the JavaScript original.
 *
 * Every ordered pair of a 22-value corpus, with numbers on and off, plus the
 * caret cases: 976 diffs, out of the same file the iOS twin replays. A branch of
 * the diff this port got wrong shows up as a list of the pairs that reach it,
 * which is far more useful than a single failing assertion. It is how the iOS
 * side found that a word which moved must keep its segments whole.
 */
class DiffGoldenTest {
    private val cases =
        Fixtures.json("goldens").getAsJsonArray("diffSegments").map {
            it.asJsonObject
        }

    @Test
    fun `the corpus is the sweep it claims to be`() {
        assertTrue("the corpus looks truncated", cases.size > 900)
        assertTrue(cases.any { !it.get("cursor").isJsonNull })
        assertTrue(cases.any { !it.get("numbers").asBoolean })
        assertTrue(cases.any { it.getAsJsonObject("splits").size() > 0 })
        assertTrue(cases.any { it.get("after").asString.contains("\n") })
    }

    @Test
    fun `every diff in the sweep`() {
        val failures =
            cases.mapNotNull { case ->
                val problems = problems(case)
                if (problems.isEmpty()) {
                    null
                } else {
                    val caret =
                        case.get("cursor").takeIf { !it.isJsonNull }?.let { " caret ${it.asInt}" } ?: ""
                    "  ${escaped(case.get("before").asString)} to " +
                        "${escaped(case.get("after").asString)} " +
                        "numbers=${case.get("numbers").asBoolean}$caret\n" +
                        problems.joinToString("\n")
                }
            }
        assertTrue(report(failures, cases.size), failures.isEmpty())
    }

    private fun problems(case: com.google.gson.JsonObject): List<String> {
        val numbers = case.get("numbers").asBoolean
        val minter = MintedIds()
        val previous =
            TextSegmenter.segmentText(
                case.get("before").asString,
                defaultMorphLocale,
                numbers,
                minter,
            )

        // Canonicalise across both sides at once, so an identity inherited from
        // the old segmentation gets the same name in both.
        val renamer = Renamer()
        for (segment in previous) renamer.name(segment.id)

        val expectedPrevious = case.getAsJsonArray("previous").map { it.asString }
        val actualPrevious = previous.map { renamer.name(it.id) }
        if (actualPrevious != expectedPrevious) {
            return listOf(
                "    the starting segmentation already differs",
                "    expected $expectedPrevious",
                "    got      $actualPrevious",
            )
        }

        val result =
            SegmentDiff.diffSegments(
                previous,
                case.get("after").asString,
                defaultMorphLocale,
                DiffOptions(
                    numbers = numbers,
                    cursorIndex =
                        case
                            .get("cursor")
                            .takeIf { !it.isJsonNull }
                            ?.let { UTF16Offset(it.asInt) },
                ),
                minter,
            )

        val segments =
            result.segments.map {
                GoldenSegment(renamer.name(it.id), it.string, it.kind?.rawValue)
            }
        val expectedSegments =
            case.getAsJsonArray("segments").map {
                goldenSegment(it.asJsonObject)
            }
        val actualAlignment =
            alignment(
                result.segments.map { renamer.name(it.id) },
                previous.map { renamer.name(it.id) },
            )
        val expectedAlignment = case.getAsJsonArray("alignment").map { Fixtures.nullableInt(it) }

        val splits =
            result.splits.entries.associate { (key, value) ->
                renamer.name(key) to value.map { renamer.name(it.id) to it.string }
            }
        val expectedSplits =
            case.getAsJsonObject("splits").entrySet().associate { (key, value) ->
                key to
                    value.asJsonArray.map {
                        val obj = it.asJsonObject
                        obj.get("id").asString to obj.get("string").asString
                    }
            }

        val problems = ArrayList<String>()
        if (segments != expectedSegments) {
            problems.add("    segments expected ${expectedSegments.joinToString(" | ")}")
            problems.add("             got      ${segments.joinToString(" | ")}")
        }
        if (actualAlignment != expectedAlignment) {
            problems.add("    alignment expected ${describe(expectedAlignment)}")
            problems.add("              got      ${describe(actualAlignment)}")
        }
        if (splits != expectedSplits) {
            problems.add("    splits expected $expectedSplits")
            problems.add("           got      $splits")
        }
        return problems
    }
}
