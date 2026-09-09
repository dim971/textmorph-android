package io.github.dim971.textmorph.showcase.demos

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.sp
import io.github.dim971.textmorph.compose.TextMorph
import io.github.dim971.textmorph.engine.TextMorphFont
import io.github.dim971.textmorph.showcase.catalog.Demo
import io.github.dim971.textmorph.showcase.shared.LocalShowcaseSettings
import io.github.dim971.textmorph.showcase.shared.Ticking
import io.github.dim971.textmorph.showcase.shared.showcaseColour

private val words = listOf("Text", "arrives", "one", "word", "at", "a", "time.")

/** Text arriving a word at a time, the way a model writes it. */
@Composable
fun StreamingDemo() {
    val settings = LocalShowcaseSettings.current
    var count by remember { mutableIntStateOf(1) }

    Ticking(
        intervalMs = 450,
        // Back to one word rather than none: an empty value is a state the
        // library handles, but it reads as a broken row in a list.
        advance = { count = if (count >= words.size) 1 else count + 1 },
    ) {
        TextMorph(
            text = words.take(count).joinToString(" "),
            options = settings.options,
            font = TextMorphFont(fontSize = 22.sp),
            colour = showcaseColour(),
        )
    }
}

val streamingDemo =
    Demo(
        id = "streaming",
        name = "Streaming",
        summary =
            "A value appended to, over and over. Each word arrives while the ones before it " +
                "hold still, which is the case a cross-fade cannot do at all.",
        capability = "morphs arriving faster than they settle",
        code =
            """
            // words arriving one at a time
            TextMorph(text = words.take(count).joinToString(" "))
            """.trimIndent(),
        content = { StreamingDemo() },
    )
