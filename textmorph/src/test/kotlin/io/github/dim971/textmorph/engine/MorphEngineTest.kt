package io.github.dim971.textmorph.engine

import io.github.dim971.textmorph.core.MorphAlignment
import io.github.dim971.textmorph.core.MorphCallbacks
import io.github.dim971.textmorph.core.MorphValue
import io.github.dim971.textmorph.core.SegmentState
import io.github.dim971.textmorph.core.ShapedLineMetrics
import io.github.dim971.textmorph.core.SyntheticMetrics
import io.github.dim971.textmorph.core.TextMorphOptions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/** A store that measures with the made-up monospace metric. */
internal class SyntheticStore : LineMeasuring {
    override fun metrics(text: String): ShapedLineMetrics = SyntheticMetrics.metrics(text)
}

/** Counts what the engine reported, so a test can assert on it. */
internal class CallbackLog {
    var started = 0
    var completed = 0
    var cancelled = 0

    val callbacks: MorphCallbacks =
        MorphCallbacks(
            onStart = { started += 1 },
            onComplete = { completed += 1 },
            onCancel = { cancelled += 1 },
        )

    /** Every morph should account for exactly one of these two. */
    val settled: Int get() = completed + cancelled
}

/**
 * The state machine, on a clock the test controls.
 *
 * The parts of upstream exercised here are the ones its own React wrapper does
 * not even reach: it never wires `onAnimationCancel`, so the interruption path
 * is unexercised there. That makes it the most likely place for the two ports to
 * be wrong in different ways, which is why the clock is a parameter and the
 * sequences are scripted. The iOS twin runs the same script.
 */
class MorphEngineTest {
    private fun engine(
        options: TextMorphOptions = TextMorphOptions.Default,
        reduceMotion: Boolean = false,
    ) = MorphEngine(SyntheticStore(), options, MorphAlignment.LEADING, reduceMotion)

    // MARK: the first value

    @Test
    fun `the first value arrives in place and reports nothing`() {
        val engine = engine()
        val log = CallbackLog()

        assertTrue(engine.update(MorphValue.Text("Total"), callbacks = log.callbacks, nowMs = 0.0))
        assertEquals("the first value is not a morph", 0, log.started)
        assertEquals(0, log.settled)

        val render = requireNotNull(engine.render(0.0))
        assertTrue(render.isSettled)
        for (state in render.frame.states) assertEquals(SegmentState.Resting, state)
        assertEquals(5 * SyntheticMetrics.ADVANCE, engine.size(0.0).width, 0.0)
    }

    @Test
    fun `an update that changes nothing is not a morph at all`() {
        val engine = engine()
        val log = CallbackLog()
        engine.update(MorphValue.Text("Total"), nowMs = 0.0)

        assertFalse(engine.update(MorphValue.Text("Total"), callbacks = log.callbacks, nowMs = 0.0))
        assertEquals("not even onStart, which is upstream's first line", 0, log.started)
        assertEquals(0, log.settled)
    }

    @Test
    fun `a number that formats to the same string is the same value`() {
        val engine = engine(TextMorphOptions(decimals = 0))
        engine.update(MorphValue.Number(1.4), nowMs = 0.0)
        assertEquals("1", engine.text)
        // Both round to the same string, so the second is not a change.
        assertFalse(engine.update(MorphValue.Number(1.2), nowMs = 0.0))
    }

    // MARK: one morph

    @Test
    fun `a morph reports its start once, and its completion once`() {
        val engine = engine()
        val log = CallbackLog()
        engine.update(MorphValue.Text("one"), nowMs = 0.0)

        engine.update(MorphValue.Text("one two"), callbacks = log.callbacks, nowMs = 0.0)
        assertEquals(1, log.started)
        assertEquals(0, log.settled)

        // Part way through: nothing has settled.
        engine.render(200.0)
        assertEquals(0, log.settled)

        // Past the end.
        val render = requireNotNull(engine.render(400.0))
        assertTrue(render.isSettled)
        assertEquals(1, log.completed)
        assertEquals(0, log.cancelled)

        // Drawing again does not report it twice.
        engine.render(1000.0)
        assertEquals(1, log.completed)
    }

    // MARK: interruption

    @Test
    fun `a morph replaced before it finished is cancelled, never completed`() {
        val engine = engine()
        val first = CallbackLog()
        val second = CallbackLog()
        engine.update(MorphValue.Text("one"), nowMs = 0.0)

        engine.update(MorphValue.Text("one two"), callbacks = first.callbacks, nowMs = 0.0)
        engine.render(100.0)

        engine.update(
            MorphValue.Text("one two three"),
            callbacks = second.callbacks,
            nowMs = 100.0,
        )
        assertEquals(1, first.cancelled)
        assertEquals(0, first.completed)
        assertEquals(1, second.started)

        engine.render(600.0)
        assertEquals(1, second.completed)
        // The first morph is still accounted for exactly once.
        assertEquals(1, first.settled)
    }

