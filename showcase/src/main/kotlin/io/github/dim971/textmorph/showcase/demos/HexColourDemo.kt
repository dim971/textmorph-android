package io.github.dim971.textmorph.showcase.demos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.dim971.textmorph.compose.TextMorph
import io.github.dim971.textmorph.engine.TextMorphFont
import io.github.dim971.textmorph.showcase.catalog.Demo
import io.github.dim971.textmorph.showcase.shared.LocalShowcaseSettings
import io.github.dim971.textmorph.showcase.shared.Tappable
import io.github.dim971.textmorph.showcase.shared.rememberCycle
import io.github.dim971.textmorph.showcase.shared.showcaseColour

/** A value with no spaces in it at all. */
@Composable
fun HexColourDemo() {
    val settings = LocalShowcaseSettings.current
    val cycle = rememberCycle("#FF6B35", "#004E89", "#1A659E", "#EFEFD0")
    val swatch = Color(cycle.current.drop(1).toLong(16) or 0xFF000000L)

    Tappable(hint = "Tap to change the colour", advance = cycle::advance) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier
                    .size(44.dp)
                    .background(swatch, RoundedCornerShape(8.dp)),
            )
            TextMorph(
                text = cycle.current,
                options = settings.options,
                font = TextMorphFont(fontFamily = FontFamily.Monospace, fontSize = 26.sp),
                colour = showcaseColour(),
            )
        }
    }
}

val hexColourDemo =
    Demo(
        id = "hex",
        name = "Hex colour",
        summary =
            "No spaces, so the value is cut into grapheme clusters and morphs letter by " +
                "letter. That is the path most of this library's use takes: a counter, a price or a " +
                "code is one word.",
        capability = "the grapheme path of the segmenter",
        code =
            """
            TextMorph(
                text = hex,
                font = TextMorphFont(fontFamily = FontFamily.Monospace, fontSize = 26.sp),
            )
            """.trimIndent(),
        content = { HexColourDemo() },
    )
