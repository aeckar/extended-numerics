@file:JvmName("Functions")
@file:JvmMultifileClass
package io.github.aeckar.numerics.functions

import io.github.aeckar.numerics.*
import io.github.aeckar.numerics.Rational.Companion.TWO_PI
import io.github.aeckar.numerics.functions.signallingFactorial as factorial

/**
 * Returns -1 to the power of [n].
 *
 * Describes an alternating series.
 */
private fun sign(n: Int): Int128 = if (n and 1 == 0) Int128.ONE else Int128.NEGATIVE_ONE

/**
 * Returns an approximation of an elementary operation using the MacLaurin series (Taylor series at a = 0).
 *
 * Example: sin(x) =
 *
 *    (-1)^n * x^(2n+1)   -> +1 means pow0=1
 *    ----------------- = x - x^3/3! + x^5/5! ...
 *         (2n+1)!
 *
 * where pow0 = 1 and powStep = 2
 *
 * @param termNumer numerator of the coefficient
 * @param termDenom denominator of the coefficient
 * @param powConstant the integer added to `kn` in the exponent
 * @param powCoefficient the integer, `k`, multiplied by `n` in the exponent
 */
@Cumulative
private /* noinline */ fun seriesApprox(
    x: Rational,
    termNumer: (n: Int) -> Int128,
    termDenom: (n: Int) -> Int128,
    powConstant: Int,
    powCoefficient: Int
): Rational {
    /*
         Approximation of Elementary Functions using the MacLaurin Series  

         Definitions:
            powCoefficient E {1, 2}
            powConstant E {0, 1}
            
            numerBase = x.numer^powCoefficient
            numerFactor = x.numer^powConstant                             
            denomBase = x.denom^powCoefficient
            denomFactor = x.denom^powConstant
            
         Proof:
            term =

             termNumer()     x.numer^(powCoefficient(n) + powConstant)
            ------------- * ------------------------------------------- =>
             termDenom()     x.denom^(powCoefficient(n) + powConstant)

            term =

             numerFactor * termNumer() * numerBase^n
            ------------------------------------------
             denomFactor * termDenom() * denomBase^n
     */

    val numerBase = Int128(x.numer).pow(powCoefficient)
    val denomBase = Int128(x.denom).pow(powCoefficient)
    val numerFactor: Int128
    val denomFactor: Int128
    if (powConstant == 0) {
        numerFactor = Int128.ONE
        denomFactor = Int128.ONE
    } else {    // powConstant == 1
        numerFactor = Int128(x.numer)
        denomFactor = Int128(x.denom)
    }

    // n = 0
    var numer = numerFactor * termNumer(0)
    var denom = denomFactor * termDenom(0)
    val result: Rational = MutableRational(numer, denom)
    val lastResult: Rational = MutableRational(Rational.ZERO)

    var n = 1   // Since 34! overflows a 128-bit integer, will never exceed 33.
    try {
        do {
            lastResult/* = */.valueOf(result)
            numer = numerFactor.mutable() */* = */ termNumer(n) */* = */ numerBase.pow(n)
            denom = denomFactor.mutable() */* = */ termDenom(n) */* = */ denomBase.pow(n)
            result +/* = */ Rational(numer, denom)  // Reduces 128-bit integers to scaled Longs whose values determine convergence
            ++n
        } while (!result.stateEquals(lastResult))
    } catch (_: FactorialOverflowSignal) { /* no-op */ }    // Result is close enough
    return result
}

// ------------------------------ exponentiation ------------------------------

// TODO create overloads for complex numbers for all of these functions

/**
 * Returns an instance approximately equal to the square root of [x].
 *
 * Unlike [Rational.pow], this function is cumulative.
 */
@Cumulative
public fun sqrt(x: Rational): Rational = exp(ln(x) * Rational.HALF)

/**
 * Returns an instance approximately equal to the natural logarithm of [x].
 */
@Cumulative
public fun ln(x: Rational): Rational {
    return try {
        seriesApprox(x - Rational.ONE,
            termNumer = { sign(it) },
            termDenom = { Int128(it + 1) },
            powConstant = 0,
            powCoefficient = 1
        )
    } catch (e: CompositeUndefinedException) {
        throw CompositeUndefinedException("Natural logarithm is undefined for non-positive numbers (x = $x)", e)
    }
}

/**
 * Returns an instance approximately equal to [e][Rational.E] raised to [x].
 *
 * Result is found using [seriesApprox] instead of [Rational.pow].
 */
@Cumulative
internal fun exp(x: Rational): Rational = seriesApprox(x,
    termNumer = { Int128.ONE },
    termDenom = { factorial(it) },
    powConstant = 0,
    powCoefficient = 1
)

// ------------------------------ trigonometry ------------------------------

/**
 * Returns an instance approximately equal to the sine of [x].
 */
@Cumulative
public fun sin(x: Rational): Rational = seriesApprox(x % TWO_PI,
    termNumer = { sign(it) },
    termDenom = { factorial(2 * it + 1) },
    powConstant = 1,
    powCoefficient = 2
)

/**
 * Returns an instance approximately equal to the cosine of [x].
 */
@Cumulative
public fun cos(x: Rational): Rational = seriesApprox(x % TWO_PI,
    termNumer = { sign(it) },
    termDenom = { factorial(2 * it) },
    powConstant = 0,
    powCoefficient = 2
)

/**
 * Returns an instance approximately equal to the tangent of [x].
 */
@Cumulative
public fun tan(x: Rational): Rational = sin(x) / cos(x)

// ------------------------------ hyperbolic trigonometry ------------------------------

/**
 * Returns an instance approximately equal to the hyperbolic sine of [x].
 */
@Cumulative
public fun sinh(x: Rational): Rational = seriesApprox(x % TWO_PI,
    termNumer = { Int128.ONE },
    termDenom = { factorial(2 * it + 1) },
    powConstant = 1,
    powCoefficient = 2
)

/**
 * Returns an instance approximately equal to the hyperbolic cosine of [x].
 */
@Cumulative
public fun cosh(x: Rational): Rational = seriesApprox(x % TWO_PI,
    termNumer = { Int128.ONE },
    termDenom = { Int128.TWO * factorial(it) },
    powConstant = 0,
    powCoefficient = 2
)

/**
 * Returns an instance approximately equal to the hyperbolic tangent of [x].
 */
@Cumulative
public fun tanh(x: Rational): Rational = sinh(x) / cosh(x)

// ------------------------------ inverse trigonometry ------------------------------

/**
 * Returns an instance approximately equal to the inverse sine of [x].
 */
@Cumulative
public fun arcsin(x: Rational): Rational = seriesApprox(x,
    termNumer = { factorial(2*it) },
    termDenom = {
        val square = Int128.TWO.pow(it) * factorial(it)
        square * square * Int128(2 * it + 1)
    },
    powConstant = 1,
    powCoefficient = 2
)

/**
 * Returns an instance approximately equal to the inverse cosine of [x].
 */
// pi - x does not matter, just use the result of the subtraction
// Special cases and shortcuts covered by elem func symbols in simplify()
@Cumulative
public fun arccos(x: Rational): Rational = Rational.HALF_PI - arcsin(x)

/**
 * Returns an instance approximately equal to the inverse tangent of [x].
 */
@Cumulative
public fun arctan(x: Rational): Rational = seriesApprox(x,
    termNumer = { sign(it) },
    termDenom = { Int128(2 * it + 1) },
    powConstant = 1,
    powCoefficient = 2
)