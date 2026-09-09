package io.github.dim971.textmorph.showcase.demos

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.dim971.textmorph.compose.TextMorph
import io.github.dim971.textmorph.showcase.catalog.Demo
import io.github.dim971.textmorph.showcase.shared.Caption
import io.github.dim971.textmorph.showcase.shared.LocalInteractive
import io.github.dim971.textmorph.showcase.shared.LocalShowcaseSettings
import io.github.dim971.textmorph.showcase.shared.Stage
import io.github.dim971.textmorph.showcase.shared.rememberAutoplay
import io.github.dim971.textmorph.showcase.shared.showcaseColour
import io.github.dim971.textmorph.showcase.shared.stageFont
import io.github.dim971.textmorph.showcase.shared.wrap
import kotlin.math.roundToInt

private const val BODY = "Drag the handle to rewrap this sentence."

private const val MIN_DP = 68f
private const val MAX_DP = 208f

/** Where autoplay parks the handle, until someone takes it. */
private val STOPS = listOf(MAX_DP, 140f, MIN_DP, 140f)

/**
 * How many characters fit at a width.
 *
 * A rough conversion rather than a measurement, and deliberately: the demo is
 * about a value that gains and loses lines, not about a text engine. Roughly six
 * and a half density-independent pixels to a character at this size.
 */
private fun charsAt(widthDp: Float): Int = (widthDp / 6.5f).roundToInt().coerceAtLeast(4)

/** A sentence that rewraps as its column narrows. */
@Composable
fun ResizeDemo() {
    val settings = LocalShowcaseSettings.current
    val live = LocalInteractive.current
    var stop by remember { mutableFloatStateOf(0f) }
    var dragged by remember { mutableFloatStateOf(Float.NaN) }
    val autoplay = rememberAutoplay(2600) { stop = (stop + 1) % STOPS.size }
    val density = LocalDensity.current

    val target = if (dragged.isNaN()) STOPS[stop.toInt()] else dragged
    val widthDp by animateFloatAsState(target, label = "width")

    Stage {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                Modifier
                    .width(widthDp.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceContainerHighest,
                        RoundedCornerShape(10.dp),
                    ).padding(10.dp),
            ) {
                TextMorph(
                    text = wrap(BODY, charsAt(widthDp)),
                    options = settings.options,
                    font = stageFont(size = 15.sp, weight = FontWeight.Normal),
                    colour = showcaseColour(),
                    textAlign = TextAlign.Start,
                )
            }
            // The handle. Dragging it takes the column over from autoplay, which
            // is upstream's rule everywhere: a demo hands over rather than
            // fighting the finger.
            Box(
                Modifier
                    .size(width = 10.dp, height = 44.dp)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(5.dp))
                    .pointerInput(live) {
                        if (!live) return@pointerInput
                        detectDragGestures(
                            onDragStart = {
                                autoplay.takeOver()
                                if (dragged.isNaN()) dragged = widthDp
                            },
                        ) { _, delta ->
                            val step = with(density) { delta.x.toDp().value }
                            dragged = (dragged + step).coerceIn(MIN_DP, MAX_DP)
                        }
                    },
            )
        }
        Caption(if (autoplay.isPlaying) "drag the handle" else "${widthDp.roundToInt()}dp")
    }
}

val resizeDemo =
    Demo(
        id = "resize",
        name = "Resize",
        summary =
            "Drag the handle. The sentence rewraps, so words move between lines, and a word " +
                "that changes line travels diagonally to its new place instead of disappearing from " +
                "one row and appearing on another. There is no automatic wrapping in the library: the " +
                "demo computes the breaks and passes them in, which is exactly why the morph can see " +
                "that a word survived.",
        capability = "a value gaining and losing lines",
        code =
            """
            // the library never wraps, so the caller decides where the lines go
            TextMorph(text = wrap(sentence, charsAt(width)))
            """.trimIndent(),
        content = { ResizeDemo() },
    )
