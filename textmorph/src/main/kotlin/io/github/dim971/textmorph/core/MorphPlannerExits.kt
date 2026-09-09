package io.github.dim971.textmorph.core

// Turning two layouts and a diff into a plan.
//
// This replaces upstream's flip.ts together with the orchestration half of
// text-morph/index.ts. See MorphPlanner.kt for the two directions that are load
// bearing.

/**
 * Which segments are going, what each recedes towards, and which of them form a
 * run long enough to recede as one shape.
 */
internal class Departures(
    val exiting: Set<Int>,
    val anchors: Map<Int, String>,
    val inARun: Set<Int>,
    val runCentres: Map<Int, MorphPoint>,
)

internal fun departures(input: MorphPlanInput): Departures {
    val newIds = input.newSegments.map { it.id }.toSet()
    val oldBoxes = input.oldLayout.boxes
    val oldIds = oldBoxes.map { it.id }

    val exiting = HashSet<Int>()
    for ((index, id) in oldIds.withIndex()) {
        if (id !in newIds) exiting.add(index)
    }

    val runs = MorphAnchors.replacedRuns(oldBoxes.indices.toList(), exiting)
    return Departures(
        exiting = exiting,
        anchors = MorphAnchors.exitingAnchors(oldIds, exiting, newIds),
        inARun = runs.flatten().toSet(),
        runCentres = MorphPlanner.centres(runs, oldBoxes),
    )
}

/**
 * The four things every exit needs, gathered so each shape below reads as one
 * statement.
 */
internal class ExitContext(
    val box: SegmentBox,
    val string: String,
    val kind: SegmentKind?,
    val start: SegmentState,
)

internal fun leaving(input: MorphPlanInput): List<SegmentAnimation> {
    val departures = departures(input)
    if (departures.exiting.isEmpty()) return emptyList()

    val oldBoxes = input.oldLayout.boxes
    val strings = MorphPlanner.stringsById(input.oldSegments)
    val kinds = MorphPlanner.kindsById(input.oldSegments)

    val out = ArrayList<SegmentAnimation>()
    for (index in oldBoxes.indices) {
        if (index !in departures.exiting) continue
        val box = oldBoxes[index]
        val context =
            ExitContext(
                box = box,
                string = strings[box.id] ?: "",
                kind = kinds[box.id],
                start = MorphPlanner.carriedStart(input, box.id),
            )

        val centre = departures.runCentres[index]
        if (centre != null && index in departures.inARun) {
            out.add(groupExit(context, centre))
            continue
        }

        // The forward delta: where the anchor went, so the segment follows the
        // text taking its place.
        val anchor = departures.anchors[index]
        val delta =
            if (anchor != null) {
                MorphAnchors.delta(input.newLayout.positions, input.oldLayout.positions, anchor)
            } else {
                Delta(0.0, 0.0)
            }

        out.add(
            if (context.kind == null) {
                textExit(context, delta, input.scale)
            } else {
                numberExit(context, delta, input.newLayout.slideDistance)
            },
        )
    }
    return out
}

/**
 * A whole run receding as one shape: no displacement at all, only a scale about
 * the centre the run shares.
 */
private fun groupExit(
    context: ExitContext,
    centre: MorphPoint,
): SegmentAnimation =
    SegmentAnimation(
        id = context.box.id,
        string = context.string,
        kind = context.kind,
        role = SegmentRole.GROUP_EXIT,
        source = LayoutSource.OLD,
        box = context.box,
        from = SegmentState(opacity = context.start.opacity),
        to = SegmentState(scale = MorphTiming.GROUP_SCALE, opacity = 0.0),
        fadeWindow = TimeWindow.fraction(MorphTiming.GROUP_EXIT_FADE),
        scaleOrigin = centre,
    )

/**
 * A digit or a symbol leaving: the slot follows the anchor, the character inside
 * it slides out along the block axis.
 */
private fun numberExit(
    context: ExitContext,
    delta: Delta,
    slideDistance: Double,
): SegmentAnimation =
    SegmentAnimation(
        id = context.box.id,
        string = context.string,
        kind = context.kind,
        role = SegmentRole.NUMBER_EXIT,
        source = LayoutSource.OLD,
        box = context.box,
        from = SegmentState(opacity = context.start.opacity),
        to =
            SegmentState(
                dx = delta.dx,
                dy = delta.dy,
                opacity = 0.0,
                moverDy = slideDistance,
            ),
        fadeWindow = TimeWindow.fraction(MorphTiming.NUMBER_EXIT_FADE),
        scaleOrigin = context.box.centre,
    )

private fun textExit(
    context: ExitContext,
    delta: Delta,
    scales: Boolean,
): SegmentAnimation =
    SegmentAnimation(
        id = context.box.id,
        string = context.string,
        kind = null,
        role = SegmentRole.EXIT,
        source = LayoutSource.OLD,
        box = context.box,
        from = SegmentState(opacity = context.start.opacity),
        to =
            SegmentState(
                dx = delta.dx,
                dy = delta.dy,
                scale = if (scales) MorphTiming.SEGMENT_SCALE else 1.0,
                opacity = 0.0,
            ),
        fadeWindow = TimeWindow.fraction(MorphTiming.EXIT_FADE),
        scaleOrigin = context.box.centre,
    )
