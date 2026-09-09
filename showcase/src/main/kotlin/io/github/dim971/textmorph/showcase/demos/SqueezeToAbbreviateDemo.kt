package io.github.dim971.textmorph.showcase.demos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.dim971.textmorph.compose.TextMorph
import io.github.dim971.textmorph.core.NumberFormatting
import io.github.dim971.textmorph.core.defaultMorphLocale
import io.github.dim971.textmorph.engine.TextMorphFont
import io.github.dim971.textmorph.showcase.catalog.Demo
import io.github.dim971.textmorph.showcase.shared.LocalShowcaseSettings
import io.github.dim971.textmorph.showcase.shared.showcaseColour
import kotlin.math.roundToInt

private const val COUNT = 1_204_318.0

/** The widest form that fits. */
private fun abbreviated(room: Float): String =
    when {
        room > 170 -> NumberFormatting.format(COUNT, 0, defaultMorphLocale)
        room > 120 -> "${(COUNT / 1000).roundToInt()}k"
        else -> "${(COUNT / 100_000).roundToInt() / 10.0}M"
    }

/** A number that abbreviates as it runs out of room. */
@Composable
fun SqueezeToAbbreviateDemo() {
    val settings = LocalShowcaseSettings.current
    var room by remember { mutableFloatStateOf(220f) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(Modifier.width(room.dp).clipToBounds()) {
            TextMorph(
                text = abbreviated(room),
                options = settings.options,
                font = TextMorphFont(fontSize = 32.sp, fontWeight = FontWeight.SemiBold),
                colour = showcaseColour(),
            )
        }
        Text("Room")
        Slider(
            value = room,
            onValueChange = { room = it },
            valueRange = 70f..240f,
            modifier = Modifier.width(260.dp),
        )
    }
}

val squeezeToAbbreviateDemo =
    Demo(
        id = "squeeze",
        name = "Squeeze to abbreviate",
        summary =
            "Drag the slider. As the room runs out the value switches to a shorter form, and " +
                "the digits that survive the switch carry across rather than the whole number being " +
                "replaced.",
        capability = "a magnitude jump, and the cap that stops one smearing",
        code =
            """
            // 1,204,318 -> 1204k -> 1.2M as the room runs out
            TextMorph(text = abbreviated(room))
            """.trimIndent(),
        content = { SqueezeToAbbreviateDemo() },
    )
