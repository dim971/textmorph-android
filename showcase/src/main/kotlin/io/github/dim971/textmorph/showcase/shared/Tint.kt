package io.github.dim971.textmorph.showcase.shared

// The colours the tinted cards can be drawn in, and how to write on them.
//
// Upstream's own, read out of its stylesheet and its rating scale rather than
// picked to look similar: `--primary: #ffce44` is the accent the whole site
// runs on, and the other five are the tones its rating slider walks through.
// A palette that merely rhymes with a reference is a palette that drifts from
// it; these are the same numbers.

import androidx.compose.ui.graphics.Color
import kotlin.math.pow

/** One choice of tint, with a name a reader can say. */
enum class ShowcaseTint(
    val label: String,
    val colour: Color,
) {
    /** Upstream's `--primary`, and the default here for the same reason. */
    Amber("Amber", Color(0xFFFFCE44)),
    Yellow("Yellow", Color(0xFFF0B429)),
    Orange("Orange", Color(0xFFFF7A2F)),
    Red("Red", Color(0xFFF2453D)),
    Green("Green", Color(0xFF34C759)),
    Blue("Blue", Color(0xFF3B82F6)),
    ;

    /** Black or white, whichever this tint can be read through. */
    val ink: Color get() = onTint(colour)
}

/**
 * Black or white on a given colour, whichever reads better.
 *
 * WCAG relative luminance, and the threshold is where the contrast against
 * black and the contrast against white are equal rather than a number chosen by
 * eye. It matters here because the palette runs from an amber that wants black
 * to a theme accent that wants white, and a hardcoded ink is only ever right for
 * one of them: this replaces a black that was hardcoded on six cards, and was
 * wrong on all of them under the default Material accent.
 */
fun onTint(background: Color): Color =
    if (luminance(background) > CONTRAST_PIVOT) Color.Black else Color.White

/** The luminance at which black and white are equally readable. */
private const val CONTRAST_PIVOT = 0.179f

private fun luminance(colour: Color): Float =
    0.2126f * linear(colour.red) + 0.7152f * linear(colour.green) + 0.0722f * linear(colour.blue)

/** One sRGB channel, undone back to light. */
private fun linear(channel: Float): Float =
    if (channel <= 0.04045f) channel / 12.92f else ((channel + 0.055f) / 1.055f).pow(2.4f)
