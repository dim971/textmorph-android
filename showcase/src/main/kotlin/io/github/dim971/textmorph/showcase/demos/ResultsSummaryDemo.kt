package io.github.dim971.textmorph.showcase.demos

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp
import io.github.dim971.textmorph.compose.TextMorph
import io.github.dim971.textmorph.showcase.catalog.Demo
import io.github.dim971.textmorph.showcase.shared.LocalShowcaseSettings
import io.github.dim971.textmorph.showcase.shared.ShowcaseSettings
import io.github.dim971.textmorph.showcase.shared.Stage
import io.github.dim971.textmorph.showcase.shared.rememberTicker
import io.github.dim971.textmorph.showcase.shared.showcaseColour
import io.github.dim971.textmorph.showcase.shared.stageFont

private val RESULTS = listOf(24 to 1208, 24 to 986, 12 to 986, 12 to 47)

private fun grouped(value: Int): String {
    val digits = value.toString()
    return digits
        .reversed()
        .chunked(3)
        .joinToString(",")
        .reversed()
}

/** A number inside a sentence. */
@Composable
fun ResultsSummaryDemo() {
    val settings = LocalShowcaseSettings.current
    val index = rememberTicker(RESULTS.size, 2000)
    val (shown, total) = RESULTS[index]

    Stage {
        TextMorph(
            text = "Showing $shown of ${grouped(total)} results",
            options = settings.optionsWith(ShowcaseSettings.UpstreamSpring),
            font = stageFont(size = 20.sp),
            colour = showcaseColour(),
        )
    }
}

val resultsSummaryDemo =
    Demo(
        id = "results",
        name = "Results summary",
        summary =
            "Two numbers buried in a sentence. The words hold completely still while the " +
                "counts roll, which is the case that makes the word path worth having: a cross-fade " +
                "here would flicker the whole line to change two digits.",
        capability = "quantities inside prose",
        code =
            """
            TextMorph(text = "Showing ${'$'}shown of ${'$'}total results")
            """.trimIndent(),
        content = { ResultsSummaryDemo() },
    )
