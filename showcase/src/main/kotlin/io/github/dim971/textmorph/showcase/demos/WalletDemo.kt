package io.github.dim971.textmorph.showcase.demos

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import io.github.dim971.textmorph.compose.TextMorph
import io.github.dim971.textmorph.engine.TextMorphFont
import io.github.dim971.textmorph.showcase.catalog.Demo
import io.github.dim971.textmorph.showcase.shared.LocalShowcaseSettings
import io.github.dim971.textmorph.showcase.shared.Tappable
import io.github.dim971.textmorph.showcase.shared.showcaseColour
import kotlin.math.roundToInt
import kotlin.random.Random

/** A balance, which is what place-value morphing is for. */
@Composable
fun WalletDemo() {
    val settings = LocalShowcaseSettings.current
    var balance by remember { mutableDoubleStateOf(1204.0) }

    Tappable(
        hint = "Tap to spend a little",
        advance = {
            val spent = (balance - Random.nextInt(80, 400)).roundToInt().toDouble()
            balance = if (spent < 100) 1204.0 else spent
        },
    ) {
        Row {
            Text(
                text = "$",
                modifier = Modifier.alignByBaseline(),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextMorph(
                value = balance,
                modifier = Modifier.alignByBaseline(),
                options = settings.options(decimals = 2),
                font = TextMorphFont(fontSize = 40.sp, fontWeight = FontWeight.SemiBold),
                colour = showcaseColour(),
            )
        }
    }
}

val walletDemo =
    Demo(
        id = "wallet",
        name = "Wallet",
        summary =
            "The one that makes the case for the whole library. 1,204 becoming 1,318 rolls " +
                "the hundreds and the tens and leaves the thousands alone, because a digit's identity " +
                "is its column rather than its position in the string.",
        capability = "place-value alignment",
        code =
            """
            TextMorph(
                value = balance,
                options = TextMorphOptions(decimals = 2),
                font = TextMorphFont(fontSize = 40.sp, fontWeight = FontWeight.SemiBold),
            )
            """.trimIndent(),
        content = { WalletDemo() },
    )
