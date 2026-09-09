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

/** A row of labels that reorders. */
@Composable
fun FiltersDemo() {
    val settings = LocalShowcaseSettings.current
    val cycle =
        rememberCycle(
            "Unread  Flagged  Recent",
            "Recent  Unread  Flagged",
            "Flagged  Recent  Unread",
        )

    Tappable(hint = "Tap to reorder", advance = cycle::advance) {
        TextMorph(
            text = cycle.current,
            options = settings.options,
            font = TextMorphFont(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
            colour = showcaseColour(),
        )
    }
}

val filtersDemo =
    Demo(
        id = "filters",
        name = "Filters",
        summary =
            "The same words in a different order. A word that moved keeps its identity and " +
                "travels whole, rather than being cut into characters that fly separately. A " +
                "subsequence cannot see a reordering, so a second pass looks for it.",
        capability = "the exact-match reordering pass of the diff",
        code =
            """
            TextMorph(text = filters.joinToString("  "))
            """.trimIndent(),
        content = { FiltersDemo() },
    )
