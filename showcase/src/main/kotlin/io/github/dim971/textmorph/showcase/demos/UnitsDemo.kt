package io.github.dim971.textmorph.showcase.demos

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp
import io.github.dim971.textmorph.compose.TextMorph
import io.github.dim971.textmorph.showcase.catalog.Demo
import io.github.dim971.textmorph.showcase.shared.LocalShowcaseSettings
import io.github.dim971.textmorph.showcase.shared.SplitRow
import io.github.dim971.textmorph.showcase.shared.Stage
import io.github.dim971.textmorph.showcase.shared.rememberTicker
import io.github.dim971.textmorph.showcase.shared.showcaseColour
import io.github.dim971.textmorph.showcase.shared.stageFont

private val GLUED = listOf("819K", "990K", "9.9M", "19.4M")
private val SPACED = listOf("910 KB", "1.2 MB", "12 MB", "1.25 GB")

/** A quantity glued to its unit, beside one separated from it. */
@Composable
fun UnitsDemo() {
    val settings = LocalShowcaseSettings.current
    val index = rememberTicker(GLUED.size, 1600)

    Stage {
        SplitRow(
            separator = "-",
            left = {
                TextMorph(
                    text = GLUED[index],
                    options = settings.options,
                    font = stageFont(size = 30.sp),
                    colour = showcaseColour(),
                )
            },
            right = {
                TextMorph(
                    text = SPACED[index],
                    options = settings.options,
                    font = stageFont(size = 30.sp),
                    colour = showcaseColour(),
                )
            },
        )
    }
}

val unitsDemo =
    Demo(
        id = "units",
        name = "Units",
        summary =
            "819K on the left, 910 KB on the right, and the space between the number and " +
                "the unit is the whole difference. Glued, the token is one word and the letter travels " +
                "with the digits; separated, it is two words and the unit morphs on its own while the " +
                "quantity rolls by place value.",
        capability = "a space deciding whether a unit belongs to the number",
        code =
            """
            TextMorph(text = "9.9M")     // one word
            TextMorph(text = "12 MB")    // two, and only the first rolls
            """.trimIndent(),
        content = { UnitsDemo() },
    )
