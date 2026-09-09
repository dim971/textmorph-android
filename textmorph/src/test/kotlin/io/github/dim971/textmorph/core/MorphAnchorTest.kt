package io.github.dim971.textmorph.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The FLIP anchors and the replaced runs, against the JavaScript original.
 *
 * Upstream passes DOM elements to these, but only ever asks a set whether it
 * holds one and indexes an array with it, so the generator drives them with plain
 * numbers. That makes them comparable rather than merely re-implemented.
 */
class MorphAnchorTest {
    private val anchors = Fixtures.json("goldens").getAsJsonObject("anchors")

    @Test
    fun `the nearest surviving neighbour, in both search orders`() {
        val failures =
            anchors.getAsJsonArray("nearest").mapNotNull { element ->
                val case = element.asJsonObject
                val ids = case.getAsJsonArray("ids").map { it.asString }
                val persisting = case.getAsJsonArray("persisting").map { it.asString }.toSet()
                val order =
                    if (case.get("order").asString == "backward-first") {
                        MorphAnchors.SearchOrder.BACKWARD_FIRST
                    } else {
                        MorphAnchors.SearchOrder.FORWARD_FIRST
                    }
                val expected = case.get("anchor").takeIf { !it.isJsonNull }?.asString
                val actual =
                    MorphAnchors.nearestAnchor(
                        case.get("index").asInt,
                        ids,
                        persisting,
                        order,
                    )
                if (actual == expected) {
                    null
                } else {
                    "  $ids persisting $persisting at ${case.get("index")} " +
                        "${case.get("order").asString}: expected $expected, got $actual"
                }
            }
        assertTrue(report(failures), failures.isEmpty())
    }

    @Test
    fun `what each leaving segment recedes towards`() {
        val failures =
            anchors.getAsJsonArray("exiting").mapNotNull { element ->
                val case = element.asJsonObject
                val actual =
                    MorphAnchors.exitingAnchors(
                        oldIds = case.getAsJsonArray("oldIds").map { it.asString },
                        exiting = case.getAsJsonArray("exiting").map { it.asInt }.toSet(),
                        newIds = case.getAsJsonArray("newIds").map { it.asString }.toSet(),
                    )
                val expected =
                    case.getAsJsonObject("anchors").entrySet().associate { (key, value) ->
                        key.toInt() to value.asString
                    }
                if (actual == expected) {
                    null
                } else {
                    "  ${case.get("oldIds")} exiting ${case.get("exiting")} " +
                        "to ${case.get("newIds")}: expected $expected, got $actual"
                }
            }
        assertTrue(report(failures), failures.isEmpty())
    }

    @Test
    fun `which runs are long enough to collapse as one shape`() {
        val failures =
            anchors.getAsJsonArray("runs").mapNotNull { element ->
                val case = element.asJsonObject
                val count = case.get("count").asInt
                val members = case.getAsJsonArray("members").map { it.asInt }.toSet()
                val actual = MorphAnchors.replacedRuns((0 until count).toList(), members)
                val expected =
                    case.getAsJsonArray("runs").map { run ->
                        run.asJsonArray.map { it.asInt }
                    }
                if (actual == expected) {
                    null
                } else {
                    "  $count segments, members $members: expected $expected, got $actual"
                }
            }
        assertTrue(report(failures), failures.isEmpty())
    }

    @Test
    fun `the run minimum is six, and five is not enough`() {
        // Named rather than derived from the fixture, because the number is a
        // judgement upstream made about where movement stops reading as
        // movement, and a change to it should be deliberate.
        assertEquals(6, MorphTiming.GROUP_MINIMUM)
        assertEquals(1, MorphAnchors.replacedRuns((0 until 6).toList(), (0 until 6).toSet()).size)
        assertTrue(MorphAnchors.replacedRuns((0 until 5).toList(), (0 until 5).toSet()).isEmpty())
    }

    @Test
    fun `how far a segment moved, and zero when it is not in both layouts`() {
        val previous = SegmentPositions(points("previous"))
        val current = SegmentPositions(points("current"))

        val failures =
            anchors.getAsJsonArray("deltas").mapNotNull { element ->
                val case = element.asJsonObject
                val expected = case.getAsJsonObject("delta")
                val actual = MorphAnchors.delta(previous, current, case.get("id").asString)
                if (actual.dx == expected.get("dx").asDouble &&
                    actual.dy == expected.get("dy").asDouble
                ) {
                    null
                } else {
                    "  ${case.get("id").asString}: expected " +
                        "(${expected.get("dx")}, ${expected.get("dy")}), " +
                        "got (${actual.dx}, ${actual.dy})"
                }
            }
        assertTrue(report(failures), failures.isEmpty())
    }

    private fun points(name: String): Map<String, MorphPoint> =
        anchors.getAsJsonObject(name).entrySet().associate { (key, value) ->
            val point = value.asJsonObject
            key to MorphPoint(point.get("x").asDouble, point.get("y").asDouble)
        }
}
