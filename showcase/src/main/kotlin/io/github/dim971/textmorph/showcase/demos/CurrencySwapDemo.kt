package io.github.dim971.textmorph.showcase.demos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.dim971.textmorph.compose.TextMorph
import io.github.dim971.textmorph.engine.TextMorphFont
import io.github.dim971.textmorph.showcase.catalog.Demo
import io.github.dim971.textmorph.showcase.shared.LocalShowcaseSettings
import io.github.dim971.textmorph.showcase.shared.Tappable
import io.github.dim971.textmorph.showcase.shared.rememberCycle
import io.github.dim971.textmorph.showcase.shared.showcaseColour
import java.util.Locale

/** The same amount in another locale. */
@Composable
fun CurrencySwapDemo() {
    val settings = LocalShowcaseSettings.current
    val cycle =
        rememberCycle(
            "en-US" to "$",
            "de-DE" to "€",
            "fr-FR" to "€",
            "en-GB" to "£",
        )
    val (tag, symbol) = cycle.current

    Tappable(hint = "Tap to change the locale", advance = cycle::advance) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row {
                Text(
                    text = symbol,
                    modifier = Modifier.alignByBaseline(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextMorph(
                    value = 1234.5,
                    modifier = Modifier.alignByBaseline(),
                    options = settings.options(decimals = 2, locale = Locale.forLanguageTag(tag)),
                    font = TextMorphFont(fontSize = 36.sp, fontWeight = FontWeight.SemiBold),
                    colour = showcaseColour(),
                )
            }
            Text(
                text = tag,
                style = MaterialTheme.typography.labelMedium,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

val currencySwapDemo =
    Demo(
        id = "currency",
        name = "Currency swap",
        summary =
            "One number, four locales. The decimal separator is the pivot every column is " +
                "measured from, so changing it moves every digit; the grouping separator changes shape " +
                "with it, and some locales group with a space rather than a comma.",
        capability = "the locale's decimal separator as the pivot",
        code =
            """
            TextMorph(
                value = amount,
                options = TextMorphOptions(
                    decimals = 2,
                    locale = Locale.forLanguageTag("de-DE"),
                ),
            )
            """.trimIndent(),
        content = { CurrencySwapDemo() },
    )
