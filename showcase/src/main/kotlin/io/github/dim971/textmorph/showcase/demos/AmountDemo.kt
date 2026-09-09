package io.github.dim971.textmorph.showcase.demos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.dim971.textmorph.compose.TextMorph
import io.github.dim971.textmorph.showcase.catalog.Demo
import io.github.dim971.textmorph.showcase.shared.Caption
import io.github.dim971.textmorph.showcase.shared.LocalShowcaseSettings
import io.github.dim971.textmorph.showcase.shared.Stage
import io.github.dim971.textmorph.showcase.shared.rememberReducedMotion
import io.github.dim971.textmorph.showcase.shared.showcaseColour
import io.github.dim971.textmorph.showcase.shared.stageFont
import kotlinx.coroutines.delay

/**
 * Upstream's script, step for step, including the delays.
 *
 * It is worth reading as a story: type twenty dollars, put the caret back and
 * insert a four, watch it become four thousand and twenty, then put a point in
 * and watch the same digits become four dollars twenty. The same five characters
 * mean three different amounts, and the caret is the only thing telling the
 * morph which reading is happening.
 */
private val SCRIPT =
    listOf(
        Triple("${'$'}", 1, 0L),
        Triple("${'$'}2", 2, 150L),
        Triple("${'$'}20", 3, 1200L),
        Triple("${'$'}20", 3, 1800L),
        Triple("${'$'}20", 1, 200L),
        Triple("${'$'}420", 2, 400L),
        Triple("${'$'}4,020", 4, 1800L),
        Triple("${'$'}420", 2, 400L),
        Triple("${'$'}4.20", 3, 400L),
        Triple("${'$'}4.20", 3, 1800L),
        Triple("${'$'}4.20", 5, 1800L),
        Triple("${'$'}4.2", 4, 200L),
        Triple("${'$'}4", 2, 200L),
        Triple("${'$'}", 1, 200L),
    )

/** A scripted edit, so the caret can be watched doing its work. */
@Composable
fun AmountDemo() {
    val settings = LocalShowcaseSettings.current
    var step by remember { mutableIntStateOf(0) }
    val still = rememberReducedMotion()

    LaunchedEffect(step, still) {
        if (still) return@LaunchedEffect
        delay(SCRIPT[step].third.coerceAtLeast(1))
        step = (step + 1) % SCRIPT.size
    }

    val (value, caret, _) = SCRIPT[step]

    Stage {
        Box(
            modifier =
                Modifier
                    .background(
                        MaterialTheme.colorScheme.surfaceContainerHighest,
                        RoundedCornerShape(12.dp),
                    ).padding(horizontal = 20.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextMorph(
                    text = value,
                    options = settings.options,
                    font = stageFont(size = 34.sp),
                    colour = showcaseColour(),
                    cursorIndex = caret,
                )
                Box(
                    Modifier
                        .padding(start = 2.dp)
                        .width(2.dp)
                        .height(34.dp)
                        .background(MaterialTheme.colorScheme.primary),
                )
            }
        }
        Caption("a scripted edit, caret and all")
    }
}

val amountDemo =
    Demo(
        id = "amount",
        name = "Amount",
        summary =
            "The same five characters read three ways. Twenty dollars becomes four thousand " +
                "and twenty because a digit went in front of the two, then four dollars twenty because " +
                "a point went between them. Nothing but the caret tells the morph which of those " +
                "happened, and the digits move completely differently in each case.",
        capability = "the caret deciding what a change means",
        code =
            """
            TextMorph(text = "${'$'}4,020", cursorIndex = 4)   // a digit was inserted
            TextMorph(text = "${'$'}4.20", cursorIndex = 3)    // a point was
            """.trimIndent(),
        content = { AmountDemo() },
    )
