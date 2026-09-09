package io.github.dim971.textmorph.showcase.demos

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.dim971.textmorph.compose.TextMorph
import io.github.dim971.textmorph.showcase.catalog.Demo
import io.github.dim971.textmorph.showcase.shared.Caption
import io.github.dim971.textmorph.showcase.shared.LocalInteractive
import io.github.dim971.textmorph.showcase.shared.LocalShowcaseSettings
import io.github.dim971.textmorph.showcase.shared.ShowcaseSettings
import io.github.dim971.textmorph.showcase.shared.Stage
import io.github.dim971.textmorph.showcase.shared.rememberAutoplay
import io.github.dim971.textmorph.showcase.shared.showcaseColour
import io.github.dim971.textmorph.showcase.shared.stageFont
import kotlin.math.roundToInt

private val MONTHS =
    listOf(
        "January" to 4120,
        "February" to 3840,
        "March" to 5230,
        "April" to 4780,
        "May" to 6150,
        "June" to 5890,
        "July" to 7240,
        "August" to 6870,
        "September" to 7590,
        "October" to 8120,
        "November" to 7430,
        "December" to 9210,
    )

private val PEAK = MONTHS.maxOf { it.second }

private fun grouped(value: Int): String =
    value
        .toString()
        .reversed()
        .chunked(3)
        .joinToString(",")
        .reversed()

/** A figure scrubbed out of a chart. */
@Composable
fun ChartDemo() {
    val settings = LocalShowcaseSettings.current
    val live = LocalInteractive.current
    var month by remember { mutableIntStateOf(4) }
    val autoplay = rememberAutoplay(1300) { month = (month + 1) % MONTHS.size }
    val (name, value) = MONTHS[month]

    Stage {
        TextMorph(
            text = "${'$'}${grouped(value)}",
            options = settings.optionsWith(ShowcaseSettings.UpstreamSpring),
            font = stageFont(size = 34.sp),
            colour = showcaseColour(),
        )
        // A hundred milliseconds, because the label is following a finger and
        // anything slower reads as lag rather than as motion. Upstream's number.
        TextMorph(
            text = "$name revenue",
            options = settings.options.copy(duration = 100.0),
            font = stageFont(size = 14.sp),
            colour = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier =
                Modifier
                    .width(260.dp)
                    .height(72.dp)
                    .pointerInput(MONTHS.size, live) {
                        if (!live) return@pointerInput
                        detectDragGestures(onDragStart = { autoplay.takeOver() }) { change, _ ->
                            val slot = (change.position.x / size.width * MONTHS.size).roundToInt()
                            month = slot.coerceIn(0, MONTHS.size - 1)
                        }
                    },
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            for ((index, entry) in MONTHS.withIndex()) {
                Box(
                    Modifier
                        .weight(1f)
                        .height(72.dp * entry.second / PEAK)
                        .background(
                            if (index == month) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHighest
                            },
                            RoundedCornerShape(3.dp),
                        ),
                )
            }
        }
        Caption(if (autoplay.isPlaying) "drag across the bars" else "scrubbing")
    }
}

val chartDemo =
    Demo(
        id = "chart",
        name = "Chart",
        summary =
            "Drag across the bars. Two morphs at two speeds: the amount on a spring, which " +
                "settles, and the month on a hundred milliseconds, which has to keep up with a finger. " +
                "That difference is the point of the card. A label following a gesture on a four " +
                "hundred millisecond morph reads as lag; the amount on the same duration reads as " +
                "haste.",
        capability = "two durations chosen for two jobs",
        code =
            """
            TextMorph(text = amount, options = TextMorphOptions(ease = spring))
            TextMorph(text = "${'$'}name revenue", options = TextMorphOptions(duration = 100.0))
            """.trimIndent(),
        content = { ChartDemo() },
    )
