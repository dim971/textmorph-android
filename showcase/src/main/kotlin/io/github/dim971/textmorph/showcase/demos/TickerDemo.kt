package io.github.dim971.textmorph.showcase.demos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.dim971.textmorph.compose.TextMorph
import io.github.dim971.textmorph.engine.TextMorphFont
import io.github.dim971.textmorph.showcase.catalog.Demo
import io.github.dim971.textmorph.showcase.shared.LocalShowcaseSettings
import io.github.dim971.textmorph.showcase.shared.Ticking
import io.github.dim971.textmorph.showcase.shared.showcaseColour
import kotlin.math.roundToInt
import kotlin.random.Random

/** A value that changes faster than the morph settles. */
@Composable
fun TickerDemo() {
    val settings = LocalShowcaseSettings.current
    var price by remember { mutableDoubleStateOf(128.44) }

    Ticking(
        intervalMs = 120,
        advance = {
            val next = price + Random.nextDouble(-1.2, 1.2)
            price = ((next.coerceIn(80.0, 180.0)) * 100).roundToInt() / 100.0
        },
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextMorph(
                value = price,
                modifier = Modifier.alignByBaseline(),
                options = settings.options(decimals = 2),
                font =
                    TextMorphFont(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                colour = showcaseColour(),
            )
            Text(
                text = "USD",
                modifier = Modifier.alignByBaseline(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

val tickerDemo =
    Demo(
        id = "ticker",
        name = "Ticker",
        summary =
            "A new value every eighth of a second, against a morph that takes four hundred " +
                "milliseconds. Each morph is interrupted by the next and carries on from where it had " +
                "got to, and the box resumes its curve rather than replaying the opening sliver of it. " +
                "Without that the digits race and the box crawls.",
        capability = "interruption, carried momentum, and the container resuming",
        code =
            """
            // updated every 120ms, against a 400ms morph
            TextMorph(value = price, options = TextMorphOptions(decimals = 2))
            """.trimIndent(),
        content = { TickerDemo() },
    )
