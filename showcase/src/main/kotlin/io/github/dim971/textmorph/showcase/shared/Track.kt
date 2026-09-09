package io.github.dim971.textmorph.showcase.shared

// A track with a bubble riding its thumb.
//
// Three of upstream's demos are this shape. Upstream hangs the bubble on a
// spring so it leans into the travel, and where two bubbles meet it solves a
// separating-axis test and leans them apart. None of that is reproduced here:
// the bubble is carried rather than thrown. What is kept is the thing the demo
// exists for, which is a value morphing inside a box that is moving.

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val THUMB = 18.dp
private val TRACK = 4.dp

/** Where the track sits under the bubbles. */
private val TRACK_TOP = 72.dp

/** Where a pill sits, and where it goes when it has to make room. */
private val PILL_TOP = 34.dp
private val PILL_LIFTED = 0.dp

/** One bubble on one thumb. */
@Composable
fun BubbleTrack(
    fraction: Float,
    modifier: Modifier = Modifier,
    trackWidth: Dp = 240.dp,
    onFraction: ((Float) -> Unit)? = null,
    bubbleColour: Color? = null,
    bubble: @Composable () -> Unit,
) {
    RangeTrack(
        fractions = listOf(fraction),
        modifier = modifier,
        trackWidth = trackWidth,
        onFraction = onFraction?.let { set -> { _, value -> set(value) } },
        bubbleColour = bubbleColour,
        bubbles = listOf(bubble),
    )
}

/**
 * A track carrying one or two bubbles.
 *
 * Two is the interesting case, and it is where this parts company with upstream:
 * when the bubbles would overlap, upstream leans them apart about their tails,
 * and this lifts the lower one clear instead. Simpler, and it keeps both values
 * readable, which is what the leaning is for.
 */
@Composable
fun RangeTrack(
    fractions: List<Float>,
    modifier: Modifier = Modifier,
    trackWidth: Dp = 240.dp,
    onFraction: ((Int, Float) -> Unit)? = null,
    /** Null takes the theme's own accent, which is what most of these want. */
    bubbleColour: Color? = null,
    bubbles: List<@Composable () -> Unit>,
) {
    BoxWithConstraints(modifier.width(trackWidth).height(TRACK_TOP + THUMB)) {
        val span = maxWidth - THUMB
        val from = if (fractions.size > 1) fractions.first() else 0f
        val to = fractions.last()
        // Close enough that two pills would touch, in the units the track is
        // in. Generous, because the pills are as wide as their values and a
        // value can gain a digit mid-morph.
        val crowded = fractions.size > 1 && (to - from) * span.value < 104f

        Box(
            Modifier
                .fillMaxWidth()
                .padding(top = TRACK_TOP)
                .height(TRACK)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        )
        Box(
            Modifier
                .padding(top = TRACK_TOP)
                .offset(x = THUMB / 2 + span * from.coerceIn(0f, 1f))
                .width(span * (to - from).coerceIn(0f, 1f))
                .height(TRACK)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.primary),
        )

        for ((index, fraction) in fractions.withIndex()) {
            val centre = THUMB / 2 + span * fraction.coerceIn(0f, 1f)

            Box(
                Modifier
                    .offset(x = centre - THUMB / 2, y = TRACK_TOP - THUMB / 2 + TRACK / 2)
                    .size(THUMB)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.onSurface)
                    .then(
                        if (onFraction == null) {
                            Modifier
                        } else {
                            Modifier.pointerInput(index, span) {
                                detectDragGestures { _, delta ->
                                    onFraction(
                                        index,
                                        (fraction + delta.x / span.toPx()).coerceIn(0f, 1f),
                                    )
                                }
                            }
                        },
                    ),
            )

            // Half the pill's own width is unknown until it is measured, so the
            // pill is centred on the thumb by a box that is itself centred.
            Box(
                modifier =
                    Modifier
                        .offset(
                            x = centre - trackWidth / 2,
                            y = if (crowded && index == 0) PILL_LIFTED else PILL_TOP,
                        ).fillMaxWidth(),
                contentAlignment = Alignment.TopCenter,
            ) {
                Bubble(bubbleColour) { bubbles[index]() }
            }
        }
    }
}

/** The pill a value rides in. */
@Composable
private fun Bubble(
    colour: Color?,
    content: @Composable () -> Unit,
) {
    Box(
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(colour ?: MaterialTheme.colorScheme.primary)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
