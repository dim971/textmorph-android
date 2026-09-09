package io.github.dim971.textmorph.showcase.demos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.dim971.textmorph.compose.TextMorph
import io.github.dim971.textmorph.core.TextMorphEase
import io.github.dim971.textmorph.core.TextMorphOptions
import io.github.dim971.textmorph.engine.TextMorphFont
import io.github.dim971.textmorph.showcase.catalog.Demo
import io.github.dim971.textmorph.showcase.shared.showcaseColour

/**
 * Upstream's own spring for this example, and deliberately not the catalogue's:
 * the point of the screen is what a spring does.
 */
private val springOptions =
    TextMorphOptions(
        ease = TextMorphEase.spring(stiffness = 150.0, damping = 19.0, mass = 1.2),
        decimals = 0,
    )

/** A morph on a spring rather than a curve. */
@Composable
fun SpinDialDemo() {
    var value by remember { mutableDoubleStateOf(24.0) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TextMorph(
            value = value,
            options = springOptions,
            font = TextMorphFont(fontSize = 44.sp, fontWeight = FontWeight.Bold),
            colour = showcaseColour(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = { value = (value - 1).coerceAtLeast(0.0) }) { Text("-") }
            OutlinedButton(onClick = { value = (value + 1).coerceAtMost(99.0) }) { Text("+") }
        }
    }
}

val spinDialDemo =
    Demo(
        id = "spin",
        name = "Spin dial",
        summary =
            "A spring settles on its own physics and ignores the duration. It also " +
                "overshoots, which is why a morph is allowed to draw outside its own box: a glyph " +
                "clipped at the peak of its bounce is a bug that only appears here.",
        capability = "a spring, and the overshoot the surface allows for",
        code =
            """
            TextMorph(
                value = value,
                options = TextMorphOptions(
                    ease = TextMorphEase.spring(stiffness = 150.0, damping = 19.0, mass = 1.2),
                    decimals = 0,
                ),
            )
            """.trimIndent(),
        content = { SpinDialDemo() },
    )
