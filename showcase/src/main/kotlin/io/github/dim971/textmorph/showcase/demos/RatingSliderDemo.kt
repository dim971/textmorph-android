package io.github.dim971.textmorph.showcase.demos

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import io.github.dim971.textmorph.showcase.shared.onTint
import io.github.dim971.textmorph.showcase.shared.rememberAutoplay
import io.github.dim971.textmorph.showcase.shared.stageFont
import kotlin.math.roundToInt

private val RATINGS =
    listOf(
        "Abysmal" to Color(0xFFF2453D),
        "Awful" to Color(0xFFFF7A2F),
        "Alright" to Color(0xFFF0B429),
        "Amazing" to Color(0xFF3B82F6),
        "Astonishing" to Color(0xFF34C759),
    )

/** Five words that all begin with an A. */
@Composable
fun RatingSliderDemo() {
    val settings = LocalShowcaseSettings.current
    var index by remember { mutableIntStateOf(0) }
    val autoplay = rememberAutoplay(1800) { index = (index + 1) % RATINGS.size }
    val fraction by animateFloatAsState(index / (RATINGS.size - 1f), label = "rating")
    val (word, tone) = RATINGS[index]

    Stage(caption = if (autoplay.isPlaying) "drag to rate" else "rating") {
        BubbleTrack(
            fraction = fraction,
            onFraction =
                if (!LocalInteractive.current) {
                    null
                } else {
                    { next ->
                        autoplay.takeOver()
                        index = (next * (RATINGS.size - 1)).roundToInt()
                    }
                },
            // The pill takes the rating's colour and the word goes dark on
            // it, rather than the other way round: five tones on one accent
            // would have two of them unreadable.
            bubbleColour = tone,
        ) {
            TextMorph(
                text = word,
                options = settings.options,
                font = stageFont(size = 20.sp),
                colour = onTint(tone),
            )
        }
    }
}

val ratingSliderDemo =
    Demo(
        id = "rating",
        name = "Rating slider",
        summary =
            "Abysmal, Awful, Alright, Amazing, Astonishing. Every one of them starts with " +
                "an A, and the A never moves: it is the survivor the rest of the word is measured " +
                "against. Watch the second letter instead, which is where each morph actually happens.",
        capability = "a shared first character anchoring five different words",
        code =
            """
            BubbleTrack(fraction = index / 4f) {
                TextMorph(text = word, colour = tone)
            }
            """.trimIndent(),
        content = { RatingSliderDemo() },
    )
