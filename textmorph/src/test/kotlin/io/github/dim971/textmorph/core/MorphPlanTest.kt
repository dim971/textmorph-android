package io.github.dim971.textmorph.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The plan, and the function both ports sample.
 *
 * These are not fixtures of upstream behaviour, because upstream has no plan: it
 * writes keyframes and lets the browser interpolate them. What can be asserted is
 * that the plan says the same thing those keyframes say, and that the sampling
 * function is pure, total and lands exactly at rest. The timing table it is
 * checked against is [MorphTiming], which carries upstream's own numbers under
 * upstream's own names.
 *
 * Every assertion here is the iOS suite's, on the same synthetic metric, so the
 * two ports agreeing is a thing the machine checks rather than a thing this claims.
 */
class MorphPlanTest {
    // MARK: the first render

    @Test
    fun `a value appearing for the first time does not move`() {
        val segments = TextSegmenter.segmentText("Total balance")
        val layout = SyntheticMetrics.layout(segments)
        val plan = MorphPlanner.still(layout, segments)

        assertEquals(0.0, plan.durationMs, 0.0)
        assertEquals(layout.boxes.size, plan.segments.size)
        for (elapsed in listOf(-100.0, 0.0, 1.0, 400.0, 100_000.0)) {
            val frame = plan.frame(elapsed)
            assertTrue(frame.isFinished)
            assertEquals(layout.width, frame.width, 0.0)
            for (state in frame.states) assertEquals(SegmentState.Resting, state)
        }
    }

    // MARK: a segment that stays

    @Test
    fun `a segment that stays starts where it was and ends at rest`() {
        // "Total" survives and moves right by one word plus a space, which is
        // two characters of the synthetic font.
        val morph = SyntheticMorph(from = "Total x", to = "x Total")
        val total = requireNotNull(morph.animation("Total"))

        assertEquals(SegmentRole.PERSIST, total.role)
        assertEquals(LayoutSource.NEW, total.source)
        // Was at 0, will be at 20. The inverse delta is where it was minus
        // where it is going.
        assertEquals(2 * SyntheticMetrics.ADVANCE, total.box.x, 0.0)
        assertEquals(-2 * SyntheticMetrics.ADVANCE, total.from.dx, 0.0)
        assertEquals(0.0, total.to.dx, 0.0)
        assertEquals(1.0, total.from.scale, 0.0)
        assertEquals(1.0, total.from.opacity, 0.0)

        // Fully opaque and merely moving, so there is no fade at all.
        assertEquals(total.fadeWindow.start, total.fadeWindow.end, 0.0)

        val settled = requireNotNull(morph.state("Total", 400.0))
        assertEquals(SegmentState.Resting, settled)
    }

    // MARK: a segment that arrives

    @Test
    fun `a segment that arrives fades in over half the morph, a quarter late`() {
        val morph = SyntheticMorph(from = "one", to = "one two")
        val two = requireNotNull(morph.animation("two"))

        assertEquals(SegmentRole.ENTER, two.role)
        assertEquals(0.0, two.from.opacity, 0.0)
        assertEquals(1.0, two.to.opacity, 0.0)
        assertEquals(MorphTiming.SEGMENT_SCALE, two.from.scale, 0.0)
        assertEquals(1.0, two.to.scale, 0.0)
        assertEquals(MorphTiming.ENTER_FADE_DELAY, two.fadeWindow.start, 0.0)
        assertEquals(
            MorphTiming.ENTER_FADE_DELAY + MorphTiming.ENTER_FADE,
            two.fadeWindow.end,
            0.0,
        )

        // Still invisible through the delay, then linear to one.
        assertEquals(0.0, opacity(morph, "two", 0.0), 0.0)
        assertEquals(0.0, opacity(morph, "two", 100.0), 0.0)
        assertEquals(0.5, opacity(morph, "two", 200.0), 0.0)
        assertEquals(1.0, opacity(morph, "two", 300.0), 0.0)
        assertEquals(1.0, opacity(morph, "two", 400.0), 0.0)
    }

    // MARK: a segment that leaves

