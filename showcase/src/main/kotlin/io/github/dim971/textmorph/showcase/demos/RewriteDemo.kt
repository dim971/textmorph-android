package io.github.dim971.textmorph.showcase.demos

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp
import io.github.dim971.textmorph.compose.TextMorph
import io.github.dim971.textmorph.engine.TextMorphFont
import io.github.dim971.textmorph.showcase.catalog.Demo
import io.github.dim971.textmorph.showcase.shared.LocalShowcaseSettings
import io.github.dim971.textmorph.showcase.shared.Tappable
import io.github.dim971.textmorph.showcase.shared.rememberCycle
import io.github.dim971.textmorph.showcase.shared.showcaseColour

/** A sentence replaced outright. */
@Composable
fun RewriteDemo() {
    val settings = LocalShowcaseSettings.current
    val cycle =
        rememberCycle(
            "the quick brown fox",
            "a slow green turtle",
            "one lazy grey cat",
        )

    Tappable(hint = "Tap to rewrite", advance = cycle::advance) {
        TextMorph(
            text = cycle.current,
            options = settings.options,
            font = TextMorphFont(fontSize = 24.sp),
            colour = showcaseColour(),
        )
    }
}

val rewriteDemo =
    Demo(
        id = "rewrite",
        name = "Rewrite",
        summary =
            "Nothing survives, so the old sentence recedes as one shape rather than as " +
                "twenty characters each going its own way. Six adjacent characters all leaving is " +
                "where that switch happens.",
        capability = "the group replacement path",
        code =
            """
            TextMorph(text = sentence, font = TextMorphFont(fontSize = 24.sp))
            """.trimIndent(),
        content = { RewriteDemo() },
    )
