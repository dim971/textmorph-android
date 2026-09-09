package io.github.dim971.textmorph.showcase.demos

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.dim971.textmorph.compose.TextMorph
import io.github.dim971.textmorph.showcase.catalog.Demo
import io.github.dim971.textmorph.showcase.shared.Caption
import io.github.dim971.textmorph.showcase.shared.LocalShowcaseSettings
import io.github.dim971.textmorph.showcase.shared.Stage
import io.github.dim971.textmorph.showcase.shared.rememberAutoplay
import io.github.dim971.textmorph.showcase.shared.showcaseColour
import io.github.dim971.textmorph.showcase.shared.stageFont
import kotlin.math.roundToInt

private const val BUDGET = 1000
private val SPLITS = listOf(0.64f, 0.28f, 0.5f, 0.83f)

private fun grouped(value: Int): String =
    value
        .toString()
        .reversed()
        .chunked(3)
        .joinToString(",")
        .reversed()

/** One budget, divided two ways. */
@Composable
fun SplitBarDemo() {
    val settings = LocalShowcaseSettings.current
    var step by remember { mutableIntStateOf(0) }
    rememberAutoplay(2600) { step = (step + 1) % SPLITS.size }
    val share by animateFloatAsState(SPLITS[step], label = "share")

    val left = (BUDGET * share).roundToInt()
    val right = BUDGET - left

    Stage {
        Row(
            modifier =
                Modifier
                    .width(260.dp)
                    .height(56.dp)
                    .clip(RoundedCornerShape(12.dp)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .weight(share.coerceIn(0.08f, 0.92f))
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                TextMorph(
                    text = "${'$'}${grouped(left)}",
                    options = settings.options,
                    font = stageFont(size = 20.sp),
                    colour = Color.Black.copy(alpha = 0.85f),
                )
            }
            Box(
                Modifier
                    .weight((1 - share).coerceIn(0.08f, 0.92f))
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                TextMorph(
                    text = "${'$'}${grouped(right)}",
                    options = settings.options,
                    font = stageFont(size = 20.sp),
                    colour = showcaseColour(),
                )
            }
        }
        Caption("one budget, two shares")
    }
}

val splitBarDemo =
    Demo(
        id = "split",
        name = "Split bar",
        summary =
            "A thousand divided between two shares that always add up. Both halves change at " +
                "once and in opposite directions, so one gains a column exactly as the other loses " +
                "one, and each rolls by its own place values inside a box whose width is moving under " +
                "it. That last part is the interesting bit: the morph is measured against the box it " +
                "is settling into, not the one it started in.",
        capability = "place value inside a container that is itself resizing",
        code =
            """
            TextMorph(text = money(left))
            TextMorph(text = money(budget - left))
            """.trimIndent(),
        content = { SplitBarDemo() },
    )
