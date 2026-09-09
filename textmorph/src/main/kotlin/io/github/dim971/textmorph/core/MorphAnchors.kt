package io.github.dim971.textmorph.core

// A port of the arithmetic in torph's packages/torph/src/lib/utils/flip.ts, and
// of `replacedRuns` from text-morph/utils/replace-animate.ts.
//
// `measure` itself is not ported. Upstream reads each span's box back out of the
// DOM, subtracting the transform in flight so the result stays subpixel; this
// port computes both layouts itself, so the boxes are already known and there is
// nothing to read back. What remains of the file is the part that decides which
// segment a moving one takes its bearing from, and that is pure.

/** A point in the container's own coordinates, with y increasing downwards. */
public data class MorphPoint(
    /** From the container's left edge. */
    val x: Double,
    /** From the container's top edge. */
    val y: Double,
)

/**
 * Where each segment sits, by identity, in one layout.
 *
 * A map rather than a list because a segment is found by identity, not by
 * position: the whole point of the diff is that position changes.
 */
internal class SegmentPositions(
    private val positions: Map<String, MorphPoint> = emptyMap(),
) {
    operator fun get(id: String): MorphPoint? = positions[id]

    /** Whether a segment appears in this layout at all. */
    fun contains(id: String): Boolean = id in positions
}

/** How far a segment moved between two layouts. */
internal data class Delta(
    val dx: Double,
    val dy: Double,
)

/**
 * Deciding how far each segment has to travel, and what an arriving or leaving
 * one takes its bearing from.
 */
internal object MorphAnchors {
    /**
     * Which way to look first for a segment to take a bearing from.
     *
     * The direction is not a preference. A segment arriving looks backwards
     * first, so it enters from the text that was already there; a segment leaving
     * looks forwards first, so it recedes towards the text taking its place.
     * Swapping them makes a morph read backwards.
     */
    enum class SearchOrder { BACKWARD_FIRST, FORWARD_FIRST }

    /**
     * How far a segment moved between two layouts.
     *
     * Zero when the segment is absent from either, which is not a fallback so
     * much as the correct answer: there is no displacement to speak of.
     */
    fun delta(
        previous: SegmentPositions,
        current: SegmentPositions,
        id: String,
    ): Delta {
        val p = previous[id] ?: return Delta(0.0, 0.0)
        val c = current[id] ?: return Delta(0.0, 0.0)
        return Delta(p.x - c.x, p.y - c.y)
    }

    /**
     * The nearest segment either side of a position that survives the change.
     *
     * A segment with no previous box of its own has nothing to be displaced from,
     * so it borrows the displacement of a neighbour that does. Null means nothing
     * survived anywhere in the value, which is a wholesale replacement.
     */
    fun nearestAnchor(
        targetIndex: Int,
        ids: List<String>,
        persisting: Set<String>,
        order: SearchOrder = SearchOrder.BACKWARD_FIRST,
    ): String? {
        fun backward(): String? {
            var index = targetIndex - 1
            while (index >= 0) {
                if (ids[index] in persisting) return ids[index]
                index -= 1
            }
            return null
        }

        fun forward(): String? {
            var index = targetIndex + 1
            while (index < ids.size) {
                if (ids[index] in persisting) return ids[index]
                index += 1
            }
            return null
        }

        return when (order) {
            SearchOrder.BACKWARD_FIRST -> backward() ?: forward()
            SearchOrder.FORWARD_FIRST -> forward() ?: backward()
        }
    }

    /**
     * What each leaving segment recedes towards.
     *
     * A leaving segment cannot anchor to another leaving segment, so the
     * surviving set is the old identities that appear in the new value *and* are
     * not themselves on their way out.
     */
    fun exitingAnchors(
        oldIds: List<String>,
        exiting: Set<Int>,
        newIds: Set<String>,
    ): Map<Int, String> {
        val persisting = HashSet<String>()
        for ((index, id) in oldIds.withIndex()) {
            if (id in newIds && index !in exiting) persisting.add(id)
        }

        val anchors = HashMap<Int, String>()
        for (index in oldIds.indices) {
            if (index !in exiting) continue
            val anchor = nearestAnchor(index, oldIds, persisting, SearchOrder.FORWARD_FIRST)
            if (anchor != null) anchors[index] = anchor
        }
        return anchors
    }

    /**
     * Maximal stretches of adjacent segments that are all leaving, or all
     * arriving, and long enough to read as one thing replacing another.
     *
     * A run broken by a survivor is no replacement: that survivor is right there
     * to move relative to. Below the minimum length, the characters are still
     * near enough to each other to animate individually; past it, nothing that
     * survived is close enough and the run smears, so it collapses towards its
     * own centre instead.
     */
    fun replacedRuns(
        all: List<Int>,
        members: Set<Int>,
    ): List<List<Int>> {
        val runs = ArrayList<List<Int>>()
        var run = ArrayList<Int>()

        fun flush() {
            if (run.size >= MorphTiming.GROUP_MINIMUM) runs.add(run)
            run = ArrayList()
        }

        for (index in all) {
            if (index in members) run.add(index) else flush()
        }
        flush()

        return runs
    }
}
