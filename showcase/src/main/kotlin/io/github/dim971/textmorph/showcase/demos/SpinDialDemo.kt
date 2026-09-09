package io.github.dim971.textmorph.showcase.demos

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
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
import io.github.dim971.textmorph.showcase.shared.stageFont
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private const val DIAL_MAX = 500

/** Degrees of dial per unit of value, so the whole range is most of a turn. */
private const val STEP_DEG = 0.6f

private val PRESETS = listOf(75, 240, 18, 410)

/** A number on a dial, settling on a spring. */
@Composable
fun SpinDialDemo() {
    val settings = LocalShowcaseSettings.current
    val live = LocalInteractive.current
    var preset by remember { mutableIntStateOf(0) }
    var turned by remember { mutableFloatStateOf(Float.NaN) }
    val autoplay = rememberAutoplay(2600) { preset = (preset + 1) % PRESETS.size }

    val target = if (turned.isNaN()) PRESETS[preset].toFloat() else turned
    val shown by animateFloatAsState(target, label = "dial")
    val value = shown.roundToInt().coerceIn(0, DIAL_MAX)

    var last by remember { mutableFloatStateOf(0f) }
    val ink = MaterialTheme.colorScheme.onSurface
    // Visible against the card, which surfaceContainerHighest is not.
    val dim = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f)
    val tint = MaterialTheme.colorScheme.primary

    Stage {
        Box(
            modifier =
                Modifier
                    .size(180.dp)
                    .pointerInput(live) {
                        if (!live) return@pointerInput
                        detectDragGestures(
                            onDragStart = { start ->
                                autoplay.takeOver()
                                if (turned.isNaN()) turned = value.toFloat()
                                last =
                                    atan2(
                                        start.y - size.height / 2f,
                                        start.x - size.width / 2f,
                                    )
                            },
                        ) { change, _ ->
                            val angle =
                                atan2(
                                    change.position.y - size.height / 2f,
                                    change.position.x - size.width / 2f,
                                )
                            var delta = angle - last
                            // Across the seam, the short way round is the right way.
                            if (delta > PI) delta -= (2 * PI).toFloat()
                            if (delta < -PI) delta += (2 * PI).toFloat()
                            last = angle
                            val degrees = (delta * 180 / PI).toFloat()
                            turned = (turned + degrees / STEP_DEG).coerceIn(0f, DIAL_MAX.toFloat())
                        }
                    },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.size(180.dp)) {
                val radius = size.minDimension / 2 - 8f
                val centre = Offset(size.width / 2, size.height / 2)
                val ticks = 60
                val lit = (value.toFloat() / DIAL_MAX * ticks).roundToInt()
                for (i in 0 until ticks) {
                    // Starting at the bottom and going clockwise, so an empty
                    // dial reads as empty rather than as half full.
                    val a = (PI / 2 + (i.toDouble() / ticks) * 2 * PI).toFloat()
                    val inner = radius - if (i < lit) 16f else 10f
                    drawLine(
                        color = if (i < lit) tint else dim,
                        start = centre + Offset(cos(a) * inner, sin(a) * inner),
                        end = centre + Offset(cos(a) * radius, sin(a) * radius),
                        strokeWidth = 2.5f,
                        cap = StrokeCap.Round,
                    )
                }
            }
            TextMorph(
                text = "${'$'}$value",
                options = settings.optionsWith(ShowcaseSettings.UpstreamSpring),
                font = stageFont(size = 36.sp),
                colour = ink,
            )
        }
        Caption(if (autoplay.isPlaying) "drag around the dial" else "$value of $DIAL_MAX")
    }
}

val spinDialDemo =
    Demo(
        id = "spin",
        name = "Spin dial",
        summary =
            "Drag around the dial. The value is driven by an angle, so it arrives in a rush " +
                "and settles on a spring, and a spring is the one ease that ignores the duration " +
                "entirely: it takes as long as its own physics say. It also overshoots, which is why " +
                "a morph is allowed to draw outside its own box.",
        capability = "a spring, and the overshoot the drawing surface allows for",
        code =
            """
            TextMorph(
                text = money(value),
                options = TextMorphOptions(
                    ease = TextMorphEase.spring(stiffness = 150.0, damping = 19.0, mass = 1.2),
                ),
            )
            """.trimIndent(),
        content = { SpinDialDemo() },
    )
