package io.github.dim971.textmorph.showcase.demos

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import io.github.dim971.textmorph.showcase.shared.rememberReducedMotion
import io.github.dim971.textmorph.showcase.shared.showcaseColour
import io.github.dim971.textmorph.showcase.shared.stageFont
import kotlinx.coroutines.delay

/** Upstream's thresholds, and the label each one earns. */
private val STEPS =
    listOf(
        0f to "Hold to Delete",
        0.12f to "Holding to Delete",
        0.62f to "Deleting",
        1f to "Deleted",
    )

/** How fast it drains when let go, as a share of the bar per second. */
private const val RELEASE = 0.9f

private fun labelAt(progress: Float): String = STEPS.last { progress >= it.first }.second

/** A button that has to be meant. */
@Composable
fun HoldToConfirmDemo() {
    val settings = LocalShowcaseSettings.current
    val live = LocalInteractive.current
    var progress by remember { mutableFloatStateOf(0f) }
    var held by remember { mutableStateOf(false) }
    var auto by remember { mutableStateOf(true) }
    val still = rememberReducedMotion()

    // Autoplay runs the whole gesture, holds on the finished state, and starts
    // over. A card that only moved under a finger would be a blank in the
    // catalogue.
    rememberAutoplay(4600) { if (auto) held = true }

    LaunchedEffect(held, still) {
        if (still) return@LaunchedEffect
        while (true) {
            delay(16)
            if (held) {
                progress = (progress + 0.016f / 1.6f).coerceAtMost(1f)
                if (progress == 1f && auto) held = false
            } else if (progress < 1f) {
                progress = (progress - 0.016f * RELEASE).coerceAtLeast(0f)
            } else {
                delay(900)
                progress = 0f
            }
        }
    }

    Stage {
        Box(
            modifier =
                Modifier
                    .width(220.dp)
                    .height(48.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .pointerInput(live) {
                        if (!live) return@pointerInput
                        detectTapGestures(
                            onPress = {
                                auto = false
                                held = true
                                tryAwaitRelease()
                                held = false
                            },
                        )
                    },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .width(220.dp * progress)
                    .background(Color(0xFFF2453D))
                    .align(Alignment.CenterStart),
            )
            TextMorph(
                text = labelAt(progress),
                options = settings.options,
                font = stageFont(size = 18.sp),
                colour = showcaseColour(),
            )
        }
        Caption("press and hold")
    }
}

val holdToConfirmDemo =
    Demo(
        id = "hold",
        name = "Hold to confirm",
        summary =
            "Hold to Delete, Holding to Delete, Deleting, Deleted. Four labels crossed at " +
                "four points of one gesture, and each morph starts wherever the last one had got to, " +
                "because letting go part way drains the bar back through the same thresholds in " +
                "reverse. Hold is the survivor between the first two, and Delet is the survivor " +
                "through all four.",
        capability = "a morph reversed mid-flight by a gesture",
        code =
            """
            TextMorph(text = STEPS.last { progress >= it.first }.second)
            """.trimIndent(),
        content = { HoldToConfirmDemo() },
    )
