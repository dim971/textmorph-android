package io.github.dim971.textmorph.showcase.demos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.dim971.textmorph.compose.TextMorph
import io.github.dim971.textmorph.showcase.catalog.Demo
import io.github.dim971.textmorph.showcase.shared.Caption
import io.github.dim971.textmorph.showcase.shared.LocalShowcaseSettings
import io.github.dim971.textmorph.showcase.shared.Stage
import io.github.dim971.textmorph.showcase.shared.rememberReducedMotion
import io.github.dim971.textmorph.showcase.shared.showcaseColour
import io.github.dim971.textmorph.showcase.shared.stageFont
import kotlinx.coroutines.delay

private const val SCRIPT = "1234567.89"
private const val TYPE_MS = 220L

/** A beat on the formatted total before it clears and starts over. */
private const val HOLD_MS = 1800L

/** The digits so far, grouped the way a field would show them. */
private fun formatted(typed: String): String {
    if (typed.isEmpty()) return "0"
    val point = typed.indexOf('.')
    val whole = if (point < 0) typed else typed.substring(0, point)
    val rest = if (point < 0) "" else typed.substring(point)
    val grouped =
        whole
            .reversed()
            .chunked(3)
            .joinToString(",")
            .reversed()
    return grouped + rest
}

/** A field being typed into, where the caret says where the edit was. */
@Composable
fun NumoraFieldDemo() {
    val settings = LocalShowcaseSettings.current
    var typed by remember { mutableIntStateOf(0) }
    val still = rememberReducedMotion()

    LaunchedEffect(still) {
        if (still) {
            typed = SCRIPT.length
            return@LaunchedEffect
        }
        while (true) {
            val done = typed >= SCRIPT.length
            delay(if (done) HOLD_MS else TYPE_MS)
            typed = if (done) 0 else typed + 1
        }
    }

    val value = formatted(SCRIPT.take(typed))

    Stage {
        Box(
            modifier =
                Modifier
                    .width(240.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextMorph(
                    text = value,
                    options = settings.options(decimals = null),
                    font = stageFont(size = 30.sp, mono = true),
                    colour = showcaseColour(),
                    // The caret is at the end of what has been typed, and the
                    // whole point of passing it is that the digits either side
                    // hold their identity instead of realigning by column.
                    cursorIndex = value.length,
                )
                Box(
                    Modifier
                        .padding(start = 2.dp)
                        .width(2.dp)
                        .height(30.dp)
                        .background(MaterialTheme.colorScheme.primary),
                )
            }
        }
        Caption("typing, with the caret passed in")
    }
}

val numoraFieldDemo =
    Demo(
        id = "field",
        name = "Number field",
        summary =
            "A number typed one character at a time, with the caret handed to the morph. " +
                "Every keystroke pushes a grouping separator along, so without a caret the digits " +
                "would realign by column and half of them would slide a place on every press. With " +
                "one, both sides of the edit hold their identity and only the keystroke is new.",
        capability = "caret matching instead of place matching",
        code =
            """
            TextMorph(text = formatted(typed), cursorIndex = caret)
            """.trimIndent(),
        content = { NumoraFieldDemo() },
    )
