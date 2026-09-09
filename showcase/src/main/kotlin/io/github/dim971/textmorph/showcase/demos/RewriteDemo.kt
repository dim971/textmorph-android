package io.github.dim971.textmorph.showcase.demos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.dim971.textmorph.compose.TextMorph
import io.github.dim971.textmorph.showcase.catalog.Demo
import io.github.dim971.textmorph.showcase.shared.Caption
import io.github.dim971.textmorph.showcase.shared.LocalShowcaseSettings
import io.github.dim971.textmorph.showcase.shared.Stage
import io.github.dim971.textmorph.showcase.shared.rememberTicker
import io.github.dim971.textmorph.showcase.shared.showcaseColour
import io.github.dim971.textmorph.showcase.shared.stageFont
import io.github.dim971.textmorph.showcase.shared.wrap

private val TONES =
    listOf(
        "Direct" to "Running late, be there soon.",
        "Friendly" to "Running a bit behind, I'll be there soon.",
        "Professional" to "I'm running a little behind, should be there soon.",
    )

/** The same message, rewritten in another tone. */
@Composable
fun RewriteDemo() {
    val settings = LocalShowcaseSettings.current
    val index = rememberTicker(TONES.size, 2600)
    val (tone, body) = TONES[index]

    Stage {
        Box(
            Modifier
                .background(
                    MaterialTheme.colorScheme.surfaceContainerHighest,
                    RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 16.dp),
                ).padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            TextMorph(
                text = wrap(body, 26),
                options = settings.options,
                font = stageFont(size = 17.sp, weight = FontWeight.Normal),
                colour = showcaseColour(),
                textAlign = TextAlign.Start,
            )
        }
        TextMorph(
            text = tone,
            options = settings.options,
            font = stageFont(size = 14.sp),
            colour = MaterialTheme.colorScheme.primary,
        )
        Caption("tone")
    }
}

val rewriteDemo =
    Demo(
        id = "rewrite",
        name = "Rewrite",
        summary =
            "A message rewritten in another tone, with the tone's own name morphing under " +
                "it. Almost nothing survives between Direct and Professional, so most of the bubble " +
                "is a group replacement: six or more adjacent characters all leaving stop being " +
                "characters and collapse as one shape.",
        capability = "the group replacement path, over several lines",
        code =
            """
            TextMorph(text = wrap(body, 26))
            TextMorph(text = tone)
            """.trimIndent(),
        content = { RewriteDemo() },
    )
