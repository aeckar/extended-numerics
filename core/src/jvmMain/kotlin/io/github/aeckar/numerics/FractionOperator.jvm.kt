package io.github.aeckar.numerics

/**
 * Returns a rational number equal to this value over the given [denominator][denom] after simplification.
 * @throws NumericUndefinedException [denom] is 0
 * @throws NumericOverflowException the value is too large or small to be represented accurately
 */
@JvmSynthetic
public infix fun Int.over(denom: Int): Rational = Rational(this, denom)

/**
 * Returns a rational number equal to this value over the given [denominator][denom] after simplification.
 * @throws NumericUndefinedException [denom] is 0
 * @throws NumericOverflowException the value is too large or small to be represented accurately
 */
@JvmSynthetic
public infix fun Long.over(denom: Long): Rational = Rational(this, denom)

/**
 * Returns a rational number equal to this value over the given [denominator][denom] after simplification.
 *
 * Some information may be lost during conversion.
 * @throws NumericOverflowException [denom] is 0 or the value is too large or small to be represented accurately
 */
@JvmSynthetic
public infix fun Int128.over(denom: Int128): Rational = Rational(this, denom)