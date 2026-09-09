package io.github.dim971.textmorph.engine

// The state machine: what is on screen, what it is becoming, and when.
//
// This is upstream's text-morph/index.ts, minus the DOM. Three things about it
// are not obvious and all three come straight from the original.
//
// The clock is linear and nothing else. Every curve is applied by the plan, so
// the engine only ever needs to know how many milliseconds have passed. That is
// what lets an opacity window be a fraction of the duration rather than a
// fraction of the eased progress.
//
// A segment already on its way out when the next morph arrives keeps its own
// clock. Upstream's exit filter skips anything already marked exiting, so it
// carries on with the animation it had and removes itself when its fade closes.
// Here that is a ghost: a plan of its own with its own start time.
//
// Exactly one of the completion callbacks runs per morph. Not by care, but
// because the token that carries them can only fire once.

import io.github.dim971.textmorph.core.DiffOptions
import io.github.dim971.textmorph.core.MintedIds
import io.github.dim971.textmorph.core.MorphAlignment
import io.github.dim971.textmorph.core.MorphCallbacks
import io.github.dim971.textmorph.core.MorphFrame
import io.github.dim971.textmorph.core.MorphLayout
import io.github.dim971.textmorph.core.MorphPlan
import io.github.dim971.textmorph.core.MorphPlanInput
import io.github.dim971.textmorph.core.MorphPlanner
import io.github.dim971.textmorph.core.MorphSize
import io.github.dim971.textmorph.core.MorphValue
import io.github.dim971.textmorph.core.Segment
import io.github.dim971.textmorph.core.SegmentDiff
import io.github.dim971.textmorph.core.SegmentState
import io.github.dim971.textmorph.core.TextMorphOptions
import io.github.dim971.textmorph.core.TextSegmenter
import io.github.dim971.textmorph.core.UTF16Offset

/** Carries a morph's completion callbacks, and can only fire once. */
internal class MorphToken(
    private val callbacks: MorphCallbacks,
) {
    private var hasFired = false

    /** Whether either has already run. */
    val isSpent: Boolean get() = hasFired

    /** The morph ran its course. */
    fun complete() {
        if (hasFired) return
        hasFired = true
        callbacks.onComplete?.invoke()
    }

    /** The morph was replaced before it finished. */
    fun cancel() {
        if (hasFired) return
        hasFired = true
        callbacks.onCancel?.invoke()
    }
}

/** One morph, and when it started. */
internal class RunningMorph(
    val plan: MorphPlan,
    private val startedAtMs: Double,
) {
    fun elapsed(nowMs: Double): Double = maxOf(0.0, nowMs - startedAtMs)

    fun isFinished(nowMs: Double): Boolean = elapsed(nowMs) >= plan.durationMs
}

/** A morph that was interrupted and is still finishing its exits. */
internal class Ghost(
    val plan: MorphPlan,
    val frame: MorphFrame,
)

/** Everything a draw pass needs for one frame. */
internal class RenderFrame(
    val plan: MorphPlan,
    val frame: MorphFrame,
    val ghosts: List<Ghost>,
    /** Whether anything at all is still moving. */
    val isSettled: Boolean,
)

/**
 * The morph a composable owns.
 *
 * The clock is passed in rather than read, so the whole state machine is
 * testable without a frame callback and without sleeping: the interruption suite
 * drives twenty updates eight milliseconds apart and asserts on the callbacks
 * and on the ghosts.
 */
