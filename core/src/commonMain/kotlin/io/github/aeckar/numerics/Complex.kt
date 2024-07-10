package io.github.aeckar.numerics

import io.github.aeckar.numerics.functions.sqrt

/**
 * A complex number.
 *
 * @param real the constant added to the imaginary term
 * @param imaginary the coefficient multiplied by the imaginary unit [i][I]
 */
public class Complex(public val real: Rational, public val imaginary: Rational) {
    /**
     * Returns the complex conjugate of this.
     * @return a complex number equal to this whose [imaginary][imaginary] part is negated
     */
    public fun conjugate(): Complex = Complex(real, -imaginary)

    /**
     * Returns the magnitude (absolute value) of this.
     */
    public fun magnitude(): Rational = sqrt(real.pow(2) + imaginary.pow(2))

    /**
     * Returns a complex number equal in value to this, negated.
     */
    public operator fun unaryMinus(): Complex = Complex(-real, -imaginary)

    /**
     * Returns a complex number equal in value to the sum.
     */
    public operator fun plus(other: Complex): Complex = Complex(real + other.real, imaginary + other.imaginary)

    /**
     * Returns a complex number equal in value to the difference.
     */
    public operator fun minus(other: Complex): Complex = this + (-other)

    /**
     * Returns a complex number equal in value to the product.
     */
    public operator fun times(other: Complex): Complex {
        val (ac, bd, ad, bc) = distribute(other)
        return Complex(ac - bd, ad + bc)
    }

    /**
     * Returns a complex number equal in value to the quotient.
     */
    public operator fun div(other: Complex): Complex {
        val (ac, bd, ad, bc) =  distribute(other)
        val denom = other.real.pow(2) + other.imaginary.pow(2)
        return Complex((ac + bd) / denom, (bc - ad) / denom)
    }

    /**
     * If this is (a + bi) and the other is (c + di), returns (ac, bd, ad, bc).
     */
    private fun distribute(other: Complex): Array<Rational> {
        return arrayOf(real * other.real, imaginary * other.imaginary, real * other.imaginary, imaginary * other.real)
    }

    public companion object {
        /**
         * The imaginary unit.
         */
        @JvmStatic public val I: Complex = Complex(Rational.ZERO, Rational.ONE)
    }
}