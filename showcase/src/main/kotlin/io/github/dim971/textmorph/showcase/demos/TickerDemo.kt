package io.github.dim971.textmorph.showcase.demos

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.dim971.textmorph.compose.TextMorph
import io.github.dim971.textmorph.showcase.catalog.Demo
import io.github.dim971.textmorph.showcase.shared.Caption
import io.github.dim971.textmorph.showcase.shared.LocalShowcaseSettings
import io.github.dim971.textmorph.showcase.shared.ShowcaseSettings
import io.github.dim971.textmorph.showcase.shared.Stage
import io.github.dim971.textmorph.showcase.shared.rememberReducedMotion
import io.github.dim971.textmorph.showcase.shared.showcaseColour
import io.github.dim971.textmorph.showcase.shared.stageFont
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import kotlin.random.Random

private const val BASELINE = 7240.0

/** One slot's travel, which is both the scroll speed and the sampling rate. */
private const val SLOT_MS = 420L

private const val WINDOW = 20

private fun compact(value: Double): String =
    if (value >= 1000) {
        "${(value / 1000 * 10).roundToInt() / 10.0}K"
    } else {
        "${value.roundToInt()}"
    }

private fun percent(value: Double): String {
    val sign = if (value > 0) "+" else ""
    val rounded = (value * 10).roundToInt() / 10.0
    return "$sign$rounded%"
}

/** A live figure with its recent history drawn behind it. */
@Composable
fun TickerDemo() {
    val settings = LocalShowcaseSettings.current
    var series by remember { mutableStateOf(List(WINDOW) { BASELINE }) }
    var change by remember { mutableStateOf(12.4) }
    val still = rememberReducedMotion()

    LaunchedEffect(still) {
        if (still) return@LaunchedEffect
        var drift = 0.0
        while (true) {
            delay(SLOT_MS)
            drift = drift * 0.8 + (Random.nextDouble() - 0.5) * 1.6
            val next = (series.last() + drift * 90).coerceIn(4000.0, 11000.0)
            series = series.drop(1) + next
            change = change * 0.9 + drift
        }
    }

    val rising = change >= 0
    val tint = if (rising) Color(0xFF4ADE80) else Color(0xFFFF6B6B)

    Stage {
        Canvas(
            Modifier
                .width(260.dp)
                .height(56.dp),
        ) {
            val low = series.min()
            val high = series.max()
            val span = (high - low).takeIf { it > 1 } ?: 1.0
            val points =
                series.mapIndexed { i, v ->
                    Offset(
                        x = size.width * i / (series.size - 1f),
                        y = size.height * (1 - ((v - low) / span)).toFloat(),
                    )
                }
            for (i in 0 until points.size - 1) {
                drawLine(
                    color = tint.copy(alpha = 0.25f + 0.75f * i / points.size),
                    start = points[i],
                    end = points[i + 1],
                    strokeWidth = 2.5f,
                    cap = StrokeCap.Round,
                )
            }
        }
        Row(
            modifier = Modifier.width(260.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Column {
                TextMorph(
                    text = compact(series.last()),
                    options = settings.options,
                    font = stageFont(size = 26.sp),
                    colour = showcaseColour(),
                )
                Caption("requests / min")
            }
            Column(horizontalAlignment = Alignment.End) {
                TextMorph(
                    text = percent(change),
                    options = settings.optionsWith(ShowcaseSettings.UpstreamSpring),
                    font = stageFont(size = 26.sp),
                    colour = tint,
                )
                Caption("vs. last week")
            }
        }
    }
}

val tickerDemo =
    Demo(
        id = "ticker",
        name = "Ticker",
        summary =
            "A figure sampled twice a second, with its history drawn behind it and a change " +
                "that can turn negative. Two morphs on two different eases in one card: the count on " +
                "the default curve, the percentage on a spring, so the second visibly settles after " +
                "the first has arrived. Every sample interrupts the morph before it.",
        capability = "interruption, carried momentum, and two eases at once",
        code =
            """
            TextMorph(text = compact(requests))
            TextMorph(
                text = percent(change),
                options = TextMorphOptions(
                    ease = TextMorphEase.spring(stiffness = 150.0, damping = 19.0, mass = 1.2),
                ),
            )
            """.trimIndent(),
        content = { TickerDemo() },
    )