    @Test
    fun `twenty updates in a row account for every morph exactly once`() {
        val engine = engine()
        val logs = ArrayList<CallbackLog>()
        engine.update(MorphValue.Text("0"), nowMs = 0.0)

        // Eight milliseconds apart, which is faster than a frame at 120Hz and
        // far faster than the 400ms morph settles.
        for (step in 1..20) {
            val log = CallbackLog()
            logs.add(log)
            val now = step * 8.0
            engine.update(MorphValue.Number(step * 137.0), callbacks = log.callbacks, nowMs = now)
            engine.render(now)
        }

        // Let the last one finish.
        engine.render(10_000.0)

        for ((index, log) in logs.withIndex()) {
            assertEquals("morph $index should have started once", 1, log.started)
            assertEquals("morph $index should have settled exactly once", 1, log.settled)
        }
        // Every one but the last was replaced.
        assertTrue(logs.dropLast(1).all { it.cancelled == 1 })
        assertEquals(1, logs.last().completed)
    }

    @Test
    fun `a rapid run of values never makes a segment jump`() {
        val engine = engine()
        engine.update(MorphValue.Number(1000.0), nowMs = 0.0)

        var previous = emptyMap<String, SegmentState>()
        var worstJump = 0.0

        // Sampled every four milliseconds while the value changes every twenty,
        // so most frames fall inside a morph and some fall on the moment one
        // replaces another.
        for (step in 1..200) {
            val now = step * 4.0
            if (step % 5 == 0) engine.update(MorphValue.Number(1000 + step * 7.0), nowMs = now)
            val render = engine.render(now) ?: continue

            val current = HashMap<String, SegmentState>()
            for ((animation, state) in render.plan.segments.zip(render.frame.states)) {
                current[animation.id] = state
                val was = previous[animation.id] ?: continue
                worstJump = maxOf(worstJump, abs(state.dx - was.dx), abs(state.dy - was.dy))
            }
            previous = current
        }

        // One character of the synthetic font is ten units. A segment moving
        // more than a character between two frames four milliseconds apart is
        // the box crawling and then snapping, which is what carrying the state
        // across an interruption is for.
        assertTrue(
            "worst jump $worstJump, which is more than a character",
            worstJump < SyntheticMetrics.ADVANCE,
        )
    }

    @Test
    fun `segments already leaving keep their own clock`() {
        val engine = engine()
        engine.update(MorphValue.Text("alpha beta"), nowMs = 0.0)
        engine.update(MorphValue.Text("alpha"), nowMs = 0.0)

        // Interrupted a tenth of the way in, while "beta" is still leaving.
        engine.render(40.0)
        engine.update(MorphValue.Text("alpha gamma"), nowMs = 40.0)

        val during = requireNotNull(engine.render(40.0))
        assertTrue("the interrupted morph should still be drawn", during.ghosts.isNotEmpty())
        assertFalse(during.isSettled)

        // The ghost is dropped once its own plan has run its course.
        val after = requireNotNull(engine.render(2000.0))
        assertTrue(after.ghosts.isEmpty())
        assertTrue(after.isSettled)
    }

    // MARK: switched off

    @Test
    fun `disabled, a value arrives in place and reports nothing`() {
        val engine = engine(TextMorphOptions(disabled = true))
        val log = CallbackLog()
        engine.update(MorphValue.Text("one"), nowMs = 0.0)
        engine.update(MorphValue.Text("one two"), callbacks = log.callbacks, nowMs = 0.0)

        assertEquals(0, log.started)
        assertEquals("upstream fires neither callback on this path", 0, log.settled)
        assertEquals(7 * SyntheticMetrics.ADVANCE, engine.size(0.0).width, 0.0)

        // Still drawn, and still not moving. There is one rendering path, so a
        // still plan is how "draw this, not moving" is expressed.
        val render = requireNotNull(engine.render(0.0))
        assertTrue(render.isSettled)
        for (state in render.frame.states) assertEquals(SegmentState.Resting, state)
    }

    @Test
    fun `reduce motion is honoured, unless the caller says not to`() {
        assertTrue(engine(reduceMotion = true).isDisabled)
        assertFalse(
            engine(TextMorphOptions(respectReducedMotion = false), reduceMotion = true).isDisabled,
        )
    }

    @Test
    fun `coming back from disabled, the next value arrives without animating`() {
        // The segments on screen while disabled were never measured as part of a
        // morph, so a diff against them would animate from boxes that never
        // existed. Upstream resets for exactly this reason.
        val engine = engine(TextMorphOptions(disabled = true))
        engine.update(MorphValue.Text("one"), nowMs = 0.0)
        engine.update(MorphValue.Text("one two"), nowMs = 0.0)

        engine.configure(TextMorphOptions.Default, MorphAlignment.LEADING, reduceMotion = false)
        val log = CallbackLog()
        engine.update(MorphValue.Text("one two three"), callbacks = log.callbacks, nowMs = 0.0)

        assertEquals("the first value after re-enabling is a first value", 0, log.started)
        val render = requireNotNull(engine.render(0.0))
        assertTrue(render.isSettled)
        assertNotNull(render.plan)
    }
}
