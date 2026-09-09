package io.github.dim971.textmorph.showcase.demos

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import io.github.dim971.textmorph.compose.TextMorph
import io.github.dim971.textmorph.showcase.catalog.Demo
import io.github.dim971.textmorph.showcase.shared.BubbleTrack
import io.github.dim971.textmorph.showcase.shared.LocalInteractive
import io.github.dim971.textmorph.showcase.shared.LocalShowcaseSettings
import io.github.dim971.textmorph.showcase.shared.Stage
import io.github.dim971.textmorph.showcase.shared.rememberAutoplay
import io.github.dim971.textmorph.showcase.shared.stageFont
import kotlin.math.roundToInt

private val BRIGHTNESS = listOf(72, 18, 94, 41, 63)

/** A value in a pill that rides the thumb. */
@Composable
fun BubbleSliderDemo() {
    val settings = LocalShowcaseSettings.current
    var preset by remember { mutableIntStateOf(0) }
    var taken by remember { mutableFloatStateOf(Float.NaN) }
    val autoplay = rememberAutoplay(1700) { preset = (preset + 1) % BRIGHTNESS.size }

    // Autoplay glides to the next preset rather than cutting to it, so the pill
    // is carried by the travel and the digits roll on the way. A drag is
    // immediate: a tween between the finger and the thumb reads as the thumb
    // refusing to keep up, which is exactly what it is.
    val glided by animateFloatAsState(BRIGHTNESS[preset] / 100f, tween(700), label = "preset")
    val fraction = if (taken.isNaN()) glided else taken
    val value = (fraction * 100).roundToInt()

    Stage(caption = if (autoplay.isPlaying) "drag the thumb" else "brightness") {
        BubbleTrack(
            fraction = fraction,
            onFraction =
                if (!LocalInteractive.current) {
                    null
                } else {
                    { next ->
                        autoplay.takeOver()
                        taken = next
                    }
                },
        ) {
            TextMorph(
                text = "$value%",
                options = settings.options,
                font = stageFont(size = 20.sp),
                colour = Color.Black.copy(alpha = 0.9f),
            )
        }
    }
}

val bubbleSliderDemo =
    Demo(
        id = "bubble",
        name = "Bubble slider",
        summary =
            "The value rides the thumb on a spring, so it trails the travel and leans " +
                "into it, and it is morphing inside a box that is itself " +
                "travelling. Those are two different motions and the library only owns one of them: " +
                "the pill's journey is the layout's, and the digits rolling inside it are the morph's. " +
                "Drag it slowly and the two come apart; drag it fast and the digits are still catching " +
                "up when the thumb arrives.",
        capability = "a morph inside a moving container",
        code =
            """
            BubbleTrack(fraction = value / 100f) {
                TextMorph(text = "${'$'}value%")
            }
            """.trimIndent(),
        content = { BubbleSliderDemo() },
    )
