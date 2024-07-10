@file:JvmName("Rationals")
package io.github.aeckar.numerics

import io.github.aeckar.numerics.functions.exp
import io.github.aeckar.numerics.functions.floor
import io.github.aeckar.numerics.functions.ln
import io.github.aeckar.numerics.utils.*
import io.github.aeckar.numerics.utils.productSign
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.*
import kotlin.math.absoluteValue
import kotlin.random.Random



/**
 * Returns a rational number equal to this value over the other as a fraction after simplification.
 * @throws CompositeOverflowException [other] is 0 or the value is too large or small to be represented accurately
 */
@JvmName("newInstance")
public infix fun Int.over(other: Int): Rational = Rational(this.toLong(), other.toLong(), 0)

/**
 * Returns a rational number equal to this value over the other as a fraction after simplification.
 *
 * If this value is [Long.MIN_VALUE], the value stored will be equal to −2^63 + 8.
 * @throws CompositeOverflowException [other] is 0 or the value is too large or small to be represented accurately
 */
@JvmName("newInstance")
@Suppress("unused")
public infix fun Long.over(other: Long): Rational = Rational(this, other, 0)

/**
 * Returns a rational number equal to this value.
 *
 * For numbers that are known to be smaller than -1 or larger than 10,
 * the `Long`-args constructor should be used instead.
 */
@JvmName("instanceOf")
public fun Int.toRational(): Rational = when (this) {
    -1 -> Rational.NEGATIVE_ONE
    0 -> Rational.ZERO
    1 -> Rational.ONE
    2 -> Rational.TWO
    else -> this over 1
}

/**
 * See [toRational] for details.
 */
@JvmName("instanceOf")
public fun Long.toRational(): Rational = when (this) {
    -1L -> Rational.NEGATIVE_ONE
    0L -> Rational.ZERO
    1L -> Rational.ONE
    2L -> Rational.TWO
    else -> this over 1
}

/**
 * Returns a random rational number.
 */
public fun Random.nextRational(): Rational {
    val sign = nextInt(-1, 2) or 1 /* if zero */
    return Rational(sign * nextLong(), nextLong(), nextInt())
}

/**
 * Returns a rational number equal in value to [x].
 */
@JvmName("newInstance")
public fun Rational(x: Int): Rational = Rational(x.toLong())

/**
 * Returns a rational number equal in value to [x].
 *
 * Some information may be lost after conversion.
 */
@JvmName("newInstance")
public fun Rational(x: Int128): Rational {
    val (numer, scale) = ScaledLong(x)
    return Rational(numer, 1, scale, x.sign)
}

/**
 * Returns a rational number with the given [numerator][numer] and [denominator][denom] after simplification.
 * @throws CompositeOverflowException [denom] is 0 or the value is too large or small to be represented accurately
 */
@JvmName("newInstance")
public fun Rational(numer: Long, denom: Long = 1, scaleAugment: Int = 0): Rational {
    return Rational.ONE.valueOf(numer, denom, scaleAugment)
}

/**
 * Returns a rational number with the [numerator][numer] and [denominator][denom] after simplification.
 *
 * Some information may be lost during conversion.
 * @throws CompositeOverflowException [denom] is 0 or the value is too large or small to be represented accurately
 */
@JvmName("newInstance")
public fun Rational(numer: Int128, denom: Int128, scaleAugment: Int = 0): Rational {
    val sign = productSign(numer.sign, denom.sign)  // May be mutated by abs()
    return Rational.ONE.valueOf(numer.abs(), denom.abs(), scaleAugment, sign) { "Instantiation" }
}

@JvmName("newInstance")
public fun Rational(s: String): Rational = Rational.parse(s)

/**
 * A rational number.
 *
 * Instances of this class are comprised of the following:
 * - 64-bit integer numerator
 * - 64-bit integer denominator
 * - 32-bit scalar, n, by which this value is multiplied by 10^n
 * - sign value, 1 or -1, by which this value is multiplied by
 *
 * The first three components will never be their minimum values.
 *
 * Avoids the performance impact of arbitrary-precision arithmetic, while
 * allowing all instances to be readily converted to their fractional form.
 *
 * All 64-bit integer values, aside from [Long.MIN_VALUE], can be stored without losing information.
 * Furthermore, all values are guaranteed to be accurate to at least 18 digits
 * before considering error accumulated through calls to multiple operations.
 */
