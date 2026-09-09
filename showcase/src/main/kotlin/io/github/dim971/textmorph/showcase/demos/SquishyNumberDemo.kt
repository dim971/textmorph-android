package io.github.dim971.textmorph.showcase.demos

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp
import io.github.dim971.textmorph.compose.TextMorph
import io.github.dim971.textmorph.showcase.catalog.Demo
import io.github.dim971.textmorph.showcase.shared.LocalShowcaseSettings
import io.github.dim971.textmorph.showcase.shared.Stage
import io.github.dim971.textmorph.showcase.shared.rememberTicker
import io.github.dim971.textmorph.showcase.shared.showcaseColour
import io.github.dim971.textmorph.showcase.shared.stageFont

private val FIGURES = listOf("1,248,392", "1,248K", "1.2M", "1M")

/** One count, written four ways. */
@Composable
fun SquishyNumberDemo() {
    val settings = LocalShowcaseSettings.current
    val index = rememberTicker(FIGURES.size, 3400)

    Stage {
        TextMorph(
            text = FIGURES[index],
            options = settings.options,
            font = stageFont(size = 34.sp),
            colour = showcaseColour(),
        )
    }
}

val squishyNumberDemo =
    Demo(
        id = "squishy",
        name = "Squishy number",
        summary =
            "1,248,392 shortening to 1,248K to 1.2M to 1M. Each step drops most of the " +
                "number, and the digits that survive are the leading ones, so the value collapses " +
                "from the right rather than being replaced. The step from 1,248K to 1.2M is the " +
                "interesting one: the 1 and the 2 hold while everything between them leaves.",
        capability = "a quantity losing columns from the right",
        code =
            """
            TextMorph(text = abbreviated(count))
            """.trimIndent(),
        content = { SquishyNumberDemo() },
    )
