package io.github.dim971.textmorph.core

// Turning two layouts and a diff into a plan.
//
// This replaces upstream's flip.ts together with the orchestration half of
// text-morph/index.ts, and it is where the direction of every displacement is
// decided. Two things about it are easy to get backwards and both are load
// bearing.
//
// A segment that survives, or arrives, is displaced by the *inverse* delta: it
// starts where it used to be and animates to nothing. A segment that leaves is
// displaced by the *forward* delta of whatever it anchors to: it starts at
// nothing and travels to where that anchor went. Those are the two halves of the
// same gesture, and swapping either makes the morph read backwards.
//
// A run of at least six adjacent segments all leaving, or all arriving, carries
// no displacement at all. It scales about the run's own centre instead.
// Upstream's group animation *replaces* the transform rather than composing with
// it, which is easy to miss and produces a run that both travels and collapses
// if it is missed.

/** What the planner needs to know. */
public class MorphPlanInput(
    /** The segments already on screen, with any splits already applied. */
    public val oldSegments: List<Segment>,
    /** The segments the new value wants. */
    public val newSegments: List<Segment>,
    /** Where the old segments were. */
    public val oldLayout: MorphLayout,
    /** Where the new segments will settle. */
    public val newLayout: MorphLayout,
    /**
     * The new segments laid out at the width the container had a moment ago.
     *
     * This is what upstream buys by writing the old width onto the element and
     * forcing a reflow before measuring. It only differs from [newLayout] for a
     * centred or trailing value, and for those it is the difference between the
     * text holding still and the whole line sliding sideways.
     */
    public val firstFrameLayout: MorphLayout,
    /** How long the morph takes, in milliseconds. */
    public val durationMs: Double,
    /** The curve every displacement follows. */
    public val curve: EasingCurve,
    /** Whether a leaving segment shrinks as it goes. */
    public val scale: Boolean = true,
    /**
     * Where each segment already was, for a morph interrupting another.
     *
     * Only the displacement and the opacity are carried. Upstream reads exactly
     * those two back off the running animation and lets the scale restart, so a
     * segment interrupted mid-shrink snaps to full size and carries on from where
     * it had travelled to.
     */
    public val carried: Map<String, SegmentState> = emptyMap(),
    /** Set when the new value is empty and a stand-in is holding the line box. */
    public val isEmptyTransition: Boolean = false,
)

/** Building a plan. */
public object MorphPlanner {
    /** The plan for one change of value. */
    public fun plan(input: MorphPlanInput): MorphPlan {
        val animations = leaving(input) + arrivingAndPersisting(input)

        return MorphPlan(
            durationMs = input.durationMs,
            curve = input.curve,
            lineCount = input.newLayout.lineCount,
            slideDistance = input.newLayout.slideDistance,
            segments = animations,
            container = container(input),
        )
    }

    /**
     * The plan for a value appearing for the first time.
     *
     * Nothing moves and nothing fades. Upstream's initial render produces no
     * animations and fires no callbacks, and that is not an optimisation:
     * animating the first value in would mean every piece of morphing text on a
     * screen flew in from nowhere the moment it appeared.
     */
    public fun still(
        layout: MorphLayout,
        segments: List<Segment>,
    ): MorphPlan {
        val strings = stringsById(segments)
        val kinds = kindsById(segments)
        val animations =
            layout.boxes.map { box ->
                SegmentAnimation(
                    id = box.id,
                    string = strings[box.id] ?: "",
                    kind = kinds[box.id],
                    role = SegmentRole.PERSIST,
                    source = LayoutSource.NEW,
                    box = box,
                    from = SegmentState.Resting,
                    to = SegmentState.Resting,
                    fadeWindow = TimeWindow.Whole,
                    scaleOrigin = box.centre,
                )
            }
        return MorphPlan.still(
            segments = animations,
            width = layout.width,
            height = layout.height,
            lineCount = layout.lineCount,
            slideDistance = layout.slideDistance,
        )
    }

    internal fun container(input: MorphPlanInput): ContainerAnimation =
        ContainerAnimation(
            width = ContainerAxis(input.oldLayout.width, input.newLayout.width, input.curve),
            height = ContainerAxis(input.oldLayout.height, input.newLayout.height, input.curve),
            holdsOldSize = input.isEmptyTransition,
        )

    /** What a segment already on screen was doing, or nothing. */
    internal fun carriedStart(
        input: MorphPlanInput,
        id: String,
    ): SegmentState = input.carried[id] ?: SegmentState.Resting

    /** The centre each run shares, restated for every member of it. */
    internal fun centres(
        runs: List<List<Int>>,
        boxes: List<SegmentBox>,
    ): Map<Int, MorphPoint> {
        val out = HashMap<Int, MorphPoint>()
        for (run in runs) {
            val members = run.map { boxes[it] }
            val first = members.firstOrNull() ?: continue
            var left = first.x
            var right = first.x + first.width
            var top = first.y
            var bottom = first.y + first.height
            for (box in members.drop(1)) {
                left = minOf(left, box.x)
                right = maxOf(right, box.x + box.width)
                top = minOf(top, box.y)
                bottom = maxOf(bottom, box.y + box.height)
            }
            val centre = MorphPoint((left + right) / 2, (top + bottom) / 2)
            for (index in run) out[index] = centre
        }
        return out
    }

    internal fun stringsById(segments: List<Segment>): Map<String, String> =
        segments.associate { it.id to it.string }

    internal fun kindsById(segments: List<Segment>): Map<String, SegmentKind> =
        segments.mapNotNull { segment -> segment.kind?.let { segment.id to it } }.toMap()
}