internal class MorphEngine(
    private val store: LineMeasuring,
    private var options: TextMorphOptions = TextMorphOptions(),
    private var alignment: MorphAlignment = MorphAlignment.LEADING,
    private var reduceMotion: Boolean = false,
) {
    /** The value currently shown, already formatted. */
    var text: String = ""
        private set

    /** The segments currently at rest, which the next diff runs against. */
    private var segments: List<Segment> = emptyList()

    /** Where they are, which is the layout the next morph starts from. */
    private var layout: MorphLayout? = null

    /**
     * Whether the next update is the first, which never animates and never fires
     * a callback.
     */
    private var isFirstValue = true

    private var running: RunningMorph? = null
    private var token: MorphToken? = null
    private var ghosts = mutableListOf<RunningMorph>()

    /**
     * One minter for the life of the composable, so a numeric identity cannot be
     * minted twice while the first one is still on screen.
     */
    private val minter = MintedIds()

    /** Whether the morphing is off, for either reason. */
    val isDisabled: Boolean
        get() = options.disabled || (options.respectReducedMotion && reduceMotion)

    /** Where the first line's baseline sits, so a morph lines up with a `Text`. */
    val firstBaseline: Double get() = layout?.firstBaseline ?: 0.0

    /** The size the container should report right now. */
    fun size(nowMs: Double): MorphSize {
        val running =
            running ?: return layout
                ?.let { MorphSize(it.width, it.height) }
                ?: MorphSize.Zero
        val frame = running.plan.frame(running.elapsed(nowMs))
        return MorphSize(frame.width, frame.height)
    }

    // MARK: configuration

    /**
     * Applies a new configuration.
     *
     * A change that alters what a morph would look like takes effect on the next
     * value rather than restarting the one in flight, which is what upstream's
     * config key achieves by only tearing the instance down when something it
     * cares about changed.
     */
    fun configure(
        options: TextMorphOptions,
        alignment: MorphAlignment,
        reduceMotion: Boolean,
    ) {
        val wasDisabled = isDisabled
        this.options = options
        this.alignment = alignment
        this.reduceMotion = reduceMotion

        // Coming back from disabled, the segments on screen were never measured,
        // so a diff against them would animate from boxes that never existed.
        // Upstream resets its own two fields for exactly this reason.
        if (wasDisabled && !isDisabled) forget()
        if (!wasDisabled && isDisabled) settleImmediately()
    }

    /** Drops what is on screen, so the next value arrives without animating. */
    private fun forget() {
        segments = emptyList()
        layout = null
        isFirstValue = true
        running = null
        ghosts = mutableListOf()
        token = null
    }

    /** Stops everything where it should have ended up. */
    private fun settleImmediately() {
        ghosts = mutableListOf()
        // Deliberately not cancelled: upstream fires neither callback on this
        // path, because the morph did not fail, it was switched off.
        token = null
        val layout = layout
        running = if (layout == null) null else RunningMorph(still(layout), 0.0)
    }

    /** Re-lays the value out, for a font or a size that changed under it. */
    fun relayout() {
        if (segments.isEmpty()) return
        val layout = store.layout(segments, alignment)
        this.layout = layout
        // At rest, not animating: a size category changing is not a morph.
        running = RunningMorph(still(layout), 0.0)
        ghosts = mutableListOf()
        token = null
    }

    // MARK: the value

    /**
     * Shows a new value.
     *
     * Returns whether anything changed, so a caller can tell a no-op from a
     * morph. An update whose formatted value equals the one already shown does
     * nothing at all, not even fire `onStart`, which is upstream's first line.
     */
    fun update(
        value: MorphValue,
        cursorIndex: Int? = null,
        callbacks: MorphCallbacks = MorphCallbacks(),
        nowMs: Double = 0.0,
    ): Boolean {
        // Upstream's first line, and it comes before everything including
        // `onStart`: an update that does not change the formatted value is not a
        // morph. One consequence is that showing an empty value first does
        // nothing at all, since the engine starts out showing one.
        val formatted = options.formatted(value)
        if (formatted == text) return false
        text = formatted

        if (isDisabled) {
            settle(formatted)
            return true
        }

        if (isFirstValue) {
            first(formatted)
            return true
        }

        callbacks.onStart?.invoke()
        morph(formatted, cursorIndex, callbacks, nowMs)
        return true
    }

    /**
     * The value arrives already in place: no clock, no callbacks.
     *
     * It still gets a plan, because there is only one rendering path: a still
     * plan is how "draw this, not moving" is expressed.
     */
    private fun settle(formatted: String) {
        segments = segment(formatted)
        val layout = store.layout(segments, alignment)
        this.layout = layout
        // A later diff against these would animate from boxes that were never on
        // screen, so the next enabled morph starts fresh.
        isFirstValue = true
        running = RunningMorph(still(layout), 0.0)
        ghosts = mutableListOf()
    }

    /** The first value a composable shows, which never animates. */
    private fun first(formatted: String) {
        segments = segment(formatted)
        val layout = store.layout(segments, alignment)
        this.layout = layout
        isFirstValue = false
        running = RunningMorph(still(layout), 0.0)
    }

    private fun morph(
        formatted: String,
        cursorIndex: Int?,
        callbacks: MorphCallbacks,
        nowMs: Double,
    ) {
        val oldLayout = layout
        if (oldLayout == null) {
            first(formatted)
            return
        }

        // What was on screen becomes a ghost, so anything already leaving keeps
        // its own clock and finishes its own fade.
        val carried = carriedStates(nowMs)
        running?.let {
            if (!it.isFinished(nowMs)) {
                token?.cancel()
                ghosts.add(it)
            }
        }
        ghosts = ghosts.filter { !it.isFinished(nowMs) }.toMutableList()

        val result =
            SegmentDiff.diffSegments(
                segments,
                formatted,
                options.locale,
                DiffOptions(
                    numbers = options.numbers,
                    cursorIndex = cursorIndex?.let { UTF16Offset(it) },
                ),
                minter,
            )

        // A value that empties out keeps a zero-width stand-in in the flow, so
        // the line box does not collapse while the last segments are still
        // leaving.
        val isEmpty = result.segments.isEmpty()
        val newSegments =
            if (isEmpty) {
                listOf(Segment(id = Segment.EMPTY_ID, string = "\u200B"))
            } else {
                result.segments
            }

        // The old segments are cut finer before being measured, exactly as
        // upstream splits the spans before reading their boxes.
        val split = applySplits(segments, result.splits)
        val splitLayout =
            if (result.splits.isEmpty()) {
                oldLayout
            } else {
                store.layout(split, alignment)
            }

        val newLayout = store.layout(newSegments, alignment)
        val firstFrame = store.layout(newSegments, alignment, splitLayout.width)

        val resolved = options.ease.resolve(options.duration)
        val plan =
            MorphPlanner.plan(
                MorphPlanInput(
                    oldSegments = split,
                    newSegments = newSegments,
                    oldLayout = splitLayout,
                    newLayout = newLayout,
                    firstFrameLayout = firstFrame,
                    durationMs = resolved.durationMs,
                    curve = resolved.curve,
                    scale = options.scale,
                    carried = carried,
                    isEmptyTransition = isEmpty,
                ),
            )

        segments = newSegments
        layout = newLayout
        running = RunningMorph(plan, nowMs)
        token = MorphToken(callbacks)
    }

    // MARK: frames

    /**
     * The frame to draw at a moment, firing a completion that has come due.
     *
     * The callback is fired from here rather than from a timer so it cannot run
     * before the frame that finished the morph has been drawn, and cannot run at
     * all for a morph that was replaced first.
     */
    fun render(nowMs: Double): RenderFrame? {
        val running = running ?: return null

        ghosts = ghosts.filter { !it.isFinished(nowMs) }.toMutableList()
        val frame = running.plan.frame(running.elapsed(nowMs))

        val token = token
        if (frame.isFinished && token != null && !token.isSpent) token.complete()

        return RenderFrame(
            plan = running.plan,
            frame = frame,
            ghosts = ghosts.map { Ghost(it.plan, it.plan.frame(it.elapsed(nowMs))) },
            isSettled = frame.isFinished && ghosts.isEmpty(),
        )
    }

    /**
     * Where every segment is right now, for a morph interrupting another.
     *
     * Only the displacement and the opacity. Upstream reads exactly those two
     * back off the running animation and lets the scale restart.
     */
    private fun carriedStates(nowMs: Double): Map<String, SegmentState> {
        val running = running ?: return emptyMap()
        if (running.isFinished(nowMs)) return emptyMap()
        val frame = running.plan.frame(running.elapsed(nowMs))

        val out = HashMap<String, SegmentState>()
        for ((animation, state) in running.plan.segments.zip(frame.states)) {
            if (animation.role.isLeaving) continue
            out[animation.id] =
                SegmentState(
                    dx = state.dx,
                    dy = state.dy,
                    opacity = state.opacity,
                    moverDy = state.moverDy,
                )
        }
        return out
    }

    private fun segment(formatted: String): List<Segment> =
        TextSegmenter.segmentText(formatted, options.locale, options.numbers, minter)

    private fun still(layout: MorphLayout): MorphPlan = MorphPlanner.still(layout, segments)

    /** Substitutes the finer spans the diff asked for. */
    private fun applySplits(
        segments: List<Segment>,
        splits: Map<String, List<Segment>>,
    ): List<Segment> {
        if (splits.isEmpty()) return segments
        return segments.flatMap { splits[it.id] ?: listOf(it) }
    }
}
