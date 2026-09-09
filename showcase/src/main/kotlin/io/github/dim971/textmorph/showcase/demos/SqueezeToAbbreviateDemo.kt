package io.github.dim971.textmorph.showcase.demos

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import io.github.dim971.textmorph.compose.TextMorph
import io.github.dim971.textmorph.showcase.catalog.Demo
import io.github.dim971.textmorph.showcase.shared.LocalShowcaseSettings
import io.github.dim971.textmorph.showcase.shared.Stage
import io.github.dim971.textmorph.showcase.shared.rememberTicker
import io.github.dim971.textmorph.showcase.shared.showcaseColour
import io.github.dim971.textmorph.showcase.shared.stageFont

private val PHRASES =
    listOf(
        "3 hours 24 minutes ago",
        "3 hr 24 min ago",
        "3h 24m ago",
        "3h ago",
        "now",
    )

/** A phrase abbreviating as the room runs out. */
@Composable
fun SqueezeToAbbreviateDemo() {
    val settings = LocalShowcaseSettings.current
    val index = rememberTicker(PHRASES.size, 3000)

    Stage {
        TextMorph(
            text = PHRASES[index],
            options = settings.options,
            font = stageFont(size = 24.sp, weight = FontWeight.Normal),
            colour = showcaseColour(),
        )
    }
}

val squeezeToAbbreviateDemo =
    Demo(
        id = "squeeze",
        name = "Squeeze to abbreviate",
        summary =
            "The same timestamp in five lengths, from a full sentence down to one word. " +
                "Every step keeps the digits and drops the words around them, so the 3 and the 24 " +
                "hold their places while hours becomes hr becomes h. The last step keeps nothing at " +
                "all, and that is where the group replacement takes over.",
        capability = "words shrinking around quantities that stay put",
        code =
            """
            TextMorph(text = phrases[step])
            """.trimIndent(),
        content = { SqueezeToAbbreviateDemo() },
    )
