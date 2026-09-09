package io.github.dim971.textmorph.showcase.demos

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.dim971.textmorph.compose.TextMorph
import io.github.dim971.textmorph.showcase.catalog.Demo
import io.github.dim971.textmorph.showcase.shared.Caption
import io.github.dim971.textmorph.showcase.shared.LocalShowcaseSettings
import io.github.dim971.textmorph.showcase.shared.Stage
import io.github.dim971.textmorph.showcase.shared.onTint
import io.github.dim971.textmorph.showcase.shared.rememberAutoplay
import io.github.dim971.textmorph.showcase.shared.showcaseColour
import io.github.dim971.textmorph.showcase.shared.stageFont
import kotlin.math.roundToInt

private val LEVELS = listOf(0.72f, 0.52f, 0.95f, 0.46f)

/** A tank filling and emptying, with its level written across it. */
@Composable
fun SloshGaugeDemo() {
    val settings = LocalShowcaseSettings.current
    var step by remember { mutableIntStateOf(0) }
    rememberAutoplay(2600) { step = (step + 1) % LEVELS.size }
    val level by animateFloatAsState(
        LEVELS[step],
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 90f),
        label = "level",
    )
    val value = "${(level * 100).roundToInt()}%"
    val liquid = settings.tint.colour

    Stage {
        Box(
            modifier =
                Modifier
                    .width(150.dp)
                    .height(96.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center,
        ) {
            // The liquid, filling from the bottom.
            Box(
                Modifier
                    .matchParentSize()
                    .drawWithContent {
                        drawRect(
                            color = liquid,
                            topLeft =
                                androidx.compose.ui.geometry.Offset(
                                    0f,
                                    size.height * (1 - level),
                                ),
                            size =
                                androidx.compose.ui.geometry.Size(
                                    size.width,
                                    size.height * level,
                                ),
                        )
                    },
            )
            // The value twice: once in the ink colour, and once in a dark one
            // clipped to the liquid, so the digits under the surface read
            // against it rather than through it. Upstream clips the same value
            // to the same surface.
            TextMorph(
                text = value,
                options = settings.options,
                font = stageFont(size = 30.sp),
                colour = showcaseColour(),
            )
            Box(
                Modifier
                    .matchParentSize()
                    .drawWithContent {
                        clipRect(top = size.height * (1 - level)) { this@drawWithContent.drawContent() }
                    },
                contentAlignment = Alignment.Center,
            ) {
                TextMorph(
                    text = value,
                    options = settings.options,
                    font = stageFont(size = 30.sp),
                    colour = onTint(liquid),
                )
            }
        }
        Caption("level")
    }
}

val sloshGaugeDemo =
    Demo(
        id = "slosh",
        name = "Slosh gauge",
        summary =
            "A level written across the thing it measures. The same value is drawn twice, " +
                "once in the ink colour and once in a dark one clipped to the liquid, so the digits " +
                "under the surface read against it. Two morphs of the same value, in step because " +
                "they are given the same value at the same moment rather than because anything " +
                "synchronises them.",
        capability = "two morphs of one value, layered",
        code =
            """
            TextMorph(text = level)                       // above the surface
            Box(Modifier.drawWithContent { clipRect(top = surface) { drawContent() } }) {
                TextMorph(text = level, colour = Color.Black)   // below it
            }
            """.trimIndent(),
        content = { SloshGaugeDemo() },
    )
