package io.github.dim971.textmorph.showcase.demos

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import io.github.dim971.textmorph.compose.TextMorph
import io.github.dim971.textmorph.showcase.catalog.Demo
import io.github.dim971.textmorph.showcase.shared.LocalShowcaseSettings
import io.github.dim971.textmorph.showcase.shared.ShowcaseSettings
import io.github.dim971.textmorph.showcase.shared.Stage
import io.github.dim971.textmorph.showcase.shared.rememberTicker
import io.github.dim971.textmorph.showcase.shared.showcaseColour
import io.github.dim971.textmorph.showcase.shared.stageFont

// U+2212, not a hyphen: it is the width of the plus it replaces, so the digits
// beside it do not shift when the sign changes. Upstream's own note.
private val DELTAS = listOf("+2.4%", "\u22120.8%", "+11.2%", "0.0%", "\u221213.6%")

private fun tone(value: String): Color =
    when {
        value.startsWith("\u2212") -> Color(0xFFFF6B6B)
        value.startsWith("+") -> Color(0xFF4ADE80)
        else -> Color.Unspecified
    }

/** A signed change, where the sign is part of the morph. */
@Composable
fun DeltaDemo() {
    val settings = LocalShowcaseSettings.current
    val index = rememberTicker(DELTAS.size, 1600)
    val value = DELTAS[index]
    val colour = tone(value)

    Stage {
        TextMorph(
            text = value,
            options = settings.optionsWith(ShowcaseSettings.UpstreamSpring),
            font = stageFont(size = 34.sp),
            colour = if (colour == Color.Unspecified) showcaseColour() else colour,
        )
    }
}

val deltaDemo =
    Demo(
        id = "delta",
        name = "Delta",
        summary =
            "A sign in front and a percent sign behind, both affixes, so they pair off before " +
                "the columns are aligned and the digits still roll by place. The sign is U+2212 rather " +
                "than a hyphen, which is upstream's choice and a good one: it is the width of the plus " +
                "it replaces, so nothing shifts when the reading turns negative.",
        capability = "affixes at both ends, and symbols sliding from below",
        code =
            """
            TextMorph(
                text = "\u22120.8%",
                colour = if (change < 0) Color.Red else Color.Green,
            )
            """.trimIndent(),
        content = { DeltaDemo() },
    )
