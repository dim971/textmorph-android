package io.github.dim971.textmorph.engine

// Resolving a font this port can actually measure.
//
// A morph shapes the value itself, so it needs a `Typeface` and a size in
// pixels. A Compose `TextStyle` carries neither directly: the family is a
// description that a resolver turns into a typeface, and the size is in scaled
// pixels that only a `Density` can turn into pixels. Both of those depend on the
// composition, so the font is described here and resolved where the composition
// can be read.

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * How a morph should be drawn.
 *
 * The same fields a `TextStyle` carries for the face, and nothing else: colour
 * is a parameter of the composable, and alignment comes from the layout
 * direction and the text alignment.
 */
@Immutable
public data class TextMorphFont(
    /** The face. Null means the default, which is what a `BasicText` uses. */
    public val fontFamily: FontFamily? = null,
    /**
     * The size, in scaled pixels.
     *
     * Scaled pixels rather than pixels, so the system's font scale applies, and
     * converted through `Density.toPx` rather than multiplied by the scale, so
     * Android 14's non-linear scaling is honoured rather than approximated.
     */
    public val fontSize: TextUnit = DEFAULT_SIZE,
    public val fontWeight: FontWeight = FontWeight.Normal,
    public val fontStyle: FontStyle = FontStyle.Normal,
    /**
     * Extra space between characters, in ems.
     *
     * Applied to the paint, so it is part of the shaping rather than added
     * afterwards, and therefore part of every offset the layout is built from.
     */
    public val letterSpacing: TextUnit = TextUnit.Unspecified,
    /** Whether a missing weight or slant may be synthesised. */
    public val fontSynthesis: FontSynthesis = FontSynthesis.All,
) {
    public companion object {
        /** What a `BasicText` uses when nothing says otherwise. */
        public val DEFAULT_SIZE: TextUnit = 14.sp

        /** The default description. */
        public val Default: TextMorphFont = TextMorphFont()
    }
}
