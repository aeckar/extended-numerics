@file:JvmName("Functions")
@file:JvmMultifileClass
package io.github.aeckar.numerics.functions

import io.github.aeckar.numerics.Rational
import io.github.aeckar.numerics.Rational.Companion.NEGATIVE_ONE
import io.github.aeckar.numerics.Rational.Companion.ONE
import io.github.aeckar.numerics.Rational.Companion.ZERO

/**
 * Returns the whole number closest to this value, rounding towards positive infinity.
 */
public fun ceil(x: Rational): Rational = with(x) {
    if (numer < denom) {
        return if (sign == -1) ZERO else ONE
    }
    val numer = if (sign == -1) (numer % denom) - (numer + denom) + denom else (numer + denom) - (numer % denom)
    return Rational(numer, denom, scale)
}

/**
 * Returns the whole number closest to this value, rounding towards negative infinity.
 */
public fun floor(x: Rational): Rational = with(x) {
    if (numer < denom) {
        return if (sign == -1) NEGATIVE_ONE else ZERO
    }
    val numer = if (sign == -1) (numer % denom) - numer - denom else numer - (numer % denom)
    return Rational(numer, denom, scale)
}