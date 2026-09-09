package io.github.dim971.textmorph.core

import com.google.gson.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.ulp

/**
 * The spring solver, against the JavaScript original.
 *
 * This suite holds the only tolerance anywhere in the fixtures, on both sides,
 * and it is worth saying exactly what it is.
 *
 * [JsMath] is `StrictMath`, which is fdlibm, and V8's `Math` is a *port* of
 * fdlibm rather than fdlibm itself. Almost everywhere that comes to the same
 * bits: of the 357 sampled positions here, exactly one disagrees, and it was
 * traced rather than absorbed. At stiffness 100, damping 5, mass 1, sampled at
 * t = 0.60425, `cos(5.850625467364579)` is 0x3fed0d7b2dbe7bad in V8 and
 * 0x...bac under `StrictMath`: one ulp apart. `exp`, `sin` and `sqrt` agree
 * exactly at the same point, and `sqrt` is exactly rounded by IEEE 754 so it
 * always will.
 *
 * The bound is tied to one rather than to the value, and that is not laziness.
 * Every branch of the position function computes `1 - something`, so the result
 * inherits the absolute accuracy of a quantity near one however small the result
 * itself is. Bounding by the ulp of the value would be the wrong shape as well
 * as tighter than the arithmetic can honour.
 *
 * The duration is compared exactly on both sides. It is an integer produced by a
 * threshold crossing inside an accumulating loop, and every opacity window in the
 * library is a fraction of it, so a last-bit difference could in principle move a
 * whole millisecond and shift every fade.
 */
class SpringGoldenTest {
    private val cases = Fixtures.json("goldens").getAsJsonArray("spring").map { it.asJsonObject }

    @Test
    fun `stiffness, damping and mass resolve to the same frequency and ratio`() {
        val failures =
            cases.mapNotNull { case ->
                val stiffness = case.get("stiffness").asDouble
                val mass = case.get("mass").asDouble
                val omega0 = JsMath.sqrt(stiffness / mass)
                val zeta = case.get("damping").asDouble / (2 * JsMath.sqrt(stiffness * mass))
                if (omega0 == case.get("omega0").asDouble && zeta == case.get("zeta").asDouble) {
                    null
                } else {
                    "  ${describe(case)}: expected omega0 ${case.get("omega0")} " +
                        "zeta ${case.get("zeta")}, got $omega0 and $zeta"
                }
            }
        assertTrue(report(failures), failures.isEmpty())
    }

    @Test
    fun `the settling duration, to the millisecond`() {
        val failures =
            cases.filter { it.get("deviates").isJsonNull }.mapNotNull { case ->
                val expected = Fixtures.exactDouble(case.get("upstreamDuration"))
                val actual =
                    Spring.settlingDuration(
                        case.get("omega0").asDouble,
                        case.get("zeta").asDouble,
                        case.get("precision").asDouble,
                    )
                if (actual == expected) {
                    null
                } else {
                    "  ${describe(case)}: expected ${expected}ms, got ${actual}ms"
                }
            }
        assertTrue(report(failures), failures.isEmpty())
    }

    @Test
    fun `the position, sampled across the settling time, to within one ulp`() {
        val failures = ArrayList<String>()
        var worstUlps = 0.0
        var differing = 0
        var compared = 0

        for (case in cases) {
            val samples = case.get("samples")?.takeIf { !it.isJsonNull }?.asJsonArray ?: continue
            val duration = Fixtures.exactDouble(case.get("upstreamDuration"))
            for ((index, element) in samples.withIndex()) {
                val expected = element.asDouble
                val t = (index.toDouble() / 20) * (duration / 1000)
                val actual = Spring.position(t, case.get("omega0").asDouble, case.get("zeta").asDouble)
                compared += 1
                if (actual == expected) continue
                differing += 1
                val ulps = abs(actual - expected) / 1.0.ulp
                worstUlps = maxOf(worstUlps, ulps)
                if (ulps > 1) {
                    failures.add(
                        "  ${describe(case)} at t=$t: expected $expected, got $actual " +
                            "($ulps ulps of one, which is more than the one this allows)",
                    )
                }
            }
        }

        assertTrue(report(failures), failures.isEmpty())
        assertTrue(compared > 300)
        // Recorded, so a JDK that makes the agreement worse shows up here rather
        // than only in a wider tolerance.
        assertTrue("worst difference $worstUlps ulps of one", worstUlps <= 1)
        assertTrue(
            "$differing of $compared positions differ at all, which is more of " +
                "a gap between fdlibm and V8's port of it than was measured",
            differing <= 2,
        )
    }

    @Test
    fun `the default spring is the one upstream ships`() {
        val resolved = Spring.resolve(SpringParameters())
        // Measured, not derived: the exponential envelope suggests about
        // 1410ms, and the threshold crossing actually lands at 1271.
        assertEquals(1271.0, resolved.durationMs, 0.0)
        assertEquals(EasingCurve.SpringCurve(10.0, 0.5, 1271.0), resolved.curve)
    }

    @Test
    fun `critical damping works here, where upstream returns NaN and minus zero`() {
        val broken = cases.firstOrNull { !it.get("deviates").isJsonNull }
        assertNotNull("the fixture should record upstream's broken critical case", broken)
        checkNotNull(broken)

        // What upstream does, for the record.
        assertEquals(-0.0, Fixtures.exactDouble(broken.get("upstreamDuration")), 0.0)
        assertTrue(1.0 / Fixtures.exactDouble(broken.get("upstreamDuration")) < 0)
        assertEquals(1.0, broken.get("zeta").asDouble, 0.0)

        // What this port does instead: the analytic critical solution, which
        // rises monotonically to its target and settles in a sane time.
        val precision = broken.get("precision").asDouble
        val resolved =
            Spring.resolve(
                SpringParameters(
                    stiffness = broken.get("stiffness").asDouble,
                    damping = broken.get("damping").asDouble,
                    mass = broken.get("mass").asDouble,
                    precision = precision,
                ),
            )
        assertTrue(resolved.durationMs > 100)
        assertTrue(resolved.durationMs < 10000)

        var previous = -1.0
        for (step in 0..40) {
            val t = (step.toDouble() / 40) * (resolved.durationMs / 1000)
            val value = Spring.position(t, broken.get("omega0").asDouble, 1.0)
            assertTrue("the critical branch must not produce NaN", value.isFinite())
            assertTrue("critical damping does not overshoot or ring", value >= previous)
            assertTrue(value <= 1)
            previous = value
        }
        assertTrue(previous > 1 - precision)
    }

    @Test
    fun `a spring ignores the morph's own duration, and a bezier honours it`() {
        val spring = TextMorphEase.spring().resolve(400.0)
        assertEquals(1271.0, spring.durationMs, 0.0)

        val bezier = TextMorphEase.Default.resolve(400.0)
        assertEquals(400.0, bezier.durationMs, 0.0)
        assertEquals(EasingCurve.Bezier(CubicBezier.Default), bezier.curve)
    }

    private fun describe(case: JsonObject): String =
        "stiffness ${case.get("stiffness")} damping ${case.get("damping")} " +
            "mass ${case.get("mass")} precision ${case.get("precision")}"
}
