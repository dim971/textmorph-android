package io.github.dim971.textmorph.showcase.demos

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import io.github.dim971.textmorph.compose.TextMorph
import io.github.dim971.textmorph.showcase.catalog.Demo
import io.github.dim971.textmorph.showcase.shared.LocalShowcaseSettings
import io.github.dim971.textmorph.showcase.shared.Stage
import io.github.dim971.textmorph.showcase.shared.rememberTicker
import io.github.dim971.textmorph.showcase.shared.showcaseColour
import io.github.dim971.textmorph.showcase.shared.stageFont

/**
 * Upstream cycles four package managers. A Gradle project has one, and four
 * configurations to declare a dependency in, so that is what cycles here: the
 * morph is the same shape, one word changing at the front of a monospace line.
 */
private val LINES =
    listOf(
        "implementation(libs.textmorph.compose)",
        "api(libs.textmorph.compose)",
        "debugImplementation(libs.textmorph.compose)",
        "testImplementation(libs.textmorph.compose)",
    )

/** A dependency line, rewritten. */
@Composable
fun InstallDemo() {
    val settings = LocalShowcaseSettings.current
    val index = rememberTicker(LINES.size, 1600)

    Stage {
        Row {
            Text(
                text = "> ",
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextMorph(
                text = LINES[index],
                options = settings.options,
                font = stageFont(size = 13.sp, mono = true),
                colour = showcaseColour(),
            )
        }
    }
}

val installDemo =
    Demo(
        id = "install",
        name = "Install",
        summary =
            "One word changes at the front of a monospace line and the rest holds still. " +
                "The plainest thing the word path does, and the one that shows a fixed-pitch face " +
                "keeps its grid through a morph.",
        capability = "the word path, at a fixed pitch",
        code =
            """
            TextMorph(
                text = line,
                font = TextMorphFont(fontFamily = FontFamily.Monospace, fontSize = 13.sp),
            )
            """.trimIndent(),
        content = { InstallDemo() },
    )
