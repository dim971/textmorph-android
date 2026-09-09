package io.github.dim971.textmorph.showcase.demos

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import io.github.dim971.textmorph.compose.TextMorph
import io.github.dim971.textmorph.engine.TextMorphFont
import io.github.dim971.textmorph.showcase.catalog.Demo
import io.github.dim971.textmorph.showcase.shared.LocalShowcaseSettings
import io.github.dim971.textmorph.showcase.shared.Tappable
import io.github.dim971.textmorph.showcase.shared.rememberCycle
import io.github.dim971.textmorph.showcase.shared.showcaseColour

/** The plainest thing the library does. */
@Composable
fun HelloDemo() {
    val settings = LocalShowcaseSettings.current
    val cycle = rememberCycle("Hello world", "Hello there", "Goodbye world")

    Tappable(hint = "Tap to change the value", advance = cycle::advance) {
        TextMorph(
            text = cycle.current,
            options = settings.options,
            font = TextMorphFont(fontSize = 28.sp, fontWeight = FontWeight.Medium),
            colour = showcaseColour(),
        )
    }
}

val helloDemo =
    Demo(
        id = "hello",
        name = "Hello",
        summary =
            "A value changing, and nothing else. The words that survive the change move; " +
                "the ones that do not arrive and leave.",
        capability = "the word path of the segmenter",
        code =
            """
            TextMorph(
                text = greeting,
                font = TextMorphFont(fontSize = 28.sp, fontWeight = FontWeight.Medium),
            )
            """.trimIndent(),
        content = { HelloDemo() },
    )
