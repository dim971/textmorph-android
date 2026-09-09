package io.github.dim971.textmorph.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The bezier and the carried curve, against the JavaScript original.
 *
 * Compared without tolerance. A bezier is multiplies and adds, so there is no
 * reason for two platforms to disagree, and the carried curve adds one `pow`,
 * which goes through StrictMath and therefore through fdlibm, which is what V8
 * uses.
 */
class EasingGoldenTest {
    private val easing = Fixtures.json("goldens").getAsJsonObject("easing")

    private val bases =
        mapOf(
            "default" to CubicBezier.Default,
            "linear" to CubicBezier.Linear,
            "ease" to CubicBezier.Ease,
        )

    @Test
    fun `every bezier, sampled at 101 points`() {
        val failures = ArrayList<String>()
        for (element in easing.getAsJsonArray("bezier")) {
            val case = element.asJsonObject
            val points = case.getAsJsonArray("points").map { it.asDouble }
            val curve = CubicBezier(points[0], points[1], points[2], points[3])
            val samples = case.getAsJsonArray("samples").map { it.asDouble }
            for ((index, expected) in samples.withIndex()) {
                val t = index.toDouble() / (samples.size - 1)
                val actual = curve.value(t)
                if (actual != expected) {
                    failures.add(
                        "  ${case.get("name").asString} at t=$t: expected $expected, " +
                            "got $actual (difference ${actual - expected})",
                    )
                }
            }
        }
        assertTrue(report(failures), failures.isEmpty())
    }

    @Test
    fun `the slope, which decides how much momentum an interrupted morph carries`() {
        val failures = ArrayList<String>()
        for (element in easing.getAsJsonArray("bezier")) {
            val case = element.asJsonObject
            val points = case.getAsJsonArray("points").map { it.asDouble }
            val curve = EasingCurve.Bezier(CubicBezier(points[0], points[1], points[2], points[3]))
            val expectations =
                listOf(
                    0.0 to case.get("slopeAtZero").asDouble,
                    0.5 to case.get("slopeAtHalf").asDouble,
                    1.0 to case.get("slopeAtOne").asDouble,
                )
            for ((t, expected) in expectations) {
                val actual = Easing.slope(curve, t)
                if (actual != expected) {
                    failures.add(
                        "  ${case.get("name").asString} slope at $t: " +
                            "expected $expected, got $actual",
                    )
                }
            }
        }
        assertTrue(report(failures), failures.isEmpty())
    }

    @Test
    fun `every carried curve, sampled at 21 points`() {
        val failures = ArrayList<String>()
        for (element in easing.getAsJsonArray("carry")) {
            val case = element.asJsonObject
            val base = bases[case.get("base").asString]
            if (base == null) {
                failures.add("  unknown base curve ${case.get("base").asString}")
                continue
            }
            val velocity = case.get("velocity").asDouble
            val carried = Easing.carry(EasingCurve.Bezier(base), velocity)
            val expectedK = Fixtures.exactDouble(case.get("k"))
            if (carried.k != expectedK) {
                failures.add(
                    "  ${case.get("base").asString} at velocity $velocity: " +
                        "k expected $expectedK, got ${carried.k}",
                )
                continue
            }
            val samples = case.getAsJsonArray("samples").map { it.asDouble }
            for ((index, expected) in samples.withIndex()) {
                val t = index.toDouble() / (samples.size - 1)
                val actual = carried.curve.value(t)
                if (actual != expected) {
                    failures.add(
                        "  ${case.get("base").asString} velocity $velocity at t=$t: " +
                            "expected $expected, got $actual (difference ${actual - expected})",
                    )
                }
            }
        }
        assertTrue(report(failures), failures.isEmpty())
    }

    @Test
    fun `the default curve starts fast, which is why most velocities carry nothing`() {
        val slope = Easing.slope(EasingCurve.Bezier(CubicBezier.Default), 0.0)
        assertTrue(slope > 5)
        // Momentum is only ever added. A box travelling slower than the curve
        // already leaves gets the curve unchanged.
        assertEquals(0.0, Easing.carry(EasingCurve.Bezier(CubicBezier.Default), 3.0).k, 0.0)
        assertTrue(Easing.carry(EasingCurve.Bezier(CubicBezier.Default), slope + 1).k > 0)
    }

    @Test
    fun `a velocity that could not be measured carries nothing`() {
        // Upstream relies on NaN propagating through Math.min and Math.max,
        // which Kotlin's do not do, so this is the guard that stands in for it.
        val base = EasingCurve.Bezier(CubicBezier.Default)
        for (velocity in listOf(Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY)) {
            val carried = Easing.carry(base, velocity)
            assertEquals(0.0, carried.k, 0.0)
            assertEquals(base, carried.curve)
        }
    }
}
