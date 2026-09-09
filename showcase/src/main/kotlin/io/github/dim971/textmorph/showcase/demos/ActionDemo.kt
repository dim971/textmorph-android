package io.github.dim971.textmorph.showcase.demos

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.dim971.textmorph.compose.TextMorph
import io.github.dim971.textmorph.showcase.catalog.Demo
import io.github.dim971.textmorph.showcase.shared.Chip
import io.github.dim971.textmorph.showcase.shared.LocalShowcaseSettings
import io.github.dim971.textmorph.showcase.shared.ShowcaseSettings
import io.github.dim971.textmorph.showcase.shared.Stage
import io.github.dim971.textmorph.showcase.shared.rememberTicker
import io.github.dim971.textmorph.showcase.shared.showcaseColour
import io.github.dim971.textmorph.showcase.shared.stageFont

private val STATES = listOf("Processing Transaction", "Transaction Safe")

/** A status that resolves, with the icon resolving beside it. */
@Composable
fun ActionDemo() {
    val settings = LocalShowcaseSettings.current
    val index = rememberTicker(STATES.size, 2000)
    val spin by rememberInfiniteTransition(label = "spinner").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(500, easing = LinearEasing), RepeatMode.Restart),
        label = "angle",
    )
    val tint = MaterialTheme.colorScheme.primary

    Stage {
        Chip {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Canvas(Modifier.size(20.dp)) {
                    val stroke = Stroke(width = size.minDimension * 0.16f, cap = StrokeCap.Round)
                    if (index == 0) {
                        drawCircle(tint.copy(alpha = 0.15f), style = stroke)
                        drawArc(
                            color = tint,
                            startAngle = spin,
                            sweepAngle = 90f,
                            useCenter = false,
                            style = stroke,
                        )
                    } else {
                        drawCircle(tint)
                        val w = size.minDimension
                        drawLine(
                            Color.Black.copy(alpha = 0.85f),
                            Offset(w * 0.28f, w * 0.52f),
                            Offset(w * 0.44f, w * 0.68f),
                            strokeWidth = w * 0.13f,
                            cap = StrokeCap.Round,
                        )
                        drawLine(
                            Color.Black.copy(alpha = 0.85f),
                            Offset(w * 0.44f, w * 0.68f),
                            Offset(w * 0.72f, w * 0.36f),
                            strokeWidth = w * 0.13f,
                            cap = StrokeCap.Round,
                        )
                    }
                }
                TextMorph(
                    text = STATES[index],
                    options =
                        settings.optionsWith(ShowcaseSettings.ActionCurve).copy(
                            duration = 600.0,
                        ),
                    font = stageFont(size = 18.sp),
                    colour = showcaseColour(),
                )
            }
        }
    }
}

val actionDemo =
    Demo(
        id = "action",
        name = "Action",
        summary =
            "Processing Transaction becoming Transaction Safe. The word Transaction survives " +
                "and travels the width of Processing, which is the longest journey any segment makes " +
                "in this catalogue. On a curve whose control points rise past one, so it arrives, " +
                "overshoots a little and settles.",
        capability = "a long word journey, and a curve that overshoots",
        code =
            """
            TextMorph(
                text = state,
                options = TextMorphOptions(
                    duration = 600.0,
                    ease = TextMorphEase.Bezier(CubicBezier(0.41, 1.03, 0.6, 1.03)),
                ),
            )
            """.trimIndent(),
        content = { ActionDemo() },
    )
