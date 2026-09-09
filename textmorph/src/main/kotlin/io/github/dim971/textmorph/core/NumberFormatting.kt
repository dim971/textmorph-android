package io.github.dim971.textmorph.core

// A port of the one line of torph that turns a numeric value into a string,
// from packages/torph/src/lib/text-morph/index.ts:
//
//   value.toLocaleString(locale, {
//     minimumFractionDigits: decimals,
//     maximumFractionDigits: decimals,
//   })

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

/** Turning a numeric value into the string a morph is run on. */
public object NumberFormatting {
    /**
     * Intl's default when no fraction length is asked for: at least none, at
     * most three.
     *
     * Passing `undefined` for both options in JavaScript is not the same as
     * passing nothing at all here, so the default is written out. Three is easy
     * to mistake for an arbitrary choice; it is what `Intl.NumberFormat` does,
     * and a value formatted with four decimals here would morph differently from
     * the same value on the web.
     */
    public const val DEFAULT_MINIMUM_FRACTION_DIGITS: Int = 0
    public const val DEFAULT_MAXIMUM_FRACTION_DIGITS: Int = 3

    /**
     * Formats a numeric value the way upstream does.
     *
     * [decimals] sets both the minimum and the maximum fraction length, so
     * `decimals = 2` pads as well as truncates: 1.5 formats as `1.50`.
     *
     * Halves round away from zero. That is `halfExpand`, which is what
     * `Intl.NumberFormat` uses and what ICU calls `HALF_UP`, and it is not the
     * same as rounding towards positive infinity: it takes -1.5 to -2 rather than
     * to -1. The fixtures pin it for negative values rather than trusting the
     * name.
     *
     * The value is taken through its shortest decimal representation before it is
     * rounded, and that is not tidiness. `Intl.NumberFormat` rounds the decimal
     * form, so 1.005 with two places gives 1.01; rounding the binary double
     * instead sees 1.00499999999999989 and gives 1.00. 110 of the 4650 fixture
     * cases turn on it. `BigDecimal.valueOf` is documented to use the canonical
     * string form of the double, which is the shortest representation that round
     * trips, and is therefore the same number `Intl` starts from.
     */
    public fun format(
        value: Double,
        decimals: Int? = null,
        locale: Locale = defaultMorphLocale,
    ): String {
        val format = NumberFormat.getNumberInstance(locale)
        format.isGroupingUsed = true
        format.roundingMode = RoundingMode.HALF_UP
        format.minimumFractionDigits = decimals ?: DEFAULT_MINIMUM_FRACTION_DIGITS
        format.maximumFractionDigits = decimals ?: DEFAULT_MAXIMUM_FRACTION_DIGITS

        if (!value.isFinite()) return format.format(value)
        val scale = decimals ?: DEFAULT_MAXIMUM_FRACTION_DIGITS
        val rounded = BigDecimal.valueOf(value).setScale(scale, RoundingMode.HALF_UP)
        return format.format(rounded)
    }
}

/**
 * The locale upstream defaults to.
 *
 * Deliberately not the device's: the segmentation and the formatting of a number
 * both depend on it, so taking it from there would make the same value morph
 * differently on two phones, and would make a fixture unreproducible.
 */
public val defaultMorphLocale: Locale = Locale.forLanguageTag("en")