    @Test
    fun `a segment that leaves follows what takes its place, and fades early`() {
        val morph = SyntheticMorph(from = "one two", to = "one")
        val two = requireNotNull(morph.animation("two"))

        assertEquals(SegmentRole.EXIT, two.role)
        assertEquals(LayoutSource.OLD, two.source)
        assertEquals(SegmentState(), two.from)
        assertEquals(0.0, two.to.opacity, 0.0)
        assertEquals(MorphTiming.SEGMENT_SCALE, two.to.scale, 0.0)
        assertEquals(0.0, two.fadeWindow.start, 0.0)
        assertEquals(MorphTiming.EXIT_FADE, two.fadeWindow.end, 0.0)

        // Gone a quarter of the way in, which is earlier than an arriving
        // segment even starts: a character that has already left is a hole.
        assertEquals(1.0, opacity(morph, "two", 0.0), 0.0)
        assertEquals(0.5, opacity(morph, "two", 50.0), 0.0)
        assertEquals(0.0, opacity(morph, "two", 100.0), 0.0)
    }

    @Test
    fun `turning the scale off only affects what leaves`() {
        val with = SyntheticMorph(from = "one two", to = "one")
        val without = SyntheticMorph(from = "one two", to = "one", scale = false)
        assertEquals(MorphTiming.SEGMENT_SCALE, with.animation("two")?.to?.scale)
        assertEquals(1.0, without.animation("two")?.to?.scale)

        // An arriving segment scales either way, which is upstream: the option
        // is only read on the exit path.
        val arriving = SyntheticMorph(from = "one", to = "one two", scale = false)
        assertEquals(MorphTiming.SEGMENT_SCALE, arriving.animation("two")?.from?.scale)
    }

    // MARK: numbers

    @Test
    fun `a digit arrives from above and a symbol from below`() {
        val morph = SyntheticMorph(from = "1", to = "1.5")
        val slide = morph.plan.slideDistance
        assertEquals(SyntheticMetrics.ASCENT + SyntheticMetrics.DESCENT, slide, 0.0)

        val entering = morph.animations(SegmentRole.NUMBER_ENTER)
        assertTrue(entering.isNotEmpty())
        for (animation in entering) {
            assertEquals(0.0, animation.to.moverDy, 0.0)
            when (animation.kind) {
                SegmentKind.DIGIT -> assertEquals(-slide, animation.from.moverDy, 0.0)
                SegmentKind.SYMBOL -> assertEquals(slide, animation.from.moverDy, 0.0)
                null -> error("a number's character must carry a kind")
            }
            assertEquals(MorphTiming.NUMBER_ENTER_FADE, animation.fadeWindow.end, 0.0)
        }
    }

    @Test
    fun `a digit leaving slides out and fades over nearly half the morph`() {
        val morph = SyntheticMorph(from = "1.5", to = "1")
        val leaving = morph.animations(SegmentRole.NUMBER_EXIT)
        assertTrue(leaving.isNotEmpty())
        for (animation in leaving) {
            assertEquals(0.0, animation.from.moverDy, 0.0)
            // Out along the block axis, whichever kind it is: what differs is
            // the direction they arrive from, not the direction they go.
            assertEquals(morph.plan.slideDistance, animation.to.moverDy, 0.0)
            assertEquals(MorphTiming.NUMBER_EXIT_FADE, animation.fadeWindow.end, 0.0)
        }
    }

    @Test
    fun `a number's slot never scales`() {
        val morph = SyntheticMorph(from = "1,204", to = "1,318")
        val numbers = morph.plan.segments.filter { it.role.isNumber }
        assertTrue(numbers.isNotEmpty())
        for (animation in numbers) {
            assertEquals(1.0, animation.from.scale, 0.0)
            assertEquals(1.0, animation.to.scale, 0.0)
        }
    }

    // MARK: a run replaced as one shape

