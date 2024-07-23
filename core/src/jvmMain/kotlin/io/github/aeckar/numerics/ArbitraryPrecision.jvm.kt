@file:JvmName("ArbitraryPrecision")

package io.github.aeckar.numerics

import io.github.aeckar.numerics.utils.ScaledLong
import java.math.BigDecimal
import java.math.BigInteger
import java.nio.ByteBuffer

/**
 * Returns a scaled, 64-bit integer equal to the absolute value of [value].
 */
private fun ScaledLong(value: BigInteger): ScaledLong {
    var int = value.abs()
    var scale = 0
    while (int > Long.MAX_VALUE.toBigInteger()) {
        // Realistically, we could count up to a certain number of bits, but this way is more accurate
        int /= BigInteger.TEN
        ++scale
    }
    return ScaledLong(int.longValueExact(), scale)
}

// ------------------------------ 128-bit integer functions ------------------------------

/**
 * Returns true if the given value can fit within a 128-bit integer.
 *
 * If the value contains any decimal digits, they are disregarded.
 * @see Int128.MIN_VALUE
 * @see Int128.MAX_VALUE
 */
public fun isInt128(value: BigDecimal): Boolean =
    value in Int128.MIN_VALUE.toBigDecimal()..Int128.MAX_VALUE.toBigDecimal()

/**
 * Returns true if the given value can fit within a 128-bit integer.
 * @see Int128.MIN_VALUE
 * @see Int128.MAX_VALUE
 */
public fun isInt128(value: BigInteger): Boolean =
    value in Int128.MIN_VALUE.toBigInteger()..Int128.MAX_VALUE.toBigInteger()

/**
 * Compares this value to the other.
 *
 * See [compareTo][Comparable.compareTo] for details.
 */
@JvmName("compare")
public operator fun Int128.compareTo(value: BigDecimal): Int = toBigDecimal().compareTo(value)

/**
 * Compares this value to the other.
 *
 * See [compareTo][Comparable.compareTo] for details.
 */
@JvmName("compare")
public operator fun Int128.compareTo(value: BigInteger): Int = toBigInteger().compareTo(value)

/**
 * Returns an arbitrary-precision decimal equal in value to this.
 */
public fun Int128.toBigDecimal(): BigDecimal = toBigInteger().toBigDecimal()

/**
 * Returns an arbitrary-precision integer equal in value to this.
 */
public fun Int128.toBigInteger(): BigInteger {
    val value = ByteBuffer.allocate(Int128.SIZE_BYTES).apply { putInt(q1); putInt(q2); putInt(q3); putInt(q4) }.array()
    return BigInteger(value)
}

/**
 * Returns a 128-bit integer equal to the given arbitrary-precision number.
 *
 * Any decimal digits are truncated during conversion.
 * @throws ArithmeticException [value] is too large to be represented as an Int128
 */
@JvmName("toInt128")
public fun Int128(value: BigDecimal): Int128 = Int128(value.toBigInteger())

/**
 * Returns a 128-bit integer equal to the given arbitrary-precision integer.
 *
 * @throws ArithmeticException [value] is too large to be represented as an Int128
 */
@JvmName("toInt128")
public fun Int128(value: BigInteger): Int128 {
    var bytes = value.toByteArray()
    val maxBytes = Int128.SIZE_BYTES
    if (bytes.size > maxBytes) {
        Int128.raiseOverflow(value.toString())
    }
    if (bytes.size != maxBytes) {
        val padding = maxBytes - bytes.size
        bytes = bytes.copyInto(ByteArray(maxBytes), padding)
        val blank = Int128.blank(value.signum() or 1 /* if zero */)
        repeat(padding) { bytes[it] = blank.toByte() }
    }
    val parts = IntArray(4).apply(ByteBuffer.wrap(bytes).asIntBuffer()::get)
    return Int128(parts[0], parts[1], parts[2], parts[3])
}

// ------------------------------ rational number functions ------------------------------

/**
 * Returns true if the given value can fit within a [Rational].
 * @see Rational.MIN_VALUE
 * @see Rational.MAX_VALUE
 */
public fun isRational(value: BigDecimal): Boolean =
    value in Rational.MIN_VALUE.toBigDecimal()..Rational.MAX_VALUE.toBigDecimal()

/**
 * Returns true if the given value can fit within a [Rational].
 * @see Rational.MIN_VALUE
 * @see Rational.MAX_VALUE
 */
public fun isRational(value: BigInteger): Boolean =
    value in Rational.MIN_VALUE.toBigInteger()..Rational.MAX_VALUE.toBigInteger()

/**
 * Compares this value to the other.
 *
 * See [compareTo][Comparable.compareTo] for details.
 */
@JvmName("compare")
public operator fun Rational.compareTo(value: BigDecimal): Int = toBigDecimal().compareTo(value)

/**
 * Compares this value to the other.
 *
 * See [compareTo][Comparable.compareTo] for details.
 */
@JvmName("compare")
public operator fun Rational.compareTo(value: BigInteger): Int = toBigInteger().compareTo(value)

/**
 * Returns an arbitrary-precision decimal equal in value to this.
 *
 * Information may be lost during conversion.
 */
public fun Rational.toBigDecimal(): BigDecimal {
    return numer.toBigDecimal().setScale(-scale) / denom.toBigDecimal() * sign.toBigDecimal()
}

/**
 * Returns an arbitrary-precision integer equal in value to this.
 *
 * Information may be lost during conversion.
 */
public fun Rational.toBigInteger(): BigInteger = numer.toBigInteger() * BigInteger.TEN.pow(scale) * sign.toBigInteger()

/**
 * Returns a rational number equal to the given arbitrary-precision number.
 *
 * Some information may be lost on conversion.
 */
@JvmName("toRational")
public fun Rational(value: BigDecimal): Rational {
    val whole = value.toBigInteger()
    val (unscaledWhole, wholeScale) = ScaledLong(whole)
    val rawFracScale: Int
    val frac = (value - whole.toBigDecimal())
        .also { rawFracScale = it.scale() /* < 0 */ }
        .setScale(rawFracScale - rawFracScale.coerceAtLeast(-19 /* = -log10(Long.MAX_SIZE) */))
    val (unscaledFrac, fracScale) = ScaledLong(frac.toBigInteger())
    return Rational(unscaledWhole, 1L, wholeScale, 1) + Rational(unscaledFrac, 1L, -fracScale, value.signum() or 1)
}

/**
 * Returns a rational number equal to the given arbitrary-precision integer.
 *
 * Some information may be lost on conversion.
 */
@JvmName("toRational")
public fun Rational(value: BigInteger): Rational {
    val (numer, scale) = ScaledLong(value)
    return Rational(numer, 1L, scale, value.signum() or 1)
}