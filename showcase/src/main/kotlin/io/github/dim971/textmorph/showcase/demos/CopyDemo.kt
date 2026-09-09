package io.github.dim971.textmorph.showcase.demos

import androidx.compose.runtime.Composable
import io.github.dim971.textmorph.compose.TextMorph
import io.github.dim971.textmorph.showcase.catalog.Demo
import io.github.dim971.textmorph.showcase.shared.Chip
import io.github.dim971.textmorph.showcase.shared.LocalShowcaseSettings
import io.github.dim971.textmorph.showcase.shared.Stage
import io.github.dim971.textmorph.showcase.shared.rememberTicker
import io.github.dim971.textmorph.showcase.shared.showcaseColour
import io.github.dim971.textmorph.showcase.shared.stageFont

/** Two words, one letter apart. */
@Composable
fun CopyDemo() {
    val settings = LocalShowcaseSettings.current
    val index = rememberTicker(2, 2000)

    Stage {
        Chip {
            TextMorph(
                text = if (index == 0) "Copy" else "Copied",
                options = settings.options,
                font = stageFont(),
                colour = showcaseColour(),
            )
        }
    }
}

val copyDemo =
    Demo(
        id = "copy",
        name = "Copy",
        summary =
            "Copy becoming Copied. Four letters survive, two arrive, and the pill grows to " +
                "fit rather than jumping, because the container animates its own width.",
        capability = "the container's width as an animated axis",
        code =
            """
            Chip { TextMorph(text = if (copied) "Copied" else "Copy") }
            """.trimIndent(),
        content = { CopyDemo() },
    )