    @Test
    fun `a long enough run collapses towards its own centre and does not travel`() {
        // Nothing in common, and long enough on both sides.
        val morph = SyntheticMorph(from = "abcdefghij", to = "KLMNOPQRST", numbers = false)

        val leaving = morph.animations(SegmentRole.GROUP_EXIT)
        val arriving = morph.animations(SegmentRole.GROUP_ENTER)
        assertTrue(leaving.size >= MorphTiming.GROUP_MINIMUM)
        assertTrue(arriving.size >= MorphTiming.GROUP_MINIMUM)

        for (animation in leaving) {
            // No displacement at all: upstream's group animation replaces the
            // transform rather than composing with it.
            assertEquals(0.0, animation.from.dx, 0.0)
            assertEquals(0.0, animation.to.dx, 0.0)
            assertEquals(0.0, animation.from.dy, 0.0)
            assertEquals(0.0, animation.to.dy, 0.0)
            assertEquals(MorphTiming.GROUP_SCALE, animation.to.scale, 0.0)
            assertEquals(MorphTiming.GROUP_EXIT_FADE, animation.fadeWindow.end, 0.0)
        }
        for (animation in arriving) {
            assertEquals(MorphTiming.GROUP_SCALE, animation.from.scale, 0.0)
            assertEquals(1.0, animation.to.scale, 0.0)
            assertEquals(MorphTiming.GROUP_ENTER_FADE, animation.fadeWindow.end, 0.0)
        }

        // Every member of a run scales about the same point.
        assertEquals(1, leaving.map { it.scaleOrigin }.toSet().size)
        assertEquals(1, arriving.map { it.scaleOrigin }.toSet().size)
    }

    @Test
    fun `a run broken by a survivor is not a replacement`() {
        // The "x" survives in the middle, so neither half is long enough on its
        // own and every character animates individually. That survivor is right
        // there to move relative to.
        val morph = SyntheticMorph(from = "abcxdefg", to = "hijxklmn", numbers = false)
        assertTrue(morph.animations(SegmentRole.GROUP_EXIT).isEmpty())
        assertTrue(morph.animations(SegmentRole.GROUP_ENTER).isEmpty())
    }

    // MARK: sampling

    @Test
    fun `sampling is total, and lands exactly at rest`() {
        val morph = SyntheticMorph(from = "1,204 apples", to = "1,318 pears")

        for (elapsed in listOf(-1000.0, -1.0, 0.0, 1.0, 200.0, 399.0, 400.0, 401.0, 100_000.0)) {
            val frame = morph.plan.frame(elapsed)
            assertEquals(morph.plan.segments.size, frame.states.size)
            for (state in frame.states) {
                assertTrue(state.dx.isFinite())
                assertTrue(state.dy.isFinite())
                assertTrue(state.opacity >= 0)
                assertTrue(state.opacity <= 1)
            }
        }

        // At and past the end, everything that stays is exactly at rest and
        // exactly opaque. Not nearly: a segment left a hair off its box would
        // be measured as displaced by the next morph.
        val settled = morph.plan.frame(400.0)
        assertTrue(settled.isFinished)
        for ((animation, state) in morph.plan.segments.zip(settled.states)) {
            if (animation.role.isLeaving) continue
            assertEquals("${animation.id} is not at rest", SegmentState.Resting, state)
        }
    }

    @Test
    fun `opacity runs on linear time even when the curve does not`() {
        // The whole reason the clock stays linear. Under the default curve the
        // eased progress at the halfway point is far past half, and the fade
        // must not be.
        val morph = SyntheticMorph(from = "one", to = "one two")
        val eased = morph.plan.curve.value(0.5)
        assertTrue("the default curve is well past half at half time", eased > 0.8)

        // The window is [0.25, 0.75] of 400ms, so 300ms is exactly halfway
        // through it.
        assertEquals(1.0, opacity(morph, "two", 300.0), 0.0)
        assertEquals(0.5, opacity(morph, "two", 200.0), 0.0)
        assertEquals(0.25, opacity(morph, "two", 150.0), 0.0)
    }

    // MARK: the container

    @Test
    fun `the container's two axes run from the old size to the new one`() {
        val morph = SyntheticMorph(from = "one", to = "one two")
        assertEquals(3 * SyntheticMetrics.ADVANCE, morph.plan.container.width.from, 0.0)
        assertEquals(7 * SyntheticMetrics.ADVANCE, morph.plan.container.width.to, 0.0)
        assertEquals(morph.plan.container.height.from, morph.plan.container.height.to, 0.0)

        assertEquals(3 * SyntheticMetrics.ADVANCE, morph.plan.frame(0.0).width, 0.0)
        assertEquals(7 * SyntheticMetrics.ADVANCE, morph.plan.frame(400.0).width, 0.0)
    }

