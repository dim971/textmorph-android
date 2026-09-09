package io.github.dim971.textmorph.showcase.demos

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp
import io.github.dim971.textmorph.compose.TextMorph
import io.github.dim971.textmorph.showcase.catalog.Demo
import io.github.dim971.textmorph.showcase.shared.LocalShowcaseSettings
import io.github.dim971.textmorph.showcase.shared.ShowcaseSettings
import io.github.dim971.textmorph.showcase.shared.Stage
import io.github.dim971.textmorph.showcase.shared.rememberTicker
import io.github.dim971.textmorph.showcase.shared.showcaseColour
import io.github.dim971.textmorph.showcase.shared.stageFont

private val DIMENSIONS =
    listOf(
        "320 \u00D7 240",
        "640 \u00D7 480",
        "1280 \u00D7 720",
        "1920 \u00D7 1080",
    )

/** Two quantities either side of a multiplication sign. */
@Composable
fun DimensionsDemo() {
    val settings = LocalShowcaseSettings.current
    val index = rememberTicker(DIMENSIONS.size, 1600)

    Stage {
        TextMorph(
            text = DIMENSIONS[index],
            options = settings.optionsWith(ShowcaseSettings.UpstreamSpring),
            font = stageFont(size = 30.sp, mono = true),
            colour = showcaseColour(),
        )
    }
}

val dimensionsDemo =
    Demo(
        id = "dimensions",
        name = "Dimensions",
        summary =
            "Two quantities in one value, each rolling by its own columns, with a symbol " +
                "between them that belongs to neither. Both gain a digit at 1280, so both carry, and " +
                "the multiplication sign travels the distance the left one grew.",
        capability = "two numeric words in one value",
        code =
            """
            TextMorph(
                text = "1280 \u00D7 720",
                options = TextMorphOptions(
                    ease = TextMorphEase.spring(stiffness = 150.0, damping = 19.0, mass = 1.2),
                ),
            )
            """.trimIndent(),
        content = { DimensionsDemo() },
    )
