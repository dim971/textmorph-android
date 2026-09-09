package io.github.dim971.textmorph.showcase.shared

// A track with a bubble hanging off its thumb.
//
// Three of upstream's cards are this shape, and on two of them the motion is
// the demo rather than the decoration: the bubble trails its thumb and leans
// into the travel, and where two bubbles meet they pivot apart about their tail
// tips. Both come from `Bubbles.kt`, transcribed from upstream with its own
// constants.
//
// Everything here is in density-independent points, not device pixels, and that
// is not a detail. Upstream's constants are CSS pixels, which is what a dp is;
// feeding device pixels to them makes every threshold three times too small on
// a three-times screen, so the pair never lean and the pill barely tilts. Pixels
// appear at exactly one place, where a position is handed to the layout.
//
// Everything that moves per frame is written into float state and read inside a
// layout or a layer lambda, never in the composable's body, so a frame moves the
// pill without recomposing the track.

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.roundToInt

private val THUMB = 18.dp
private val TRACK = 4.dp

/** Where the track sits under the bubbles. */
private val TRACK_TOP = 62.dp

/** The state one frame of the physics writes, and the layout reads. All in points. */
private class Motion(
    count: Int,
) {
    val lag = List(count) { mutableFloatStateOf(0f) }
    val tilt = List(count) { mutableFloatStateOf(0f) }
    val stretch = List(count) { mutableFloatStateOf(0f) }
    val width = List(count) { mutableFloatStateOf(0f) }
    val height = List(count) { mutableFloatStateOf(0f) }

    /**
     * How pressed together the pair are, kept unclamped because that is where
     * the spring's overshoot lives. Clamped only where it is read.
     */
    val shove = mutableFloatStateOf(0f)
    val squash = mutableFloatStateOf(0f)
    val lean = mutableFloatStateOf(0f)
}

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
 * Two is the interesting case: both tails stay pinned to their thumbs, so
 * leaning is the only way out of an overlap, and how far they lean is found by
 * bisecting on the daylight between their bodies. That is upstream's rule, and
 * it is the whole of the Range shove card.
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
    val count = fractions.size
    val motion = remember(count) { Motion(count) }
    val bobs = remember(count) { List(count) { Bob() } }
    val density = LocalDensity.current
    val scale = density.density
    val accent = bubbleColour ?: MaterialTheme.colorScheme.primary

    BoxWithConstraints(modifier.width(trackWidth).height(TRACK_TOP + THUMB)) {
        val span = maxWidth - THUMB
        val spanDp = span.value
        val thumbHalf = THUMB.value / 2

        fun thumbAt(fraction: Float) = thumbHalf + spanDp * fraction.coerceIn(0f, 1f)

        // Keyed on the targets rather than on the list, which is a new object
        // every recomposition. The loop runs until the springs settle and then
        // stops, so a track at rest costs nothing.
        LaunchedEffect(spanDp, fractions.joinToString(",")) {
            for ((index, fraction) in fractions.withIndex()) {
                val target = thumbAt(fraction)
                if (motion.lag[index].floatValue == 0f) bobs[index].carry(target)
                bobs[index].x = target
            }

            var shoveVelocity = 0f
            while (true) {
                withFrameNanos { }

                for (bob in bobs) bob.swing()

                val target =
                    if (count > 1) {
                        shoveTarget(
                            motion.width[0].floatValue,
                            motion.width[1].floatValue,
                            bobs[1].x - bobs[0].x,
                        )
                    } else {
                        0f
                    }
                shoveVelocity =
                    (shoveVelocity + (target - motion.shove.floatValue) * SHOVE_STIFFNESS) *
                    SHOVE_DAMPING
                motion.shove.floatValue += shoveVelocity
                motion.squash.floatValue =
                    motion.shove.floatValue.coerceIn(0f, 1f) * SHOVE_SQUASH

                for ((index, bob) in bobs.withIndex()) {
                    motion.lag[index].floatValue = bob.lag
                    motion.tilt[index].floatValue = bob.tilt
                    motion.stretch[index].floatValue = bob.stretch
                }

                motion.lean.floatValue =
                    if (count > 1) {
                        leanApart(
                            loBox = bodyOf(motion, 0),
                            hiBox = bodyOf(motion, 1),
                            loTilt = motion.tilt[0].floatValue,
                            hiTilt = motion.tilt[1].floatValue,
                            loX = bobs[0].lag,
                            hiX = bobs[1].lag,
                        )
                    } else {
                        0f
                    }

                val settled =
                    bobs.all { it.isSettled } &&
                        abs(shoveVelocity) < 0.001f &&
                        abs(target - motion.shove.floatValue) < 0.002f
                if (settled) break
            }
        }

        Box(
            Modifier
                .fillMaxWidth()
                .padding(top = TRACK_TOP)
                .height(TRACK)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        )

        val from = if (count > 1) fractions.first() else 0f
        Box(
            Modifier
                .padding(top = TRACK_TOP)
                .offset(x = THUMB / 2 + span * from.coerceIn(0f, 1f))
                .width(span * (fractions.last() - from).coerceIn(0f, 1f))
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
                            Modifier.pointerInput(index, spanDp) {
                                detectDragGestures { _, delta ->
                                    val step = delta.x / scale / spanDp
                                    onFraction(index, (fraction + step).coerceIn(0f, 1f))
                                }
                            }
                        },
                    ),
            )

            // The pill rides the bob rather than the thumb, and pivots about its
            // own tail tip. Bottom-aligned in a box reaching the track's centre
            // line, so the tip lands on the thumb whatever the pill's own height
            // turns out to be, and centred horizontally by a full-width box
            // because half its width is unknown until it is measured.
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(TRACK_TOP + TRACK / 2)
                        .offset {
                            IntOffset(
                                x =
                                    ((motion.lag[index].floatValue - spanDp / 2 - thumbHalf) * scale)
                                        .roundToInt(),
                                y = 0,
                            )
                        },
                contentAlignment = Alignment.BottomCenter,
            ) {
                Box(
                    Modifier
                        .graphicsLayer {
                            transformOrigin = TransformOrigin(0.5f, 1f)
                            val apart =
                                when {
                                    count < 2 -> 0f
                                    index == 0 -> -motion.lean.floatValue
                                    else -> motion.lean.floatValue
                                }
                            rotationZ = motion.tilt[index].floatValue + apart
                            val stretch = motion.stretch[index].floatValue
                            scaleX = scaleXOf(stretch, motion.squash.floatValue)
                            scaleY = 1 + stretch
                        }.onSizeChanged {
                            motion.width[index].floatValue = it.width / scale
                            motion.height[index].floatValue = it.height / scale - TAIL
                        }.background(accent, BubbleShape())
                        .padding(
                            start = 10.dp,
                            end = 10.dp,
                            top = 4.dp,
                            bottom = 4.dp + TAIL.dp,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    bubbles[index]()
                }
            }
        }
    }
}

