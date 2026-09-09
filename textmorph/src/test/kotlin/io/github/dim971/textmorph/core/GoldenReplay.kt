package io.github.dim971.textmorph.core

import com.google.gson.JsonObject

/** The prefix upstream mints a numeric identity with: a NULL and an "n". */
internal const val MINTED_PREFIX: String = "\u0000n"

/**
 * Renaming minted identities the way the fixture renames them.
 *
 * The counter climbs for the life of the process upstream, so the same call
 * gives different identities depending on what ran before it. The fixture
 * canonicalises them in order of first appearance; this does the same, so the
 * two can be compared without either side claiming anything about how the other
 * mints.
 */
internal class Renamer {
    private val seen = LinkedHashMap<String, String>()

    fun name(id: String): String {
        if (!id.startsWith(MINTED_PREFIX)) return id
        return seen.getOrPut(id) { "#${seen.size}" }
    }
}

/** A segment in the shape the fixtures record it. */
internal data class GoldenSegment(
    val id: String,
    val string: String,
    val kind: String?,
) {
    override fun toString(): String {
        val suffix = kind?.let { ":$it" } ?: ""
        return "${escaped(id)}=${escaped(string)}$suffix"
    }
}

internal fun goldenSegment(element: JsonObject): GoldenSegment =
    GoldenSegment(
        id = element.get("id").asString,
        string = element.get("string").asString,
        kind = element.get("kind")?.takeIf { !it.isJsonNull }?.asString,
    )

/** The segments a port produced, in the fixture's own shape. */
internal fun List<Segment>.asGolden(renamer: Renamer = Renamer()): List<GoldenSegment> =
    map { GoldenSegment(renamer.name(it.id), it.string, it.kind?.rawValue) }

/** Which old segment each new one carries on from, by index. */
internal fun alignment(
    next: List<String>,
    previous: List<String>,
): List<Int?> {
    val positions = previous.withIndex().associate { (index, id) -> id to index }
    return next.map { positions[it] }
}

/** An alignment, printed so a failure is readable. */
internal fun describe(alignment: List<Int?>): String =
    alignment.joinToString(", ", "[", "]") { it?.toString() ?: "-" }