    @Test
    fun `an axis whose curve began earlier is already part way through it`() {
        // This is what keeps a fast counter's box from crawling: an axis whose
        // target has not moved resumes at the phase it had reached instead of
        // playing the opening sliver of the curve again.
        val curve = EasingCurve.Bezier(CubicBezier.Default)
        val fresh = ContainerAxis(0.0, 100.0, curve)
        val resumed = ContainerAxis(0.0, 100.0, curve, clockOffsetMs = 200.0)

        assertEquals(0.0, fresh.value(0.0, 400.0), 0.0)
        assertTrue(resumed.value(0.0, 400.0) > 80)
        assertEquals(100.0, resumed.value(400.0, 400.0), 0.0)
    }

    @Test
    fun `an emptying value holds its old box rather than collapsing`() {
        val segments = TextSegmenter.segmentText("something")
        val old = SyntheticMetrics.layout(segments)
        val stand = listOf(Segment(id = Segment.EMPTY_ID, string = "\u200B"))
        val new = SyntheticMetrics.layout(stand)

        val plan =
            MorphPlanner.plan(
                MorphPlanInput(
                    oldSegments = segments,
                    newSegments = stand,
                    oldLayout = old,
                    newLayout = new,
                    firstFrameLayout = new,
                    durationMs = 400.0,
                    curve = EasingCurve.Bezier(CubicBezier.Default),
                    isEmptyTransition = true,
                ),
            )

        // Held, not animated: a container collapsing to nothing takes the line
        // box with it and the segments still leaving would jump.
        assertEquals(old.width, plan.frame(0.0).width, 0.0)
        assertEquals(old.width, plan.frame(200.0).width, 0.0)
        assertEquals(old.width, plan.frame(400.0).width, 0.0)

        // The stand-in itself is never drawn.
        assertFalse(plan.segments.any { it.id == Segment.EMPTY_ID })
    }

    // MARK: overshoot

    @Test
    fun `overshoot is nothing for the default curve and real for the default spring`() {
        val bezier = SyntheticMorph(from = "a", to = "b", numbers = false)
        assertEquals(0.0, bezier.plan.overshoot, 0.0)

        val resolved = TextMorphEase.spring().resolve(400.0)
        val sprung =
            SyntheticMorph(
                from = "a",
                to = "b",
                durationMs = resolved.durationMs,
                curve = resolved.curve,
                numbers = false,
            )
        // A glyph clipped at the peak of its bounce is a bug that only appears
        // with springs, so the surface has to know about this.
        assertTrue(sprung.plan.overshoot > 0.1)
        assertTrue(sprung.plan.overshoot < 0.25)
    }

    // MARK: interruption

    @Test
    fun `an interrupted morph carries its position and opacity, not its scale`() {
        // Upstream reads exactly the translation and the opacity back off the
        // running animation, so a segment interrupted mid-shrink snaps to full
        // size and carries on from where it had travelled to.
        val carried =
            mapOf(
                "Total" to SegmentState(dx = 7.0, dy = 3.0, scale = 0.5, opacity = 0.4),
            )
        val morph = SyntheticMorph(from = "Total x", to = "x Total", carried = carried)
        val total = requireNotNull(morph.animation("Total"))

        assertEquals(-2 * SyntheticMetrics.ADVANCE + 7, total.from.dx, 0.0)
        assertEquals(3.0, total.from.dy, 0.0)
        assertEquals(0.4, total.from.opacity, 0.0)
        assertEquals("the scale restarts rather than being carried", 1.0, total.from.scale, 0.0)

        // Part way through a fade when it was interrupted, so it finishes the
        // fade over the persist window rather than not fading at all.
        assertEquals(MorphTiming.PERSIST_FADE, total.fadeWindow.end, 0.0)
    }

    private fun opacity(
        morph: SyntheticMorph,
        id: String,
        elapsed: Double,
    ): Double {
        val state = morph.state(id, elapsed)
        assertNotNull("$id has no animation in this plan", state)
        return checkNotNull(state).opacity
    }
}