/** A bubble's body as the collision test wants it, in points. */
private fun bodyOf(
    motion: Motion,
    index: Int,
): BubbleBox {
    val stretch = motion.stretch[index].floatValue
    return BubbleBox(
        width = motion.width[index].floatValue,
        height = motion.height[index].floatValue.coerceAtLeast(1f),
        scaleX = scaleXOf(stretch, motion.squash.floatValue),
        scaleY = 1 + stretch,
    )
}

/**
 * A rounded body with a tail hanging off the bottom, tip down.
 *
 * The tip is where the bubble pivots, which is why it is part of the shape
 * rather than a separate view: the layer's transform origin can then be the
 * bottom of the box and mean the right thing.
 */
private class BubbleShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val tail = with(density) { TAIL.dp.toPx() }
        val radius = with(density) { BUBBLE_RADIUS.dp.toPx() }
        val body = size.height - tail
        val path =
            Path().apply {
                addRoundRect(
                    RoundRect(
                        left = 0f,
                        top = 0f,
                        right = size.width,
                        bottom = body,
                        cornerRadius = CornerRadius(radius),
                    ),
                )
                // Slightly off-centre and asymmetric, which is what makes it read as
                // a tail rather than as an arrow.
                moveTo(size.width / 2 - tail * 0.85f, body - 1f)
                lineTo(size.width / 2 + tail * 0.15f, size.height)
                lineTo(size.width / 2 + tail * 0.55f, body - 1f)
                close()
            }
        return Outline.Generic(path)
    }
}
