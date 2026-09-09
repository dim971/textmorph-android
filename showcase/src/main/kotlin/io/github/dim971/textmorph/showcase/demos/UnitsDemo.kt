package io.github.dim971.textmorph.showcase.demos

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import io.github.dim971.textmorph.compose.TextMorph
import io.github.dim971.textmorph.engine.TextMorphFont
import io.github.dim971.textmorph.showcase.catalog.Demo
import io.github.dim971.textmorph.showcase.shared.LocalShowcaseSettings
import io.github.dim971.textmorph.showcase.shared.Tappable
import io.github.dim971.textmorph.showcase.shared.rememberCycle
import io.github.dim971.textmorph.showcase.shared.showcaseColour

/** A quantity and its unit, which change together. */
@Composable
fun UnitsDemo() {
    val settings = LocalShowcaseSettings.current
    val cycle = rememberCycle("1.2 GB", "980 MB", "412 MB", "8.4 GB", "64 KB")

    Tappable(hint = "Tap for the next size", advance = cycle::advance) {
        TextMorph(
            text = cycle.current,
            options = settings.options,
            font = TextMorphFont(fontSize = 34.sp, fontWeight = FontWeight.Medium),
            colour = showcaseColour(),
        )
    }
}

val unitsDemo =
    Demo(
        id = "units",
        name = "Units",
        summary =
            "Two words, one of them a quantity and one of them not. The quantity morphs by " +
                "place value and the unit morphs by character, in the same value, because the numeric " +
                "pass runs over the finished segmentation rather than inside it.",
        capability = "a numeric word beside a plain one",
        code =
            """
            TextMorph(text = "${'$'}amount ${'$'}unit")
            """.trimIndent(),
        content = { UnitsDemo() },
    )
