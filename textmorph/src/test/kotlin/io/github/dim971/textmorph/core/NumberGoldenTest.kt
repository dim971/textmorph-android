package io.github.dim971.textmorph.core

import com.google.gson.JsonObject
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

/**
 * The number rules and the place alignment, against the JavaScript original.
 *
 * Pairings are recorded as an alignment array, an index in the old value per
 * character of the new one, which says nothing about how either side mints
 * identities and is therefore the only honest way to compare two
 * implementations of this.
 */
class NumberGoldenTest {
    private val goldens = Fixtures.json("goldens")
    private val rules = goldens.getAsJsonObject("numberRules")
    private val numbers = goldens.getAsJsonObject("segmentNumber")

    @Test
    fun `whether a token is a quantity`() {
        val failures = ArrayList<String>()
        val deviations = ArrayList<String>()

        for (element in rules.getAsJsonArray("isNumericWord")) {
            val case = element.asJsonObject
            val token = case.get("token").asString
            val expected = case.get("result").asBoolean
            val actual = NumberRules.isNumericWord(token)
            if (actual == expected) continue

            // The one deliberate widening: U+2019 is a separator here and is not
            // upstream, because CLDR groups Swiss German with it in some versions
            // and a device's OS decides which. Recorded rather than hidden, and
            // only in this direction: a token upstream reads as a quantity and
            // this does not is still a failure.
            if (token.contains('’') && actual && !expected) {
                deviations.add(escaped(token))
                continue
            }
            failures.add("  ${escaped(token)}: expected $expected, got $actual")
        }

        assertTrue(report(failures), failures.isEmpty())
        assertTrue(
            "the corpus should carry the U+2019 token the widening is for",
            deviations.isNotEmpty(),
        )
    }

    @Test
    fun `the locale's decimal separator`() {
        val failures =
            rules.getAsJsonArray("decimalSeparator").mapNotNull { element ->
                val case = element.asJsonObject
                val locale = case.get("locale").asString
                val expected = case.get("separator").asString
                val actual = NumberRules.decimalSeparator(Locale.forLanguageTag(locale)).toString()
                if (actual == expected) {
                    null
                } else {
                    "  $locale: expected ${escaped(expected)}, got ${escaped(actual)}"
                }
            }
        assertTrue(report(failures), failures.isEmpty())
    }

    @Test
    fun `a number with nothing to carry from`() {
        val failures =
            numbers.getAsJsonArray("fresh").mapNotNull { element ->
                val case = element.asJsonObject
                val value = case.get("value").asString
                val expected = case.getAsJsonArray("segments").map { goldenSegment(it.asJsonObject) }
                val actual =
                    NumberSegmenter
                        .segmentNumber(value, minter = MintedIds())
                        .map { it.segment }
                        .asGolden()
                if (actual == expected) {
                    null
                } else {
                    "  ${escaped(value)}\n    expected $expected\n    got      $actual"
                }
            }
        assertTrue(report(failures), failures.isEmpty())
    }

    @Test
    fun `matching by place value, with a full stop for a decimal separator`() {
        checkPlace(numbers.getAsJsonArray("place").map { it.asJsonObject }, '.')
    }

    @Test
    fun `matching by place value, with a comma for a decimal separator`() {
        checkPlace(numbers.getAsJsonArray("placeComma").map { it.asJsonObject }, ',')
    }

    @Test
    fun `matching by caret, for a field being typed into`() {
        val failures =
            numbers.getAsJsonArray("cursor").mapNotNull { element ->
                val case = element.asJsonObject
                val minter = MintedIds()
                val previous =
                    NumberSegmenter.segmentNumber(
                        case.get("before").asString,
                        minter = minter,
                    )
                val actual =
                    NumberSegmenter.segmentNumber(
                        case.get("after").asString,
                        previous = previous,
                        cursor = UTF16Offset(case.get("cursor").asInt),
                        decimalCharacter = '.',
                        minter = minter,
                    )
                val strings = actual.map { it.string }
                val expectedStrings = case.getAsJsonArray("strings").map { it.asString }
                val actualAlignment = alignment(actual.map { it.id }, previous.map { it.id })
                val expectedAlignment =
                    case.getAsJsonArray("alignment").map {
                        Fixtures.nullableInt(it)
                    }

                if (strings == expectedStrings && actualAlignment == expectedAlignment) {
                    null
                } else {
                    "  ${escaped(case.get("before").asString)} to " +
                        "${escaped(case.get("after").asString)}, caret ${case.get("cursor").asInt}\n" +
                        "    expected strings $expectedStrings " +
                        "alignment ${describe(expectedAlignment)}\n" +
                        "    got      strings $strings alignment ${describe(actualAlignment)}"
                }
            }
        assertTrue(report(failures), failures.isEmpty())
    }

    private fun checkPlace(
        cases: List<JsonObject>,
        decimalCharacter: Char,
    ) {
        val failures =
            cases.mapNotNull { case ->
                val minter = MintedIds()
                val previous =
                    NumberSegmenter.segmentNumber(
                        case.get("before").asString,
                        minter = minter,
                    )
                val actual =
                    NumberSegmenter.segmentNumber(
                        case.get("after").asString,
                        previous = previous,
                        decimalCharacter = decimalCharacter,
                        minter = minter,
                    )
                val actualAlignment = alignment(actual.map { it.id }, previous.map { it.id })
                val expectedAlignment =
                    case.getAsJsonArray("alignment").map {
                        Fixtures.nullableInt(it)
                    }

                val problems = ArrayList<String>()
                if (actualAlignment != expectedAlignment) {
                    problems.add(
                        "    alignment expected ${describe(expectedAlignment)} " +
                            "got ${describe(actualAlignment)}",
                    )
                }
                case.getAsJsonArray("strings")?.let { expected ->
                    val strings = actual.map { it.string }
                    val want = expected.map { it.asString }
                    if (strings != want) {
                        problems.add("    strings expected $want got $strings")
                    }
                }
                case.getAsJsonArray("kinds")?.let { expected ->
                    val kinds = actual.map { it.kind.rawValue }
                    val want = expected.map { it.asString }
                    if (kinds != want) {
                        problems.add("    kinds expected $want got $kinds")
                    }
                }

                if (problems.isEmpty()) {
                    null
                } else {
                    "  ${escaped(case.get("before").asString)} to " +
                        "${escaped(case.get("after").asString)}\n" + problems.joinToString("\n")
                }
            }
        assertTrue(report(failures), failures.isEmpty())
    }
}
