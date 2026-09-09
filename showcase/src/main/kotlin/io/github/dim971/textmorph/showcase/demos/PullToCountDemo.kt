package io.github.dim971.textmorph.showcase.demos

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
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
import io.github.dim971.textmorph.showcase.shared.Stage
import io.github.dim971.textmorph.showcase.shared.rememberAutoplay
import io.github.dim971.textmorph.showcase.shared.showcaseColour
import io.github.dim971.textmorph.showcase.shared.stageFont
import kotlinx.coroutines.delay
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlin.math.sign

private const val PULL_MIN = -199
private const val PULL_MAX = 999

/** px of travel before the rubber band saturates. Upstream's number. */
private const val PULL_LIMIT = 74f

/** Units per second at a full pull. Upstream's number. */
private const val PULL_RATE = 30f

/** U+2212 rather than a hyphen, so a negative reading is the width of a positive one. */
private fun signed(value: Int): String = if (value < 0) "\u2212${value.absoluteValue}" else "$value"

/** A counter driven by how far it is pulled. */
@Composable
fun PullToCountDemo() {
    val settings = LocalShowcaseSettings.current
    val live = LocalInteractive.current
    var value by remember { mutableIntStateOf(0) }
    var pull by remember { mutableFloatStateOf(0f) }
    var held by remember { mutableStateOf(false) }
    val autoplay =
        rememberAutoplay(2000) {
            value = (value + 137).let { if (it > PULL_MAX) PULL_MIN + 40 else it }
        }
    val offset by animateFloatAsState(if (held) pull else 0f, label = "pull")

    // While it is held, the count runs at a rate set by the pull. That is the
    // whole gesture: a position becomes a speed.
    LaunchedEffect(held) {
        while (held) {
            delay(16)
            val rate = (pull / PULL_LIMIT).coerceIn(-1f, 1f) * PULL_RATE
            value = (value + (rate * 0.016f).roundToInt()).coerceIn(PULL_MIN, PULL_MAX)
        }
    }

    Stage {
        Box(
            modifier =
                Modifier
                    .size(width = 200.dp, height = 120.dp)
                    .pointerInput(live) {
                        if (!live) return@pointerInput
                        detectDragGestures(
                            onDragStart = {
                                autoplay.takeOver()
                                held = true
                            },
                            onDragEnd = {
                                held = false
                                pull = 0f
                            },
                            onDragCancel = {
                                held = false
                                pull = 0f
                            },
                        ) { _, delta ->
                            // Rubber band: the last few pixels cost more than the
                            // first, so a full pull is a decision rather than a slip.
                            val next = pull + delta.y
                            pull = sign(next) * minOf(next.absoluteValue, PULL_LIMIT * 1.4f)
                        }
                    },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .offset(y = (offset * 0.5f).dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceContainerHighest,
                        RoundedCornerShape(14.dp),
                    ).padding(horizontal = 22.dp, vertical = 12.dp),
            ) {
                TextMorph(
                    text = signed(value),
                    options = settings.options,
                    font = stageFont(size = 34.sp),
                    colour = showcaseColour(),
                )
            }
        }
        Caption(if (autoplay.isPlaying) "pull it up or down" else "counting")
    }
}

val pullToCountDemo =
    Demo(
        id = "pull",
        name = "Pull to count",
        summary =
            "Pull the card and the count runs, faster the further you pull. It crosses zero " +
                "into negatives, and the sign is U+2212 rather than a hyphen so the reading does not " +
                "shift sideways when it turns. Sixty morphs a second, each interrupting the last, " +
                "which is the same pressure the Earned card puts on the engine and a different way " +
                "of applying it.",
        capability = "a gesture driving a morph faster than it settles",
        code =
            """
            TextMorph(text = signed(count))   // U+2212 for a minus, not a hyphen
            """.trimIndent(),
        content = { PullToCountDemo() },
    )
