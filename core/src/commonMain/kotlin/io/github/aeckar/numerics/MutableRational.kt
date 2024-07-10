package io.github.aeckar.numerics

import io.github.aeckar.numerics.utils.productSign

internal fun MutableRational(numer: Int128, denom: Int128): MutableRational {
    val sign = productSign(numer.sign, denom.sign)  // May be mutated by abs()
    return Rational.ONE.mutable().valueOf(numer.abs(), denom.abs(), 0, sign) { "Instantiation" } as MutableRational
}

/**
 * A mutable rational number.
 *
 * See [Cumulative] for details on composite number mutability.
 */
internal class MutableRational(unique: Rational) : Rational(unique.numer, unique.denom, unique.scale, unique.sign) {
    override fun immutable() = Rational(numer, denom, scale, sign)

    @Cumulative
    override fun mutable() = this

    @Cumulative
    override fun valueOf(numer: Long, denom: Long, scale: Int, sign: Int) = this.also {
        it.numer = numer
        if (numer != 0L) {  // Ensure Rational(0) == Rational(0)
            it.denom = denom
            it.scale = scale
            it.sign = sign
        } else {
            it.denom = 1
            it.scale = 0
            it.sign = 0
        }
    }
}