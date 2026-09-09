package io.github.dim971.textmorph.showcase.demos

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.dim971.textmorph.compose.TextMorph
import io.github.dim971.textmorph.engine.TextMorphFont
import io.github.dim971.textmorph.showcase.catalog.Demo
import io.github.dim971.textmorph.showcase.shared.LocalShowcaseSettings
import io.github.dim971.textmorph.showcase.shared.Tappable
import io.github.dim971.textmorph.showcase.shared.rememberCycle
import io.github.dim971.textmorph.showcase.shared.showcaseColour

/** A value with line breaks in it. */
@Composable
fun ReflowDemo() {
    val settings = LocalShowcaseSettings.current
    val cycle =
        rememberCycle(
            "Two lines\nof text",
            "Three lines\nof text\nthis time",
            "One line",
            "",
        )

    Tappable(hint = "Tap to gain or lose a line", advance = cycle::advance) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(130.dp),
            contentAlignment = Alignment.Center,
        ) {
            TextMorph(
                text = cycle.current,
                options = settings.options,
                font = TextMorphFont(fontSize = 24.sp),
                colour = showcaseColour(),
            )
        }
    }
}

val reflowDemo =
    Demo(
        id = "reflow",
        name = "Reflow",
        summary =
            "Line breaks are the only thing that makes a line here: there is no automatic " +
                "wrapping, deliberately, because wrapping would change which segments are adjacent and " +
                "so change the whole morph. The last step empties the value, which holds the old box " +
                "rather than collapsing it.",
        capability = "multi-line values, and the empty transition",
        code =
            """
            TextMorph(text = "Two lines\nof text")
            """.trimIndent(),
        content = { ReflowDemo() },
    )
