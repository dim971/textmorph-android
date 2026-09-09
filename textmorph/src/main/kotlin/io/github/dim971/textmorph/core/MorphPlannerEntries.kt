package io.github.dim971.textmorph.core

// Turning two layouts and a diff into a plan.
//
// This replaces upstream's flip.ts together with the orchestration half of
// text-morph/index.ts. See MorphPlanner.kt for the two directions that are load
// bearing.

/**
 * Which segments have a box to be displaced from, and which of the ones that do
 * not form a run long enough to arrive as one shape.
 */
internal class Arrivals(
    val persisting: Set<String>,
    val arriving: Set<Int>,
    val inARun: Set<Int>,
    val runCentres: Map<Int, MorphPoint>,
)

internal fun arrivals(input: MorphPlanInput): Arrivals {
    val newBoxes = input.newLayout.boxes
    val newIds = newBoxes.map { it.id }
    val oldPositions = input.oldLayout.positions

    // A segment with a box in the old layout has somewhere to be displaced from;
    // one without has to borrow a neighbour's displacement.
    val persisting = newIds.filter { oldPositions.contains(it) }.toSet()
    val arriving = HashSet<Int>()
    for ((index, id) in newIds.withIndex()) {
        if (id !in persisting) arriving.add(index)
    }

    val runs = MorphAnchors.replacedRuns(newBoxes.indices.toList(), arriving)
    return Arrivals(
        persisting = persisting,
        arriving = arriving,
        inARun = runs.flatten().toSet(),
        runCentres = MorphPlanner.centres(runs, newBoxes),
    )
}

internal fun arrivingAndPersisting(input: MorphPlanInput): List<SegmentAnimation> {
    val newBoxes = input.newLayout.boxes
    val newIds = newBoxes.map { it.id }
    val oldPositions = input.oldLayout.positions
    val arrivals = arrivals(input)
    val strings = MorphPlanner.stringsById(input.newSegments)
    val kinds = MorphPlanner.kindsById(input.newSegments)

    val out = ArrayList<SegmentAnimation>()
    for (index in newBoxes.indices) {
        val box = newBoxes[index]
        // The stand-in that holds the line box open while a value empties is not
        // something the reader should see arrive.
        if (box.id == Segment.EMPTY_ID) continue

        val string = strings[box.id] ?: ""
        val kind = kinds[box.id]
        val isNew = index in arrivals.arriving
        val start = MorphPlanner.carriedStart(input, box.id)

        val centre = arrivals.runCentres[index]
        if (centre != null && index in arrivals.inARun) {
            out.add(
                SegmentAnimation(
                    id = box.id,
                    string = string,
                    kind = kind,
                    role = SegmentRole.GROUP_ENTER,
                    source = LayoutSource.NEW,
                    box = box,
                    from = SegmentState(scale = MorphTiming.GROUP_SCALE, opacity = 0.0),
                    to = SegmentState(opacity = 1.0),
                    fadeWindow = TimeWindow.fraction(MorphTiming.GROUP_ENTER_FADE),
                    scaleOrigin = centre,
                ),
            )
            continue
        }

        // The inverse delta, against the first-frame layout: the segment starts
        // where it used to be and animates to nothing. A segment with no previous
        // box of its own borrows a surviving neighbour's, looking backwards first
        // so it enters from the text already there.
        val anchorId =
            if (isNew) {
                MorphAnchors.nearestAnchor(
                    index,
                    newIds,
                    arrivals.persisting,
                    MorphAnchors.SearchOrder.BACKWARD_FIRST,
                )
            } else {
                box.id
            }
        val delta =
            if (anchorId != null) {
                MorphAnchors.delta(oldPositions, input.firstFrameLayout.positions, anchorId)
            } else {
                Delta(0.0, 0.0)
            }

        out.add(
            if (kind != null) {
                numberAnimation(
                    box,
                    string,
                    kind,
                    isNew,
                    delta,
                    start,
                    input.newLayout.slideDistance,
                )
            } else {
                textAnimation(box, string, isNew, delta, start)
            },
        )
    }
    return out
}

/**
 * A character of a number: the slot takes the displacement, the character inside
 * it takes the slide.
 *
 * Digits arrive from above and symbols from below, so each reads as its own event
 * rather than as the whole number shifting.
 */
private fun numberAnimation(
    box: SegmentBox,
    string: String,
    kind: SegmentKind,
    isNew: Boolean,
    delta: Delta,
    start: SegmentState,
    slideDistance: Double,
): SegmentAnimation {
    val slideFrom = if (kind == SegmentKind.DIGIT) -slideDistance else slideDistance
    return SegmentAnimation(
        id = box.id,
        string = string,
        kind = kind,
        role = if (isNew) SegmentRole.NUMBER_ENTER else SegmentRole.NUMBER_PERSIST,
        source = LayoutSource.NEW,
        box = box,
        from =
            SegmentState(
                dx = delta.dx + start.dx,
                dy = delta.dy + start.dy,
                opacity = if (isNew) 0.0 else start.opacity,
                moverDy = if (isNew) start.moverDy + slideFrom else 0.0,
            ),
        to = SegmentState(opacity = 1.0),
        fadeWindow =
            if (isNew) {
                TimeWindow.fraction(MorphTiming.NUMBER_ENTER_FADE)
            } else {
                TimeWindow.fraction(MorphTiming.PERSIST_FADE)
            },
        scaleOrigin = box.centre,
    )
}

private fun textAnimation(
    box: SegmentBox,
    string: String,
    isNew: Boolean,
    delta: Delta,
    start: SegmentState,
): SegmentAnimation {
    // A segment already fully opaque and merely moving needs no fade at all,
    // which is why upstream only animates opacity when it starts below one. Here
    // that is the window collapsing to nothing.
    val startOpacity = if (isNew) 0.0 else start.opacity
    return SegmentAnimation(
        id = box.id,
        string = string,
        kind = null,
        role = if (isNew) SegmentRole.ENTER else SegmentRole.PERSIST,
        source = LayoutSource.NEW,
        box = box,
        from =
            SegmentState(
                dx = delta.dx + start.dx,
                dy = delta.dy + start.dy,
                scale = if (isNew) MorphTiming.SEGMENT_SCALE else 1.0,
                opacity = startOpacity,
            ),
        to = SegmentState(opacity = 1.0),
        fadeWindow =
            when {
                isNew -> TimeWindow.fraction(MorphTiming.ENTER_FADE, MorphTiming.ENTER_FADE_DELAY)
                startOpacity < 1 -> TimeWindow.fraction(MorphTiming.PERSIST_FADE)
                else -> TimeWindow(0.0, 0.0)
            },
        scaleOrigin = box.centre,
    )
}
