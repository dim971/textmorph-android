package io.github.dim971.textmorph.showcase.demos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import kotlin.math.roundToInt

/** How long a run takes, and the beat either side of it. Upstream's numbers. */
private const val RUN_MS = 2000f
private const val IDLE_MS = 900L
private const val DONE_MS = 1600L

private val BAR_WIDTH = 220.dp

/** A label that counts, then stops counting. */
@Composable
fun DownloadDemo() {
    val settings = LocalShowcaseSettings.current
    var progress by remember { mutableFloatStateOf(-1f) }
    val still = rememberReducedMotion()

    LaunchedEffect(still) {
        if (still) return@LaunchedEffect
        while (true) {
            progress = -1f
            delay(IDLE_MS)
            val started = System.currentTimeMillis()
            while (true) {
                val t = ((System.currentTimeMillis() - started) / RUN_MS).coerceAtMost(1f)
                // Accelerating rather than linear, so the percentages arrive
                // slowly at first and the morph has time to be seen.
                progress = t * t
                if (t >= 1f) break
                delay(100)
            }
            delay(DONE_MS)
        }
    }

    val label =
        when {
            progress < 0f -> "Download"
            progress >= 1f -> "Downloaded"
            else -> "${(progress * 100).roundToInt()}%"
        }
    val filled = progress.coerceIn(0f, 1f)

    Stage {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            TextMorph(
                text = label,
                options = settings.options,
                font = stageFont(size = 26.sp),
                colour = showcaseColour(),
            )
            Box(
                Modifier
                    .width(BAR_WIDTH)
                    .height(6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            ) {
                Box(
                    Modifier
                        .width(BAR_WIDTH * filled)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.primary),
                )
            }
        }
        Caption("a word, then a count, then a word")
    }
}

val downloadDemo =
    Demo(
        id = "download",
        name = "Download",
        summary =
            "Download, then a percentage counting up, then Downloaded. The interesting part " +
                "is the two boundaries: a word becoming a number has nothing in common with it, so " +
                "that is a replacement, while the numbers between roll by place value. Three kinds of " +
                "morph in one four-second loop, and the label never jumps width because the container " +
                "carries it.",
        capability = "a word, a quantity and a word again, in one place",
        code =
            """
            TextMorph(
                text = when {
                    idle -> "Download"
                    done -> "Downloaded"
                    else -> "${'$'}{percent}%"
                },
            )
            """.trimIndent(),
        content = { DownloadDemo() },
    )
