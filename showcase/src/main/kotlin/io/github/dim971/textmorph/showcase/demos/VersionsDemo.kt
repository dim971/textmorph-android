package io.github.dim971.textmorph.showcase.demos

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp
import io.github.dim971.textmorph.compose.TextMorph
import io.github.dim971.textmorph.showcase.catalog.Demo
import io.github.dim971.textmorph.showcase.shared.LocalShowcaseSettings
import io.github.dim971.textmorph.showcase.shared.Stage
import io.github.dim971.textmorph.showcase.shared.rememberTicker
import io.github.dim971.textmorph.showcase.shared.showcaseColour
import io.github.dim971.textmorph.showcase.shared.stageFont

private val VERSIONS = listOf("v1.2.3", "v1.3.0", "v2.0.0", "v2.0.1")

/** A version number, which is not a quantity. */
@Composable
fun VersionsDemo() {
    val settings = LocalShowcaseSettings.current
    val index = rememberTicker(VERSIONS.size, 1400)

    Stage {
        TextMorph(
            text = VERSIONS[index],
            options = settings.options,
            font = stageFont(size = 30.sp, mono = true),
            colour = showcaseColour(),
        )
    }
}

val versionsDemo =
    Demo(
        id = "versions",
        name = "Versions",
        summary =
            "A version is not a quantity, and the library knows it: a token has to start and " +
                "end with a digit and hold nothing but digits and separators to morph by place value. " +
                "So this morphs character by character, the way a date or a product code should.",
        capability = "the strictness of the numeric-word test",
        code =
            """
            // "v1.2.3" is not a quantity, so it morphs per character
            TextMorph(text = version)
            """.trimIndent(),
        content = { VersionsDemo() },
    )
