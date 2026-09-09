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

private val CURRENCIES = listOf("${'$'}99.00", "\u20AC99.00", "\u00A399.00", "\u00A599.00")

/** The same amount behind four different symbols. */
@Composable
fun CurrencySwapDemo() {
    val settings = LocalShowcaseSettings.current
    val index = rememberTicker(CURRENCIES.size, 1400)

    Stage {
        TextMorph(
            text = CURRENCIES[index],
            options = settings.options,
            font = stageFont(size = 40.sp),
            colour = showcaseColour(),
        )
    }
}

val currencySwapDemo =
    Demo(
        id = "currency",
        name = "Currency swap",
        summary =
            "Only the symbol changes, and the digits do not move at all. A currency symbol " +
                "is an affix: it is trimmed before the columns are aligned, so it is free to be " +
                "replaced without disturbing the number it sits in front of. The four symbols are " +
                "different widths, so the box breathes while the digits hold.",
        capability = "affix trimming, seen from the affix's side",
        code =
            """
            TextMorph(text = "${'$'}99.00")
            TextMorph(text = "\u20AC99.00")
            """.trimIndent(),
        content = { CurrencySwapDemo() },
    )
