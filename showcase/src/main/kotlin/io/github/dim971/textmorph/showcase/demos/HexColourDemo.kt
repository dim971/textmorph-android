package io.github.dim971.textmorph.showcase.demos

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.dim971.textmorph.compose.TextMorph
import io.github.dim971.textmorph.showcase.catalog.Demo
import io.github.dim971.textmorph.showcase.shared.LocalInteractive
import io.github.dim971.textmorph.showcase.shared.LocalShowcaseSettings
import io.github.dim971.textmorph.showcase.shared.Stage
import io.github.dim971.textmorph.showcase.shared.stageFont
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

// Fixed saturation and lightness, so the swatches move hue alone. Upstream's
// own conversion, transcribed rather than replaced by a Color.hsl call, because
// the string it produces is the value being morphed and a different rounding
// would change which characters survive.
private fun channel(
    n: Int,
    hue: Int,
): String {
    val k = (n + hue / 30.0) % 12
    val c = 0.5 - 0.35 * max(-1.0, min(min(k - 3, 9 - k), 1.0))
    return (c * 255)
        .roundToInt()
        .toString(16)
        .uppercase()
        .padStart(2, '0')
}

private fun hex(hue: Int): String = "#${channel(0, hue)}${channel(8, hue)}${channel(4, hue)}"

private val HUES = List(12) { it * 30 }

/** A value with no spaces in it at all. */
@Composable
fun HexColourDemo() {
    val settings = LocalShowcaseSettings.current
    val live = LocalInteractive.current
    var hue by remember { mutableIntStateOf(0) }
    val value = hex(hue)

    Stage {
        TextMorph(
            text = value,
            options = settings.options,
            font = stageFont(size = 30.sp, mono = true),
            colour = Color(("FF" + value.drop(1)).toLong(16)),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (h in HUES) {
                Box(
                    Modifier
                        .size(22.dp)
                        .background(
                            Color(("FF" + hex(h).drop(1)).toLong(16)),
                            RoundedCornerShape(6.dp),
                        ).then(if (live) Modifier.clickable { hue = h } else Modifier),
                )
            }
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
                "code is one word. Tap a swatch and watch which characters survive: two colours a " +
                "third of the wheel apart still share digits.",
        capability = "the grapheme path of the segmenter",
        code =
            """
            TextMorph(
                text = hex,
                font = TextMorphFont(fontFamily = FontFamily.Monospace, fontSize = 30.sp),
            )
            """.trimIndent(),
        content = { HexColourDemo() },
    )
