package io.github.dim971.textmorph.core

// The arithmetic upstream does, routed through one place.
//
// Here the indirection earns its keep immediately. V8 uses fdlibm for `pow`,
// `exp`, `sin`, `cos` and `log`, and `java.lang.Math` is explicitly allowed to
// be faster than exact: the platform documentation permits a one-ulp error and
// only requires semi-monotonicity. `java.lang.StrictMath` *is* fdlibm, and is
// therefore what V8 agrees with.
//
// One ulp is invisible in a position and decisive in `Spring.settlingDuration`,
// which returns an integer produced by a threshold crossing inside an
// accumulating loop: a last-bit difference moves the duration by a millisecond,
// and every fade window in the library is a fraction of that duration.
//
// The reference project reached the same conclusion for the same reason. The
// Swift side routes through libm, which currently agrees, and says so.

/** The subset of JavaScript's `Math` that this port uses. */
internal object JsMath {
    /** `Math.E` */
    const val E: Double = Math.E

    /** `Math.exp` */
    fun exp(x: Double): Double = StrictMath.exp(x)

    /** `Math.sin` */
    fun sin(x: Double): Double = StrictMath.sin(x)

    /** `Math.cos` */
    fun cos(x: Double): Double = StrictMath.cos(x)

    /** `Math.sqrt` */
    fun sqrt(x: Double): Double = StrictMath.sqrt(x)

    /** `Math.pow` */
    fun pow(
        x: Double,
        y: Double,
    ): Double = StrictMath.pow(x, y)

    /** `Math.ceil` */
    fun ceil(x: Double): Double = StrictMath.ceil(x)
}
