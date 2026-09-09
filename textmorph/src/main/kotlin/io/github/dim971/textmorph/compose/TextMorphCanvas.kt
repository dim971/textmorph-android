package io.github.dim971.textmorph.compose

// Drawing a plan.
//
// One text run per segment, taken out of the whole-line shaping, under the
// transform the plan gives it. Everything that moves is read inside the draw
// lambda and nowhere else, so a frame never invalidates the layout of anything
// around the morph.

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.Shader
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import io.github.dim971.textmorph.core.MorphFrame
import io.github.dim971.textmorph.core.MorphPlan
import io.github.dim971.textmorph.core.MorphPoint
import io.github.dim971.textmorph.core.MorphTiming
import io.github.dim971.textmorph.core.SegmentAnimation
import io.github.dim971.textmorph.core.SegmentRole
import io.github.dim971.textmorph.core.SegmentState
import io.github.dim971.textmorph.engine.Ghost
import io.github.dim971.textmorph.engine.LayoutStore
import kotlin.math.abs
import kotlin.math.ceil

/**
 * How far outside the container a morph is allowed to draw.
 *
 * The furthest any segment travels, plus the curve's overshoot, and never less
 * than one slide. The default bezier overshoots by nothing, but the default
 * spring bounces about sixteen percent past its target, and a glyph clipped at
 * the peak of its bounce is a bug that only appears with springs.
 *
 * Rounded up to a whole pixel: a fractional offset rasterises every glyph at a
 * different subpixel phase, which reads as a faint softness and is invisible
 * until two renderings are compared.
 */
internal fun MorphPlan.bleed(): Double {
    var travel = 0.0
    for (animation in segments) {
        travel = maxOf(travel, abs(animation.from.dx), abs(animation.from.dy))
    }
    return ceil(maxOf(slideDistance, travel * (1 + overshoot)))
}

/**
 * Draws a morph at the origin of the current draw scope.
 *
 * Ghosts first, so a segment still leaving from an interrupted morph sits under
 * the one taking its place.
 */
internal fun DrawScope.drawMorph(
    plan: MorphPlan,
    frame: MorphFrame,
    ghosts: List<Ghost>,
    store: LayoutStore,
    colour: Color,
    debug: Boolean,
) {
    val canvas = drawContext.canvas.nativeCanvas
    val paint = store.shapingPaint.let { Paint(it) }
    paint.color = colour.toArgb()

    for (ghost in ghosts) {
        draw(canvas, ghost.plan, ghost.frame, store, paint, leavingOnly = true, debug = debug)
    }
    draw(canvas, plan, frame, store, paint, leavingOnly = false, debug = debug)
}

private fun draw(
    canvas: Canvas,
    plan: MorphPlan,
    frame: MorphFrame,
    store: LayoutStore,
    paint: Paint,
    leavingOnly: Boolean,
    debug: Boolean,
) {
    val opaque = paint.alpha
    for ((animation, state) in plan.segments.zip(frame.states)) {
        if (leavingOnly && !animation.role.isLeaving) continue
        if (state.opacity <= 0) continue
        if (animation.string.isEmpty()) continue

        if (animation.role.isNumber) {
            drawSlot(canvas, animation, state, store, paint, opaque)
        } else {
            drawSegment(canvas, animation, state, store, paint, opaque)
        }
        if (debug) outline(canvas, animation, state)
    }
    paint.alpha = opaque
}

// MARK: an ordinary segment

private fun drawSegment(
    canvas: Canvas,
    animation: SegmentAnimation,
    state: SegmentState,
    store: LayoutStore,
    paint: Paint,
    opaque: Int,
) {
    val shaped = store.shapedLine(animation.box.lineText)
    val saved = canvas.save()
    paint.alpha = (opaque * state.opacity).toInt()
    applyTransform(canvas, state, animation.scaleOrigin)
    shaped.draw(
        canvas,
        animation.box.range.first,
        animation.box.range.last + 1,
        animation.box.x.toFloat(),
        (animation.box.y + shaped.metrics.ascent).toFloat(),
        paint,
    )
    canvas.restoreToCount(saved)
}

// MARK: a character of a number, in its slot

/**
 * The slot takes the displacement, the character inside it takes the slide, and
 * the slide is clipped to the slot.
 *
 * Keeping the slide off the slot is what lets a digit cross a whole line box
 * without the next morph measuring it as having moved. The clip is extended
 * sideways so a glyph's overhang is not shaved off, and its top and bottom edges
 * are softened, because a digit appearing at a hard line reads as a cut rather
 * than as a roll.
 */