@Serializable(with = Rational.Serializer::class)
@Suppress("EqualsOrHashCode")
public open class Rational internal constructor(
    numer: Long,
    denom: Long,
    scale: Int,
    sign: Int
) : CompositeNumber<Rational>() {
    /**
     * The numerator when this value is represented as a fraction.
     */
    public var numer: Long = numer
        protected set

    /**
     * The denominator when this value is represented as a fraction.
     */
    public var denom: Long = denom
        protected set

    /**
     * A non-zero scalar, n, by which this value is multiplied to 10^n.
     * 
     * API Note: Validation should be used to ensure this value never holds the value
     * of [Int.MIN_VALUE] to prevent incorrect operation results.
     */
    public var scale: Int = scale
        protected set

    final override var sign: Int = sign
        protected set

    final override val isNegative: Boolean get() = sign == -1
    final override val isPositive: Boolean get() = sign == 1

    /**
     * *kotlinx.serialization* serializer for rational numbers.
     */
    public object Serializer : KSerializer<Rational> {
        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Rational") {
            element<Long>("numer")
            element<Long>("denom")
            element<Int>("scale")
            element<Int>("sign")
        }

        override fun deserialize(decoder: Decoder): Rational = decoder.decodeStructure(descriptor) {
            var numer = 0L
            var denom = 0L
            var scale = 0
            var sign = 0
            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> numer = decodeLongElement(descriptor, 0)
                    1 -> denom = decodeLongElement(descriptor, 1)
                    2 -> scale = decodeIntElement(descriptor, 2)
                    3 -> sign = decodeIntElement(descriptor, 3)
                    CompositeDecoder.DECODE_DONE -> break
                    else -> error("Unexpected index: $index")
                }
            }
            Rational(numer, denom, scale, sign)
        }

        override fun serialize(encoder: Encoder, value: Rational) {
            encoder.encodeStructure(descriptor) {
                encodeLongElement(descriptor, 0, value.numer)
                encodeLongElement(descriptor, 1, value.denom)
                encodeIntElement(descriptor, 2, value.scale)
                encodeIntElement(descriptor, 3, value.sign)
            }
        }
    }

    // ---------------------------------------- mutability ----------------------------------------

    override fun immutable() = this

    override fun mutable() = MutableRational(this)

    final override fun uniqueMutable() = MutableRational(this)

    final override fun valueOf(other: Rational): Rational {
        return if (other !is MutableRational) other else with (other) { Rational(numer, denom, scale, sign) }
    }

    /**
     * Value function with delegation to by-property constructor.
     */
    internal open fun valueOf(numer: Long, denom: Long, scale: Int, sign: Int): Rational {
        if (numer == 0L) {  // Ensure Rational(0) == Rational(0)
            return ZERO
        }
        return Rational(numer, denom, scale, sign)
    }

    /**
     * Value function accepting a ratio of 128-bit integers.
     *
     * @throws CompositeUndefinedException [denom] is 0
     * @throws CompositeOverflowException the value is too large or small to be represented accurately
     */
    internal inline fun valueOf(
        numer: Int128,
        denom: Int128,
        scaleAugment: Int,
        sign: Int,
        crossinline additionalInfo: () -> String
    ): Rational {
        if (numer stateEquals Int128.ZERO) {   // Ensure Rational(0) == Rational(0)
            return valueOf(ZERO)
        }
        if (denom stateEquals Int128.ZERO) {
            raiseUndefined("Denominator cannot be zero (numer = $numer)")
        }

        val (unscaledNumer, numerScale) = ScaledLong(numer)
        val (unscaledDenom, denomScale) = ScaledLong(denom)
        try {
            if (addOverflowsValue(numerScale, denomScale)) {
                raiseOverflow()
            }
            var scale = numerScale - denomScale
            if (addOverflowsValue(scale, scaleAugment)) {
                raiseOverflow()
            }
            scale += scaleAugment
            val gcf = gcf(unscaledNumer, unscaledDenom)
            return valueOf(unscaledNumer / gcf, unscaledDenom / gcf, scale, sign)
        } catch (e: CompositeOverflowException) {
            raiseOverflow(additionalInfo())
        }
    }

    /**
     * Value function accepting a ratio of 64-bit integers.
     *
     * Returns a rational number with the given [numerator][numer] and [denominator][denom] after simplification.
     * @throws CompositeUndefinedException [denom] is 0
     * @throws CompositeOverflowException the value is too large or small to be represented accurately
     */
    internal fun valueOf(numer: Long, denom: Long, scaleAugment: Int): Rational {
        if (numer == 0L) {  // Logarithm of 0 is undefined
            return valueOf(ZERO)
        }
        if (denom == 0L) {
            raiseUndefined("Denominator cannot be zero")
        }
        val numerAbs = numer.absoluteValue
        val denomAbs = denom.absoluteValue
        val numerScale = scaleOf(numerAbs)
        val denomScale = scaleOf(denomAbs)
        if (addOverflowsValue(numerScale, denomScale)) {
            raiseOverflow()
        }
        var scale = numerScale - denomScale
        if (addOverflowsValue(scale, scaleAugment)) {
            raiseOverflow()
        }
        scale += scaleAugment
        val unscaledNumer = (numerAbs / tenPow(numerScale))
        val unscaledDenom = (denomAbs / tenPow(denomScale))
        val gcf = gcf(unscaledNumer, unscaledDenom)
        return valueOf(unscaledNumer / gcf, unscaledDenom / gcf, scale, productSign(numer, denom))
    }

    // ---------------------------------------- arithmetic ----------------------------------------

    /**
     * Multiplies the base-10 scalar to this number.
     */
    @Cumulative
    public fun scaleBy(scalar: Int): Rational {
        if (addOverflowsValue(scale, scalar)) {
            raiseOverflow("scalar $scalar overflows scale $scale")
        }
        return valueOf(numer, denom, scale + scalar, sign)
    }

    /**
     * Returns an instance equal to this when the numerator and denominator are swapped.
     */
    @Cumulative
    public fun reciprocal(): Rational = valueOf(denom, numer, -scale, sign)

    final override fun signum(): Int = if (numer == 0L) 0 else sign

    @Cumulative
    final override fun unaryMinus(): Rational = valueOf(numer, denom, scale, -sign)

    // a/b + c/d = (ad + bc)/bd
    @Cumulative
    final override fun plus(other: Rational): Rational {
        val scaleDiff = this.scale.toLong() - other.scale
        val alignedNumer: Int128
        val otherAlignedNumer: Int128
        var scaleAugment = this.scale
        if (scaleDiff < 0) {    // Must be compared separately since LONG_MIN_SCALE != -LONG_MAX_SCALE
            if (scaleDiff < LONG_MIN_SCALE) { // 10^n is not representable as Long, addition is negligible
                return if (this.isNegative) this else valueOf(other)
            }
            alignedNumer = MutableInt128(numer)
            otherAlignedNumer = MutableInt128(other.numer) */* = */ Int128(tenPow(-scaleDiff.toInt()))
        } else if (scaleDiff > 0) {
            if (scaleDiff > LONG_MAX_SCALE) {
                return if (this.isNegative) valueOf(other) else this
            }
            alignedNumer = MutableInt128(numer) */* = */ Int128(tenPow(scaleDiff.toInt()))  // FIXME overflows
            otherAlignedNumer = MutableInt128(other.numer)
            scaleAugment = other.scale
        } else {
            alignedNumer = MutableInt128(numer)
            otherAlignedNumer = MutableInt128(other.numer)
        }
        val ad = alignedNumer */* = */ Int128(other.denom)
        val bc = otherAlignedNumer */* = */ Int128(denom)
        val sign: Int
        val numer = if (this.sign == other.sign) {
            sign = if (this.isNegative) -1 else 1
            ad +/* = */ bc
        } else {
            val minuend = if (this.isNegative) bc else ad
            minuend -/* = */ if (minuend === bc) ad else bc
            sign = if (minuend.isNegative) -1 else 1
            minuend/* = */.abs()
        }
        val bd = denom times other.denom
        return valueOf(numer/* = */.abs(), bd/* = */.abs(), scaleAugment, sign) { "$this + $other" }
    }

    // a/b * c/d = ac/cd
    final override fun times(other: Rational): Rational {
        when {
            other.stateEqualsOne() -> return this
            other stateEquals NEGATIVE_ONE -> return -this
            this.stateEqualsOne() -> return other
            other.numer == 0L || this.numer == 0L -> return ZERO
        }
        val numer: Int128 = numer times other.numer
        val denom: Int128 = denom times other.denom
        if (addOverflowsValue(scale, other.scale)) {
            raiseOverflow("$this * $other")
        }
        val scale = scale + other.scale
        return valueOf(numer, denom, scale, productSign(sign, other.sign)) { "$this * $other" }
    }

    // a/b / c/d = a/b * d/c
    final override fun div(other: Rational): Rational = this * other.reciprocal()

    // a/b % c/d = a/b - floor(ad/bc) * c/d
    final override fun rem(other: Rational): Rational {
        val abs = abs()
        val otherAbs = other.abs()
        if (abs <= otherAbs) {
            return if (abs stateEquals otherAbs) ZERO else abs
        }
        return abs - floor(abs / otherAbs) * otherAbs
    }

    /**
     * Returns an instance approximately equal to this when raised to [power].
     */
    public fun pow(power: Rational): Rational {
        if (power.isLong()) {
            val long = power.toLong()
            if (long > Int.MIN_VALUE && long <= Int.MAX_VALUE) {
                return this.pow(long.toInt())
            }
        }
        return exp(power * ln(this))    // a^b = e^(b * ln a)
    }

    // (a/b)^k = a^k/b^k
    final override fun pow(power: Int): Rational {
        if (power == Int.MIN_VALUE) {   // Negation results in the same value
            raiseOverflow("$this ^ Int.MIN_VALUE")
        }
        return if (power < 0) powInt(-power).reciprocal() else powInt(power)
    }

    /**
     * Assumes [power] is non-negative.
     */
    private fun powInt(power: Int): Rational {
        if (power == 0 || this.stateEqualsOne()) {
            return ONE
        }
        if (power == 1) {
            return this
        }
        val pow = power.absoluteValue
        val startingNumer = numer
        val startingDenom = denom
        var numer = numer
        var denom = denom
        repeat(pow - 1) {   // Since both fractional components are positive or zero, sign is not an issue
            val lastNumer = numer
            val lastDenom = denom
            numer *= startingNumer
            denom *= startingDenom
            if (numer < lastNumer || denom < lastDenom) {   // Product overflows, perform widening
                return valueOf(
                    MutableInt128(lastNumer).pow(pow),
                    MutableInt128(lastDenom).pow(pow),
                    scale,
                    sign
                ) { "$this ^ $power" }
            }
        }
        return try {
            valueOf(numer * sign, denom, scale) // Long.MIN_VALUE is not power of 10, so negation can never overflow
        } catch (e: CompositeOverflowException) {
            raiseOverflow("$this ^ $power", e)
        }
    }

    // ---------------------------------------- comparison ----------------------------------------

    final override fun compareTo(other: Rational): Int {
        val scaleDiff = this.sign - other.sign
        when {
            sign != other.sign -> return sign.compareTo(other.sign)
            scaleDiff < LONG_MIN_SCALE -> return -1
            scaleDiff > LONG_MAX_SCALE -> return 1
            this stateEquals other -> return 0
        }
        // ...the digits (radix 10) of the two values may overlap
        return (this.immutable() - other).sign
    }

    final override fun hashCode(): Int {
        var hash = 7
        hash = 31 * hash + numer.hashCode()
        hash = 31 * hash + denom.hashCode()
        hash = 31 * hash + scale
        return hash * sign
    }

    /**
     * If [other] is guaranteed to be 1, call [stateEqualsOne] instead.
     */
    final override fun stateEquals(other: Rational): Boolean {
        return numer == other.numer && denom == other.denom && scale == other.scale && sign == other.sign
    }

    /**
     * Optimized to only perform one comparison.
     */
    private fun stateEqualsOne() = numer == denom && scale == 0

    final override fun isLong() = denom == 1L && scale >= 0 && scaleOf(numer) + scale <= 18

    // ---------------------------------------- conversion functions ----------------------------------------

    /**
     * Returns the string representation of this value in scientific notation.
     *
     * Performs rounding (half up) so that the returned value has at least [precision] significant figures.
     */
    public fun sciNotationString(precision: Int): String {
        require(precision > 0) { "Invalid precision ($precision <= 0)" }
        return decimalString(precision, repetend = null, sciNotation = true)
    }

    /**
     * Returns the string representation of this value as an unscaled fraction.
     */
    public fun fractionString(): String = buildString {
        if (sign == -1) {
            append('-')
        }
        append(numer)
        if (scale < 0) {
            repeat(-scale) { append('0') }
            append('/')
            append(denom)
        } else if (scale > 0) {
            append('/')
            append(denom)
            repeat(scale) { append('0') }
        }
    }

    /**
     * Returns the string representation of this value in decimal form.
     *
     * The returned string is not guaranteed to be a legal parameter to the `String`-arg pseudo-constructor.
     * Performs rounding (half up) so that the returned value has at least [precision] significant figures.
     */
    public fun decimalString(precision: Int): String {
        require(precision > 0) { "Invalid precision ($precision <= 0)" }
        return decimalString(precision, repetend = null, sciNotation = false)
    }

    /**
     * Returns the fully-evaluated string representation of this value in decimal form.
     *
     * The returned string is not guaranteed to be a legal parameter to the `String`-arg pseudo-constructor.
     * Unlike [decimalString], performs no rounding.
     * Any repeating decimals present are enclosed in parentheses.
     */
    public open fun decimalString(): String {
        return decimalString(Int.MAX_VALUE, repetend = StringBuilder(), sciNotation = false)
    }

    /**
     * Assumes [precision] is positive.
     */
    private fun decimalString(precision: Int, repetend: StringBuilder?, sciNotation: Boolean): String = buildString {
        fun beautifyDecimal(signLength: Int) {
            if (last() == '.') {
                setLength(length - 1)
            } else if (first() == '.') {
                insert(0, '0')
            }
            if (signLength + 1 < length && this[signLength + 1] != '.') {   // Keep single '0' and "0."
                deleteRange(signLength, indexOfFirst { it != '-' /* remove extras */ && it != '0' })
            }
        }

        fun roundUsing(digit: Char) {
            if (digit < '5') {
                return
            }
            var index = lastIndex
            while (index >= 0 && (this[index] == '9' || this[index] == '.' || this[index] == '-')) {
                if (this[index] == '9') {
                    this[index] = '0'
                }
                --index
            }
            if (index == -1) {
                append('1')
            } else {
                ++this[index]
            }
        }

        /**
         * Moves the decimal point to the right by [count] places.
         *
         * Iterates from left-to-right.
         */
        fun shiftDigitsLeft(shiftAmount: Int, start: Int) {
            var index = start
            val lastShift = shiftAmount - 1
            repeat(shiftAmount) {
                if (index == lastIndex && it != lastShift) {
                    append('0')
                }
                this[index] = this[index + 1]
                ++index
            }
            this[index] = '.'
        }

        /**
         * Moves the decimal point to the left by [count] places.
         *
         * Iterates from right-to-left.
         */
        fun shiftDigitsRight(shiftAmount: Int, start: Int) {
            var index = start
            val lastShift = shiftAmount - 1
            repeat(shiftAmount) {
                this[index] = this[index - 1]
                --index
                if (index == 0 && it != lastShift) {   // Never true for first iteration
                    ++index
                    insert(0, '0')
                }
            }
            this[index] = '.'
        }

        if (numer == 0L) {
            return "0"
        }
        val signLength = if (sign == -1) 1.also { append("-") } else 0
        val division = numer / denom
        var remainder = numer % denom
        val wholePart = division.toString()
        val wholeIsZero = division == 0L
        if (wholePart.length >= precision && ((remainder * 10) / denom) < 5 && !wholeIsZero) {    // No decimal part before scaling
            if (sciNotation) {
                val significand = wholePart.take(precision)
                val augmentedScale = scale + (wholePart.length - 1)
                append(wholePart.first())
                append('.')
                for (index in 1..significand.lastIndex) {
                    append(significand[index])
                }
                if (last() == '.') {
                    setLength(length - 1)
                }
                if (augmentedScale != 0) {
                    append('e')
                    append(augmentedScale)
                }
            } else {
                append(wholePart)
                val digit = getOrElse(signLength + precision) { '0' }
                val zeroCount = wholePart.length - precision
                setLength(length - zeroCount)
                roundUsing(digit)
                repeat(zeroCount) { append('0') }
                if (scale < 0) {
                    append('.')
                    shiftDigitsRight(-scale, start = lastIndex)
                    beautifyDecimal(signLength)
                } else if (scale > 0) {
                    repeat(scale) { append('0') }
                }
            }
            return@buildString
        }
        append(wholePart)
        append('.')
        val wholeCount = wholePart.length - wholeIsZero.toInt()
        var digitCount = wholeCount
        var repeatCount = 0
        var duplicateCount = 0
        var digit: Char
        var repeatIndex = -1
        while (remainder != 0L) {
            remainder *= 10
            digit = (remainder / denom).toInt().digitToChar()
            if (repetend != null) when {
                digitCount == wholeCount -> repetend.append(digit)    // First iteration

                digit == repetend[repeatCount] -> { // Decimal part may be a repeating sequence
                    duplicateCount = 0
                    ++repeatCount
                    if (repeatCount == repetend.length) {
                        delete(length - (repetend.length * 2 - 1), length)
                        append('(')
                        repeatIndex = lastIndex
                        append(repetend)
                        append(')')
                        break
                    }
                }

                digit == last() -> {    // Digit may be a part of repeating sequence
                    // ...repeatCount == 0
                    ++duplicateCount
                    if (duplicateCount == ACCURATE_DIGITS) {
                        delete(length - ACCURATE_DIGITS, length)
                        if (digit != '0') {
                            append('(')
                            repeatIndex = lastIndex
                            append(digit)
                            append(')')
                        }
                        break
                    }
                    repetend.append(digit)
                }

                else -> {
                    for (index in (length - repeatCount)..lastIndex) {  // Grow repetend
                        repetend.append(this[index])
                    }
                    duplicateCount = 0
                    repeatCount = 0
                    repetend.append(digit)
                }
            } else if (digitCount == precision) {
                roundUsing(digit)
                break
            }
            append(digit)
            remainder %= denom
            if (digitCount != 0 || digit != '0') {
                ++digitCount
            }
        }
        val dotIndex = signLength + wholePart.length
        if (sciNotation) {
            val leadingDigitIndex = indexOfFirst { it in '1'..'9' }
            val alignment = leadingDigitIndex - (dotIndex - (!wholeIsZero).toInt())
            val augmentedScale = scale - alignment
            if (alignment < 0) {
                shiftDigitsRight(-alignment, dotIndex)
            } else if (alignment > 0) {
                shiftDigitsLeft(alignment, dotIndex)
            }
            if (augmentedScale != 0) {
                beautifyDecimal(signLength)
                append('e')
                append(augmentedScale)
            }
            return@buildString
        }
        if (scale == 0) {
            if (last() == '.') {
                setLength(length - 1)
            }
            return@buildString
        }
        if (scale < 0) {
            shiftDigitsRight(-scale, dotIndex)
        } else {
            // ...scale > 0
            if (repetend != null && repeatIndex != -1) { // Contains repeating decimal
                // ...scale > 0
                deleteAt(repeatIndex)
                setLength(length - 1)   // Remove closed parenthesis
                val repeatSize = length - repeatIndex
                repetend.setLength(repeatSize)
                repeat(repeatSize) { repetend[it] = this[repeatIndex + ((scale + it) % repeatSize)] }
                shiftDigitsLeft(scale, dotIndex)
                deleteRange(dotIndex + scale, length)
                append(".(")
                append(repetend)
                append(')')
            } else {
                // ...scale > 0
                shiftDigitsLeft(scale, dotIndex)
            }
        }
        beautifyDecimal(signLength)
    }

    final override fun toInt(): Int = toLong().toInt()

    final override fun toLong(): Long = (numer / denom) * tenPow(scale) * sign

    final override fun toDouble(): Double = (numer.toDouble() / denom) * tenPow(scale) * sign

    final override fun toComplex(): Complex = Complex(this, ZERO)

    /**
     * Returns this instance.
     */
    final override fun toRational(): Rational = this

    /**
     * Returns a 128-bit integer equal in value to this.
     *
     * Some information may be lost during conversion.
     */
    final override fun toInt128(): Int128 {
        return if (scale < 0) {
            Int128.ZERO
        } else {
            ((MutableInt128(numer) * Int128.TEN.pow(scale)) / Int128(denom)).immutable()
        }
    }

    /**
     * Returns a string representation of this value, in terms of its components, in base-10.
     *
     * If the value of any property would otherwise have no effect on the value of
     * this composite numer as a whole, it is omitted from the returned string
     * (for example, if the [denominator][denom] is 1).
     *
     * When passed to the string constructor of the inheritor, creates an instance equal in value to this.
     * @see decimalString
     */
    final override fun toString(): String {
        var sign = if (sign != -1) "" else "-"
        var denom = if (denom == 1L) "" else "/$denom"
        val scale = if (scale == 0) "" else "e$scale"
        if (denom.isNotEmpty() && scale.isNotEmpty()) {
            sign += "("
            denom += ")"
        }
        return "$sign$numer$denom$scale"
    }

    public companion object {
        @JvmStatic public val NEGATIVE_ONE: Rational = ConstantRational(1, 1, 0, -1, "-1")
        @JvmStatic public val ZERO: Rational = ConstantRational(0, 1, 0, 1, "0")
        @JvmStatic public val ONE: Rational = ConstantRational(1, 1, 0, 1, "1")
        @JvmStatic public val HALF: Rational = ConstantRational(1, 2, 0, 1, "0.5")
        @JvmStatic public val TWO: Rational = ConstantRational(2, 1, 0, 1, "2")

        @JvmStatic public val E: Rational
                = ConstantRational(2718281828459045235, 1, -18, 1, "2.718281828459045235")
        @JvmStatic public val HALF_PI: Rational
                = ConstantRational(1570796326794896619, 1, -18, 1, "1.570796326794896619")
        @JvmStatic public val PI: Rational
                = ConstantRational(3141592653589793238, 1, -18, 1, "3.141592653589793238")
        @JvmStatic public val TWO_PI: Rational
                = ConstantRational(6283185307179586477, 1, -18, 1, "6.283185307179586477")

        @JvmStatic public val MIN_VALUE: Rational
                = ConstantRational(Long.MAX_VALUE, 1, Int.MAX_VALUE, -1, "-9223372036854775807e2147483647")
        @JvmStatic public val MAX_VALUE: Rational
                = ConstantRational(Long.MAX_VALUE, 1, Int.MAX_VALUE, 1, "9223372036854775807e2147483647")

        /**
         * The number of decimal digits a [Rational] is guaranteed to be accurate to.
         */
        public const val ACCURATE_DIGITS: Int = 19

        /**
         * The largest integer k where n * 10^k is guaranteed to fit within a 64-bit integer.
         *
         * Equal to 19, which is `-`[LONG_MIN_SCALE]` + 2`.
         */
        private const val LONG_MAX_SCALE = 19

        /**
         * The smallest integer k where n * 10^k is not equal to 0,
         * where n is guaranteed to fit in a 64-bit integer.
         *
         * Equal to -17, which is `-`[LONG_MAX_SCALE]` - 2`.
         */
        private const val LONG_MIN_SCALE = -17

        @Suppress("ConstPropertyName")
        private const val serialVersionUID = 1L

        @Volatile private var factorialCache = arrayOf(ONE, ONE, TWO)

        private class ConstantRational(
            numer: Long,
            denom: Long,
            scale: Int,
            sign: Int,
            override val stringLiteral: String
        ) : Rational(numer, denom, scale, sign), Constant {
            override fun decimalString() = stringLiteral
        }

        /**
         * Returns the factorial of [x] as a rational number.
         *
         * @throws ArithmeticException x is non-negative or the result overflows
         */
        @JvmStatic
        public fun factorial(x: Int): Rational {
            val lastFactorial = factorialCache.lastIndex
            if (x <= lastFactorial) {
                return factorialCache[x]
            }
            val factorialsNeeded = x - lastFactorial
            val cacheSize = x + 1
            val cache = factorialCache.copyInto(Array(cacheSize) { ZERO })
            repeat(factorialsNeeded) {
                val lastIndex = lastFactorial + it
                cache[lastIndex + 1] = cache[lastIndex] * Rational(lastIndex + 1)
            }
            factorialCache = cache
            return cache.last()
        }

        // ------------------------------ string conversion ------------------------------

        /**
         * Returns a rational number equal in value to the given string.
         *
         * A string is considered acceptable if it contains:
         * 1. Negative/positive sign *(optional)*
         *    - May be placed inside or outside parentheses
         * 2. Decimal numerator
         *    - A sequence of digits, `'0'..'9'`, optionally containing `'.'`
         *    - Leading and trailing zeros are allowed, but a single dot is not
         * 3. Denominator *(optional)*
         *    - `'/'`, followed by a decimal denominator in the same format as the numerator
         * 4. Exponent in scientific notation *(optional)*
         *    - `'e'` or `'E'`, followed by a signed integer
         *    - Value must be able to fit within 32 bits
         *
         * The decimal numerator and denominator may optionally be surrounded by a single pair of parentheses.
         * However, if an exponent is provided, parentheses are mandatory.
         *
         * The given string must be small enough to be representable and
         * not contain any extraneous characters (for example, whitespace).
         *
         * @throws CompositeFormatException [s] is in an incorrect format
         * @throws CompositeOverflowException the value cannot be represented accurately as a rational number
         */
        internal fun parse(s: String): Rational {
            fun parseExponent(view: StringView): Int {
                val sign = if (view.char() == '-') (-1).also { view.move(1) } else 1
                var exponent = 0L
                do {
                    exponent *= 10
                    exponent += try {
                        view.char().digitToInt()
                    } catch (e: StringIndexOutOfBoundsException) {  // Caught on first iteration
                        raiseIncorrectFormat("missing exponent value", s, e)
                    } catch (e: IllegalArgumentException) {
                        raiseIncorrectFormat("illegal character embedded in exponent value", s, e)
                    }
                    if (exponent > Int.MAX_VALUE) {
                        raiseOverflow()
                    }
                    view.move(1)
                } while (view.isWithinBounds())
                return exponent.toInt() * sign
            }

            if (s.isEmpty()) {
                raiseIncorrectFormat("empty string", s)
            }
            val view = StringView(s)
            var hasExplicitPositive = false
            var hasParentheses = false
            var sign = 1
            while (true) try {
                when (view.char()) {
                    '-' -> {
                        if (sign == -1 || hasExplicitPositive) {
                            raiseIncorrectFormat("illegal embedded sign character", s)
                        }
                        sign = -1
                        view.move(1)
                    }
                    '+' -> {
                        if (sign == -1 || hasExplicitPositive) {
                            raiseIncorrectFormat("illegal embedded sign character", s)
                        }
                        hasExplicitPositive = true
                        view.move(1)
                    }
                    '(' -> {
                        if (hasParentheses) {
                            raiseIncorrectFormat("illegal embedded open parenthesis", s)
                        }
                        hasParentheses = true
                        view.move(1)
                    }
                    '0' -> {
                        view.move(1)
                        if (view.isNotWithinBounds()) {
                            return ZERO
                        }
                        view.move(-1)
                    }
                    '1', '2', '3', '4', '5', '6', '7', '8', '9', '.', '/', 'e', 'E', ')' -> break
                    else -> raiseIncorrectFormat("illegal embedded character", s)
                }
            } catch (e: StringIndexOutOfBoundsException) {
                raiseIncorrectFormat("character expected", s, e)
            }
            val (numer, numerScale) = ScaledLong.parse(view, stop = "/eE)")
            var denom = 1L
            var denomScale = 0
            val hasDenom = view satisfies { it == '/' }
            if (hasDenom) {
                view.move(1)
                while (view satisfies { it == '0' }) {
                    view.move(1)
                }
                val denomWithScale = ScaledLong.parse(view, stop = "eE)")
                denom = denomWithScale.component1()
                denomScale = denomWithScale.component2()
            }
            if (hasParentheses) {
                if (!view.satisfies { it == ')' }) {
                    raiseIncorrectFormat("missing closing parenthesis", s)
                }
                view.move(1)
            }
            var scale = if (view satisfies { it == 'e' || it == 'E' }) {
                if (hasDenom && !hasParentheses) {
                    raiseIncorrectFormat("missing clarifying parentheses", s)
                }
                view.move(1)
                parseExponent(view)
            } else {
                0
            }
            if (addOverflowsValue(scale, numerScale)) {
                raiseOverflow()
            }
            scale += numerScale
            if (addOverflowsValue(scale, denomScale)) {
                raiseOverflow()
            }
            scale -= denomScale
            return Rational(numer, denom, scale, sign)
        }

        // ------------------------------ helpers ------------------------------

        /**
         * Resultant sign represented as 1 or -1.
         * @return the sign of the product/quotient of the two values
         */
        private fun productSign(x: Long, y: Long) = if ((x < 0L) == (y < 0L)) 1 else -1

        private fun gcf(x: Long, y: Long): Long {
            tailrec fun euclideanGCF(max: Long, min: Long): Long {
                val rem = max % min
                return if (rem == 0L) min else euclideanGCF(min, rem)
            }

            val max = maxOf(x, y)
            val min = minOf(x, y)
            return euclideanGCF(max, min)
        }

        /**
         * Returns true if the sum is the result of a signed integer overflow.
         *
         * If a result of multiple additions must be checked, this function must be called for each intermediate sum.
         * Also checks for the case [Int.MIN_VALUE] - 1.
         */
        private fun addOverflowsValue(x: Int, y: Int) = (x.toLong() + y) !in Int.MIN_VALUE..Int.MAX_VALUE
    }
}