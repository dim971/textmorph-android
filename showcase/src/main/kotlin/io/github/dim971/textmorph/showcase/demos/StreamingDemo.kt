package io.github.dim971.textmorph.showcase.demos

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import io.github.dim971.textmorph.compose.TextMorph
import io.github.dim971.textmorph.showcase.catalog.Demo
import io.github.dim971.textmorph.showcase.shared.LocalShowcaseSettings
import io.github.dim971.textmorph.showcase.shared.Stage
import io.github.dim971.textmorph.showcase.shared.rememberReducedMotion
import io.github.dim971.textmorph.showcase.shared.showcaseColour
import io.github.dim971.textmorph.showcase.shared.stageFont
import io.github.dim971.textmorph.showcase.shared.wrap
import kotlinx.coroutines.delay

private const val PASSAGE =
    "The capital of Australia is Canberra, which sits in the Australian Capital " +
        "Territory between Sydney and Melbourne. It was chosen in 1908 as a compromise " +
        "between the two rival cities, and Walter Burley Griffin and Marion Mahony " +
        "Griffin won the competition to design it. Their plan set the city around a lake " +
        "and a grid of axes and circles, and today it holds Parliament House, the High " +
        "Court, and the National Gallery."

private val WORDS = PASSAGE.split(" ")

private const val WORD_MS = 110L

/** A beat on the finished passage before it starts over. */
private const val HOLD_MS = 2400L

/** A passage arriving a word at a time, the way a model writes it. */
@Composable
fun StreamingDemo() {
    val settings = LocalShowcaseSettings.current
    var count by remember { mutableIntStateOf(1) }
    val still = rememberReducedMotion()

    LaunchedEffect(still) {
        if (still) {
            count = WORDS.size
            return@LaunchedEffect
        }
        while (true) {
            val done = count >= WORDS.size
            delay(if (done) HOLD_MS else WORD_MS)
            count = if (done) 1 else count + 1
        }
    }

    Stage {
        TextMorph(
            text = wrap(WORDS.take(count).joinToString(" "), 28),
            options = settings.options,
            font = stageFont(size = 17.sp, weight = androidx.compose.ui.text.font.FontWeight.Normal),
            colour = showcaseColour(),
            textAlign = TextAlign.Start,
        )
    }
}

val streamingDemo =
    Demo(
        id = "streaming",
        name = "Streaming",
        summary =
            "Seventy words arriving one at a time, nine a second, against a morph that takes " +
                "four hundred milliseconds. Every word after the first interrupts a morph in flight, " +
                "and the passage reflows as it grows because the line breaks are recomputed each time. " +
                "The words already on screen hold still through all of it.",
        capability = "morphs arriving faster than they settle, over a reflowing value",
        code =
            """
            // there is no automatic wrapping, so the demo says where the lines go
            TextMorph(text = wrap(words.take(count).joinToString(" "), 28))
            """.trimIndent(),
        content = { StreamingDemo() },
    )