private fun drawSlot(
    canvas: Canvas,
    animation: SegmentAnimation,
    state: SegmentState,
    store: LayoutStore,
    paint: Paint,
    opaque: Int,
) {
    val shaped = store.shapedLine(animation.box.lineText)
    val box = animation.box
    val overhang = box.height
    val slot =
        RectF(
            (box.x + state.dx - overhang).toFloat(),
            (box.y + state.dy).toFloat(),
            (box.x + state.dx + box.width + overhang).toFloat(),
            (box.y + state.dy + box.height).toFloat(),
        )

    val saved = canvas.save()
    canvas.clipRect(slot)
    paint.alpha = (opaque * state.opacity).toInt()
    // Softening the edges needs the glyphs and the mask in one layer, so the
    // mask cannot eat what is drawn outside the slot.
    val layer = canvas.saveLayer(slot, null)

    shaped.draw(
        canvas,
        box.range.first,
        box.range.last + 1,
        (box.x + state.dx).toFloat(),
        (box.y + state.dy + state.moverDy + shaped.metrics.ascent).toFloat(),
        paint,
    )
    softEdges(canvas, slot, paint.textSize)

    canvas.restoreToCount(layer)
    canvas.restoreToCount(saved)
}

/**
 * Fades the top and bottom of a slot, by drawing a vertical gradient over what
 * is already there and keeping only where the gradient is opaque.
 *
 * The band is in ems, so it is a share of the font's size rather than of the
 * line box, and it is applied whether or not anything is sliding. Upstream masks
 * the slot positionally rather than on a timer, so the softness stays in step
 * with the slide at any duration. The consequence is that a digit at rest is
 * very slightly soft at the top and bottom, and that is upstream's look rather
 * than an artefact of this port.
 */
private fun softEdges(
    canvas: Canvas,
    slot: RectF,
    em: Float,
) {
    val band = em * MorphTiming.SLOT_FADE.toFloat()
    if (band <= 0 || slot.height() <= band * 2) return

    val opaque = android.graphics.Color.BLACK
    val clear = android.graphics.Color.TRANSPARENT
    val gradient =
        LinearGradient(
            slot.centerX(),
            slot.top,
            slot.centerX(),
            slot.bottom,
            intArrayOf(clear, opaque, opaque, clear),
            floatArrayOf(0f, band / slot.height(), 1 - band / slot.height(), 1f),
            Shader.TileMode.CLAMP,
        )

    val mask = Paint()
    mask.shader = gradient
    mask.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
    canvas.drawRect(slot, mask)
}

// MARK: the transforms

/**
 * The displacement, and the scale about whatever the plan said to scale about:
 * the segment's own centre, or a whole run's shared one.
 */
private fun applyTransform(
    canvas: Canvas,
    state: SegmentState,
    origin: MorphPoint,
) {
    canvas.translate(state.dx.toFloat(), state.dy.toFloat())
    if (state.scale == 1.0) return
    canvas.scale(
        state.scale.toFloat(),
        state.scale.toFloat(),
        origin.x.toFloat(),
        origin.y.toFloat(),
    )
}

// MARK: debug

private val debugPaint =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f
    }

/** Draws each segment's box, so the layout can be checked by eye. */
private fun outline(
    canvas: Canvas,
    animation: SegmentAnimation,
    state: SegmentState,
) {
    debugPaint.color = debugColour(animation.role)
    debugPaint.alpha = 128
    canvas.drawRect(
        (animation.box.x + state.dx).toFloat(),
        (animation.box.y + state.dy).toFloat(),
        (animation.box.x + state.dx + animation.box.width).toFloat(),
        (animation.box.y + state.dy + animation.box.height).toFloat(),
        debugPaint,
    )
}

/** A colour per role, so what is happening is readable rather than merely visible. */
private fun debugColour(role: SegmentRole): Int =
    when (role) {
        SegmentRole.PERSIST, SegmentRole.NUMBER_PERSIST -> 0xFF4D99FF.toInt()
        SegmentRole.ENTER, SegmentRole.NUMBER_ENTER, SegmentRole.GROUP_ENTER -> 0xFF33CC66.toInt()
        SegmentRole.EXIT, SegmentRole.NUMBER_EXIT, SegmentRole.GROUP_EXIT -> 0xFFFF6650.toInt()
    }
