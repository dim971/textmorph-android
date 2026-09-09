package io.github.dim971.textmorph.showcase.demos

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.sp
import io.github.dim971.textmorph.compose.TextMorph
import io.github.dim971.textmorph.showcase.catalog.Demo
import io.github.dim971.textmorph.showcase.shared.LocalInteractive
import io.github.dim971.textmorph.showcase.shared.LocalShowcaseSettings
import io.github.dim971.textmorph.showcase.shared.RangeTrack
import io.github.dim971.textmorph.showcase.shared.Stage
import io.github.dim971.textmorph.showcase.shared.onTint
import io.github.dim971.textmorph.showcase.shared.rememberAutoplay
import io.github.dim971.textmorph.showcase.shared.stageFont
import kotlin.math.roundToInt

/** Units held between the thumbs, so both stay grabbable. Upstream's number. */
private const val GAP = 8

private val PRESETS = listOf(32 to 68, 12 to 30, 55 to 91, 40 to 52)

/** Two values that have to make room for each other. */
@Composable
fun RangeShoveDemo() {
    val settings = LocalShowcaseSettings.current
    var preset by remember { mutableIntStateOf(0) }
    var taken by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    val autoplay = rememberAutoplay(2200) { preset = (preset + 1) % PRESETS.size }

    // The presets glide; a drag does not. Same reason as the single slider: an
    // easing between the finger and the thumb is the thumb lagging the finger.
    val (loPreset, hiPreset) = PRESETS[preset]
    val loGlided by animateFloatAsState(loPreset / 100f, label = "lo")
    val hiGlided by animateFloatAsState(hiPreset / 100f, label = "hi")
    val lo = taken?.let { it.first / 100f } ?: loGlided
    val hi = taken?.let { it.second / 100f } ?: hiGlided

    fun set(
        index: Int,
        fraction: Float,
    ) {
        autoplay.takeOver()
        val current = taken ?: PRESETS[preset]
        val value = (fraction * 100).roundToInt()
        taken =
            if (index == 0) {
                minOf(value, current.second - GAP) to current.second
            } else {
                current.first to maxOf(value, current.first + GAP)
            }
    }

    Stage(caption = if (autoplay.isPlaying) "drag either thumb" else "range") {
        RangeTrack(
            fractions = listOf(lo, hi),
            onFraction = if (LocalInteractive.current) ::set else null,
            bubbles =
                listOf(
                    {
                        TextMorph(
                            text = "${'$'}${(lo * 100).roundToInt()}",
                            options = settings.options,
                            font = stageFont(size = 20.sp),
                            colour = onTint(settings.tint.colour),
                        )
                    },
                    {
                        TextMorph(
                            text = "${'$'}${(hi * 100).roundToInt()}",
                            options = settings.options,
                            font = stageFont(size = 20.sp),
                            colour = onTint(settings.tint.colour),
                        )
                    },
                ),
        )
    }
}

val rangeShoveDemo =
    Demo(
        id = "range",
        name = "Range shove",
        summary =
            "Two pills on one track, each morphing its own value, and each in the other's " +
                "way. Drag them together and they pivot apart about their tail tips, because a " +
                "tail stays pinned to its thumb and leaning is the only way out of an overlap. " +
                "How far they lean is found by bisecting on the daylight between their bodies, " +
                "which is upstream's own rule and the whole of this card.",
        capability = "two morphs competing for the same space",
        code =
            """
            RangeTrack(fractions = listOf(lo, hi), bubbles = listOf(
                { TextMorph(text = money(lo)) },
                { TextMorph(text = money(hi)) },
            ))
            """.trimIndent(),
        content = { RangeShoveDemo() },
    )
