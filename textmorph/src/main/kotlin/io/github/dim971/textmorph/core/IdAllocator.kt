package io.github.dim971.textmorph.core

// A port of torph's createIdAllocator and its minted numeric identities, from
// packages/torph/src/lib/text-morph/utils/{segment,number}.ts.

/**
 * Hands out segment identities, guaranteeing they are unique.
 *
 * Uniqueness has to hold across the whole value, not per line: a collision makes
 * two segments fight over one place on screen, and one of them silently loses
 * its text.
 *
 * The shape of an identity matters as much as its uniqueness. A segment's
 * identity is its own text where it can be, so the same word keeps the same
 * identity when the value is re-segmented. Only on a collision does an index
 * come into it, and only then a counter.
 */
internal class IdAllocator {
    private val used = HashSet<String>()

    /**
     * Claims an identity without handing one out, for one that will be
     * inherited later. The diff reserves before it builds, because an inherited
     * identity taken later would otherwise go to an earlier segment.
     */
    fun reserve(id: String) {
        used.add(id)
    }

    /** Whether an identity is already claimed. */
    fun has(id: String): Boolean = id in used

    /** The given identity, or the first free variant of it. */
    fun take(base: String): String {
        if (base !in used) {
            used.add(base)
            return base
        }
        var suffix = 1
        while ("$base~$suffix" in used) suffix += 1
        val id = "$base~$suffix"
        used.add(id)
        return id
    }
}

/**
 * Mints identities for the characters of a number that have no predecessor.
 *
 * A digit that arrives in a value has nothing to inherit an identity from, so it
 * needs a fresh one. The identity has to be unique not only against the value
 * being built but against every identity still on screen, including the ones on
 * segments part way through leaving. A counter that only climbs gives that for
 * free.
 *
 * Upstream keeps the counter in a module-global, which is not reproducible even
 * in JavaScript: it climbs for the life of the process, so the same call returns
 * different identities depending on what ran before it. Here the counter is an
 * object, owned by one view's engine and living exactly as long as it does.
 *
 * The consequence for a caller of the standalone `segmentText` and
 * `diffSegments`: identities are only comparable between calls that share a
 * minter.
 */
public class MintedIds {
    private var next = 0

    /**
     * The next identity, skipping any that are already in play.
     *
     * Upstream loops the same way, and it matters here more than it does there:
     * this counter starts at zero for every new minter, so an identity inherited
     * from an earlier value could otherwise be minted a second time.
     */
    internal fun take(used: Set<String>): String {
        var id = mint()
        while (id in used) id = mint()
        return id
    }

    private fun mint(): String {
        val id = "$PREFIX$next"
        next += 1
        return id
    }

    private companion object {
        /**
         * A minted identity cannot collide with one derived from text, because a
         * NULL is not a character any value carries.
         */
        const val PREFIX = "\u0000n"
    }
}
