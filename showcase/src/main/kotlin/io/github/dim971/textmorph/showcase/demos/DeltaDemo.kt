package io.github.dim971.textmorph.showcase.demos

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import io.github.dim971.textmorph.compose.TextMorph
import io.github.dim971.textmorph.engine.TextMorphFont
import io.github.dim971.textmorph.showcase.catalog.Demo
import io.github.dim971.textmorph.showcase.shared.LocalShowcaseSettings
import io.github.dim971.textmorph.showcase.shared.Tappable
import io.github.dim971.textmorph.showcase.shared.rememberCycle
import kotlin.math.abs

/** A signed change, where the sign is part of the morph. */
@Composable
fun DeltaDemo() {
    val settings = LocalShowcaseSettings.current
    val cycle = rememberCycle(2.4, -1.8, 5.1, -0.3, 12.7)
    val reading = cycle.current
    val sign = if (reading < 0) "-" else "+"

    Tappable(hint = "Tap for the next reading", advance = cycle::advance) {
        TextMorph(
            text = "$sign${abs(reading)}%",
            options = settings.options,
            font = TextMorphFont(fontSize = 34.sp, fontWeight = FontWeight.Medium),
            colour = if (reading < 0) Color(0xFFD1453B) else Color(0xFF1E8E3E),
        )
    }
}

val deltaDemo =
    Demo(
        id = "delta",
        name = "Delta",
        summary =
            "A sign and a percent sign around the digits. Both are affixes: they pair off " +
                "before the columns are aligned, so the digits still roll by place while the sign " +
                "changes on its own.",
        capability = "affix trimming, and symbols sliding from below",
        code =
            """
            TextMorph(
                text = "${'$'}sign${'$'}{abs(change)}%",
                colour = if (change < 0) Color.Red else Color.Green,
            )
            """.trimIndent(),
        content = { DeltaDemo() },
    )
