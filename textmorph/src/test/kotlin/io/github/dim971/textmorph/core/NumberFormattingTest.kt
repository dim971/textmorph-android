package io.github.dim971.textmorph.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

/**
 * Formatting a numeric value, against the JavaScript original.
 *
 * 4650 cases: 25 values, six fraction lengths and 31 locales. Two ICU
 * implementations reading the same CLDR data should agree, and mostly do, but
 * "should" is not a test.
 *
 * They do not read the same CLDR data, and this suite is how that was found out.
 * Two differences are recorded rather than absorbed, each confined to a named
 * set of locales so that anything else still fails.
 *
 * CLDR groups Swiss German with U+0027 in one version and U+2019 in another, so
 * Node's ICU and a device's ICU disagree about de-CH depending on the version.
 * The comparison treats those two apostrophes as one separator. The consequence
 * for the engine is handled in `NumberRules.coreSeparators`.
 *
 * And `java.text` does not carry two things CLDR does: a locale's minimum
 * grouping digits, which is why es-ES, it-IT and pl-PL leave a four-digit number
 * ungrouped on the web and group it here, and a grouping pattern that is not
 * uniform, which is why hi-IN groups by lakh on the web and by thousand here.
 * Neither is reachable through the JDK formatter, and neither makes the port
 * inconsistent with itself, so both are recorded with the locales they affect
 * rather than papered over. `android.icu` would fix them, at the price of a
 * formatter the unit tests cannot reach; that trade is written down in
 * docs/fidelity.md.
 */
class NumberFormattingTest {
    private val cases =
        Fixtures.json("goldens").getAsJsonArray("numberFormatting").map {
            it.asJsonObject
        }

    @Test
    fun `every value, fraction length and locale in the matrix`() {
        val failures = ArrayList<String>()
        val apostrophes = HashSet<String>()
        val grouping = HashSet<String>()

        for (case in cases) {
            val value = case.get("value").asDouble
            val decimals = case.get("decimals").takeIf { !it.isJsonNull }?.asInt
            val locale = case.get("locale").asString
            val expected = case.get("formatted").asString
            val actual =
                NumberFormatting.format(
                    value,
                    decimals,
                    Locale.forLanguageTag(locale),
                )
            if (actual == expected) continue

            if (normalisingApostrophes(actual) == normalisingApostrophes(expected)) {
                apostrophes.add(locale)
                continue
            }

            // The locale's minimum grouping digits, which CLDR carries and
            // java.text does not expose at all: es-ES and it-IT among others do
            // not group a four-digit number, and the JDK formatter always does.
            // Recorded, and only where the two differ by grouping alone.
            if (withoutGrouping(actual, locale) == withoutGrouping(expected, locale)) {
                grouping.add(locale)
                continue
            }

            failures.add(
                "  $value decimals=${decimals ?: "default"} $locale: " +
                    "expected ${escaped(expected)}, got ${escaped(actual)}",
            )
        }

        assertTrue(report(failures, cases.size), failures.isEmpty())
        // Confined to the locale that is known to group with an apostrophe. A
        // second locale appearing here means CLDR moved something else.
        assertTrue(
            "apostrophe differences in ${apostrophes.sorted()}",
            apostrophes.all { it == "de-CH" },
        )
        // Measured, not guessed, and two different causes. es-ES, it-IT and
        // pl-PL give a four-digit number no grouping at all, which is CLDR's
        // minimum grouping digits and is not exposed by java.text. hi-IN groups
        // by lakh, 12,34,567 rather than 1,234,567, which java.text does not do
        // either. A locale appearing or disappearing here means one of those
        // moved, which is worth knowing rather than absorbing.
        assertEquals(setOf("es-ES", "it-IT", "pl-PL", "hi-IN"), grouping)
    }

    @Test
    fun `a half rounds away from zero, not towards positive infinity`() {
        // The distinction only shows on a negative value, and it is the one place
        // a rounding mode's name is easy to trust and be wrong about.
        assertEquals("2", NumberFormatting.format(1.5, 0))
        assertEquals("-2", NumberFormatting.format(-1.5, 0))
        assertEquals("3", NumberFormatting.format(2.5, 0))
        assertEquals("-3", NumberFormatting.format(-2.5, 0))
    }

    @Test
    fun `asking for no fraction length gives Intl's default of at most three`() {
        assertEquals("1.5", NumberFormatting.format(1.5))
        assertEquals("1", NumberFormatting.format(1.0))
        assertEquals("1.235", NumberFormatting.format(1.23456))
        assertEquals(3, NumberFormatting.DEFAULT_MAXIMUM_FRACTION_DIGITS)
    }

    @Test
    fun `a half is found in the decimal form, not in the binary one`() {
        // 1.005 is 1.00499999999999989 as a double. Intl rounds the decimal
        // form and gives 1.01; rounding the binary value gives 1.00. This is the
        // case that made the difference visible, and it is worth a test of its
        // own so a runtime whose shortest-representation printing differs fails
        // here rather than in 110 fixture cases at once.
        assertEquals("1.01", NumberFormatting.format(1.005, 2))
        assertEquals("1.02", NumberFormatting.format(1.015, 2))
        assertEquals("0.14", NumberFormatting.format(0.135, 2))
    }

    /** The two apostrophes CLDR has used to group Swiss German, as one. */
    private fun normalisingApostrophes(value: String): String = value.replace('\u2019', '\'')

    /**
     * The value with the locale's grouping separators removed, so two strings
     * that differ only by grouping compare equal.
     */
    private fun withoutGrouping(
        value: String,
        locale: String,
    ): String {
        val separator =
            java.text.DecimalFormatSymbols
                .getInstance(Locale.forLanguageTag(locale))
                .groupingSeparator
        return value.replace(separator.toString(), "")
    }
}
