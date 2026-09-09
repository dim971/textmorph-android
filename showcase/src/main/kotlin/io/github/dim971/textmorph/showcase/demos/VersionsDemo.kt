package io.github.dim971.textmorph.showcase.demos

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import io.github.dim971.textmorph.compose.TextMorph
import io.github.dim971.textmorph.engine.TextMorphFont
import io.github.dim971.textmorph.showcase.catalog.Demo
import io.github.dim971.textmorph.showcase.shared.LocalShowcaseSettings
import io.github.dim971.textmorph.showcase.shared.Tappable
import io.github.dim971.textmorph.showcase.shared.rememberCycle
import io.github.dim971.textmorph.showcase.shared.showcaseColour

/** A version number, which is not a quantity. */
@Composable
fun VersionsDemo() {
    val settings = LocalShowcaseSettings.current
    val cycle = rememberCycle("v1.4.2", "v1.5.0", "v2.0.0", "v2.0.1")

    Tappable(hint = "Tap to bump the version", advance = cycle::advance) {
        TextMorph(
            text = cycle.current,
            options = settings.options,
            font =
                TextMorphFont(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
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
            // "v1.4.2" is not a quantity, so it morphs per character
            TextMorph(text = version)
            """.trimIndent(),
        content = { VersionsDemo() },
    )
