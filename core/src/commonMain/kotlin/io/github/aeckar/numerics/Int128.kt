@file:JvmName("Int128s")
package io.github.aeckar.numerics

import io.github.aeckar.numerics.functions.FactorialOverflowSignal
import io.github.aeckar.numerics.functions.signallingFactorial
import io.github.aeckar.numerics.utils.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.*
import kotlin.math.sign
import kotlin.random.Random

/**
 * Multiplies the two 64-bit integers and stores the result accurately within a 128-bit integer.
 */
internal infix fun Long.times(other: Long): MutableInt128 {
    fun multiplyValueOverflows(x: Long, y: Long, product: Long = x * y): Boolean {
        if (x == Long.MIN_VALUE && y == -1L || y == Long.MIN_VALUE && x == -1L) {
            return true
        }
        return x != 0L && y != 0L && x != product / y
    }
    val product = this * other
    if (multiplyValueOverflows(this, other, product)) {
        return (MutableInt128(this) * Int128(other)).mutable()
    }
    return MutableInt128(product)
}

/**
 * Returns a 128-bit integer with its lowest 32 bits equivalent to the given value.
 *
 * Performs the same widening conversion as a primitive type would.
 * As such, the sign of the original value is preserved.
 * If this value is associated with a reusable constant, that instance is returned instead.
 *
 * For numbers that are known to be smaller than -1 or larger than 16,
 * the `Int`-arg constructor should be used instead.
 */
@JvmName("instanceOf")
public fun Int.toInt128(): Int128 = when (this) {
    -1 -> Int128.NEGATIVE_ONE
    0 -> Int128.ZERO
    1 -> Int128.ONE
    2 -> Int128.TWO
    10 -> Int128.TEN
    16 -> Int128.SIXTEEN
    else -> Int128(this)
}

/**
 * See [toInt128] for details.
 */
@JvmName("instanceOf")
public fun Long.toInt128(): Int128 = when (this) {
    -1L -> Int128.NEGATIVE_ONE
    0L -> Int128.ZERO
    1L -> Int128.ONE
    2L -> Int128.TWO
    10L -> Int128.TEN
    16L -> Int128.SIXTEEN
    else -> Int128(this)
}

/**
 * Returns a random 128-bit integer.
 */
public fun Random.nextInt128(): Int128 {
    val mag = nextInt(1, 5)
    return Int128(
        nextInt(),
        if (mag > 1) 0 else nextInt(),
        if (mag > 2) 0 else nextInt(),
        if (mag == 4) 0 else nextInt()
    )
}

/**
 * Returns a 128-bit integer with its lowest 64 bits equivalent to the given value.
 *
 * Performs the same widening conversion as a primitive type would.
 * As such, the sign of the original value is preserved.
 */
@JvmName("newInstance")
public fun Int128(q3q4: Long): Int128 = Int128(q3q4, PrivateAPIFlag)

/**
 * Returns a 128-bit integer with its lowest 32 bits equivalent to the given value.
 *
 * Performs the same widening conversion as a primitive type would.
 * As such, the sign of the original value is preserved.
 *
 * For numbers that are known to be between -1 and 16, inclusive,
 * [Int.toInt128] should be used instead.
 */
@JvmName("newInstance")
public fun Int128(q4: Int): Int128 {
    val blank = Int128.blank(q4.sign)
    return Int128(blank, blank, blank, q4, PrivateAPIFlag)
}

/**
 * Returns a 128-bit integer with the specified bits.
 *
 * Each quarter consists of 32 bits.
 * If the highest bit of [q1] is 1, the returned value is [negative][Int128.isNegative].
 */
@JvmName("newInstance")
public fun Int128(q1: Int, q2: Int, q3: Int, q4: Int): Int128 = Int128(q1, q2, q3, q4, PrivateAPIFlag)

/**
 * Returns a 128-bit integer equal in value to the given string.
 *
 * A correctly formatted string contains only a sequence of digits agreeing with the given [radix].
 * Leading 0 digits are allowed.
 *
 * The given string must be small enough to be representable and
 * not contain any extraneous characters (for example, whitespace).
 * It may optionally be prefixed by a negative sign.
 *
 * @throws CompositeFormatException [s] is in an incorrect format
 * @throws CompositeOverflowException the value cannot be represented accurately as a 128-bit integer
 * @throws IllegalArgumentException [radix] is negative or over 36
 */
@JvmName("newInstance")
public fun Int128(s: String, radix: Int = 10, start: Int = 0, endExclusive: Int = s.length): Int128 {
    // Any radix above base-36 requires use of non-alphanumeric digits
    require(radix in 0..36) { "illegal radix" }
    return Int128.parse(s, radix, start, endExclusive).immutable()
}

/**
 * A 128-bit integer in two's complement format.
 *
 * Operations performed on instances of this class are analogous to those of built-in integer types, where:
 * - The most significant bit (of the [upper half][high]) determines the sign
 * - Result of division and remainder are truncated
 *
 * Unlike primitive types, operations will throw [CompositeOverflowException] on overflow or underflow.
 */
@Serializable(with = Int128.Serializer::class)
@Suppress("EqualsOrHashCode")
public open class Int128 : CompositeNumber<Int128> {
    public var q1: Int
        protected set
    public var q2: Int
        protected set
    public var q3: Int
        protected set
    public var q4: Int
        protected set

    final override val sign: Int get() = if (isNegative) -1 else 1

    final override val isNegative: Boolean get() = q1 < 0
    final override val isPositive: Boolean get() = q1 >= 0

    @Suppress("UNUSED_PARAMETER")
    internal constructor(q1: Int, q2: Int, q3: Int, q4: Int, privateAPIFlag: PrivateAPIFlag) : super() {
        this.q1 = q1
        this.q2 = q2
        this.q3 = q3
        this.q4 = q4
    }

    @Suppress("UNUSED_PARAMETER")
    internal constructor(q3q4: Long, privateAPIFlag: PrivateAPIFlag) {
        this.q3 = q3q4.high
        val blank = blank(q3.sign)
        this.q1 = blank
        this.q2 = blank
        this.q4 = q3q4.low
    }

    /**
     * *kotlinx.serialization* serializer for 128-bit integers.
     */
    public object Serializer : KSerializer<Int128> {
        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Int128") {
            element<Int>("q1")
            element<Int>("q2")
            element<Int>("q3")
            element<Int>("q4")
        }

        override fun deserialize(decoder: Decoder): Int128 = decoder.decodeStructure(descriptor) {
            var q1 = 0
            var q2 = 0
            var q3 = 0
            var q4 = 0
            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> q1 = decodeIntElement(descriptor, 0)
                    1 -> q2 = decodeIntElement(descriptor, 1)
                    2 -> q3 = decodeIntElement(descriptor, 2)
                    3 -> q4 = decodeIntElement(descriptor, 3)
                    CompositeDecoder.DECODE_DONE -> break
                    else -> error("Unexpected index: $index")
                }
            }
            Int128(q1, q2, q3, q4)
        }

        override fun serialize(encoder: Encoder, value: Int128) {
            encoder.encodeStructure(descriptor) {
                encodeIntElement(descriptor, 0, value.q1)
                encodeIntElement(descriptor, 1, value.q2)
                encodeIntElement(descriptor, 2, value.q3)
                encodeIntElement(descriptor, 3, value.q4)
            }
        }
    }

    private enum class DivisionType {
        QUOTIENT, REMAINDER, BOTH;

        @Suppress("UNCHECKED_CAST")
        inline fun <T : Any> result(quotient: () -> Int128, remainder: () -> Int128): T {
            return when (this@DivisionType) {
                QUOTIENT -> quotient()
                REMAINDER -> remainder()
                else -> quotient() to remainder()
            } as T
        }
    }

    // ---------------------------------------- mutability ----------------------------------------

    override fun immutable() = this

    @Cumulative
    override fun mutable() = MutableInt128(this)

    final override fun uniqueMutable() = MutableInt128(this)

    @Cumulative
    final override fun valueOf(other: Int128) = with (other) { this@Int128.valueOf(q1, q2, q3, q4) }

    /**
     * Value function with delegation to by-property pseudo-constructor.
     */
    @Cumulative
    protected open fun valueOf(q1: Int, q2: Int, q3: Int, q4: Int): Int128 = Int128(q1, q2, q3, q4)

    /**
     * Value function with delegation to `Long`-arg pseudo-constructor.
     */
    @Cumulative
    protected open fun valueOf(q3q4: Long): Int128 = Int128(q3q4)

    /**
     * If this is mutable, returns the given instance. Else, returns an immutable copy.
     *
     * Avoids defensive copying to an immutable instance for receivers performing intermediate operations.
     * Intended for use by non-cumulative operations only.
     */
    protected open fun out(operationResult: Int128): Int128 = operationResult.immutable()

    /**
     * Value function with delegation to fourth-quarter constructor.
     */
    @Cumulative
    internal fun valueOf(q4: Int) = valueOf(0, 0, 0, q4)

    // ---------------------------------------- partitions ----------------------------------------

    /**
     * Returns the first (most significant) 32 bits of this integer.
     */
    public operator fun component1(): Int = q1

    /**
     * Returns the second 32 bits of this integer.
     */
    public operator fun component2(): Int = q2

    /**
     * Returns the third 32 bits of this integer.
     */
    public operator fun component3(): Int = q3

    /**
     * Returns the fourth (least significant) 32 bits of this integer.
     */
    public operator fun component4(): Int = q4

    /**
     * The value of a quarter when there is no information stored in it.
     *
     * @return a value where all bits are 1 or 0 depending on whether [sign] is -1 or 1, respectively
     */
    private fun blank() = blank(this.sign)

    /**
     * Returns the number of consecutive least significant quarters which are not 0.
     * @return a value from 0 to 4
     */
    private fun magnitude() = when {
        q1 != 0 -> 4
        q2 != 0 -> 3
        q3 != 0 -> 2
        q4 != 0 -> 1
        else -> 0
    }

    /**
     * [q1] cast to a long as an unsigned integer.
     *
     * Does not preserve sign.
     */
    private fun q1w() = q1.widen()

    /**
     * [q2] cast to a long as an unsigned integer.
     *
     * Does not preserve sign.
     */
    private fun q2w() = q2.widen()

    /**
     * [q3] cast to a long as an unsigned integer.
     *
     * Does not preserve sign.
     */
    private fun q3w() = q3.widen()

    /**
     * [q4] cast to a long as an unsigned integer.
     *
     * Does not preserve sign.
     */
    private fun q4w() = q4.widen()

    // ---------------------------------------- bitwise operations ----------------------------------------

    /**
     * Performs a bitwise inversion on this value and returns the result.
     */
    @Suppress("unused")
    @Cumulative
    public fun inv(): Int128 = valueOf(q1.inv(), q2.inv(), q3.inv(), q4.inv())

    /**
     * Performs a bitwise left shift on this value and returns the result.
     *
     * If shifted by more than 128 bits, returns [zero][ZERO].
     * @throws IllegalArgumentException [bitCount] is negative
     */
    public infix fun shl(bitCount: Int): Int128 {
        ensureValidShift(bitCount)
        return leftShift(bitCount)
    }

    /**
     * Assumes [bitCount] is non-negative.
     */
    private fun leftShift(bitCount: Int): Int128 {
        /**
         * Returns the quarter at the current position after a left shift.
         */
        fun q(qCur: Int, qNext: Int, qShift: Int) = (qCur shl qShift) or (qNext ushr (Int.SIZE_BITS - qShift))

        if (bitCount == 0) {
            return this
        }
        if (bitCount >= 128) {
            return ZERO
        }
        val qShift = bitCount % Int.SIZE_BITS
        val qMove = bitCount / Int.SIZE_BITS
        return if (qShift != 0) when (qMove) {
            0 -> valueOf(q(q1, q2, qShift), q(q2, q3, qShift), q(q3, q4, qShift), q4 shl qShift)
            1 -> valueOf(q(q2, q3, qShift), q(q3, q4, qShift), q4 shl qShift, 0)
            2 -> valueOf(q(q3, q4, qShift), q4 shl qShift, 0, 0)
            else /* = 3 */ -> valueOf(q4 shl qShift, 0, 0, 0)
        } else when (qMove) {
            1 -> valueOf(q2, q3, q4, 0)
            2 -> valueOf(q3, q4, 0, 0)
            else /* = 3 */ -> valueOf(q4, 0, 0, 0)
        }
    }

    /**
     * Performs a bitwise signed right shift on this value and returns the result.
     */
    public infix fun shr(bitCount: Int): Int128 {
        ensureValidShift(bitCount)
        return rightShift(bitCount)
    }

    /**
     * Assumes [bitCount] is non-negative.
     */
    private fun rightShift(bitCount: Int): Int128 {
        if (bitCount == 0) {
            return this
        }
        val blank = blank()
        if (bitCount >= 128) {
            return valueOf(blank, blank, blank, blank)
        }
        val qShift = bitCount % Int.SIZE_BITS
        val qMove = bitCount / Int.SIZE_BITS
        return if (qShift != 0) when (qMove) {
            0 -> valueOf(q1 shr qShift, q(q1, q2, qShift), q(q2, q3, qShift), q(q3, q4, qShift))
            1 -> valueOf(blank, q1 shr qShift, q(q1, q2, qShift), q(q2, q3, qShift))
            2 -> valueOf(blank, blank, q1 shr qShift, q(q1, q2, qShift))
            else /* = 3 */ -> valueOf(blank, blank, blank, q1 shr qShift)
        } else when (qMove) {
            1 -> valueOf(blank, q1, q2, q3)
            2 -> valueOf(blank, blank, q1, q2)
            else /* = 3 */ -> valueOf(blank, blank, blank, q1)
        }
    }

    /**
     * Performs a bitwise unsigned right shift on this value and returns the result.
     */
    public infix fun ushr(bitCount: Int): Int128 {
        ensureValidShift(bitCount)
        return unsignedRightShift(bitCount)
    }

    /**
     * Assumes [bitCount] is non-negative.
     */
    private fun unsignedRightShift(bitCount: Int): Int128 {
        if (bitCount == 0) {
            return this
        }
        if (bitCount > 128) {
            return ZERO
        }
        val qShift = bitCount % Int.SIZE_BITS
        val qMove = bitCount / Int.SIZE_BITS
        return if (qShift != 0) when (qMove) {
            0 -> valueOf(q1 ushr qShift, q(q1, q2, qShift), q(q2, q3, qShift), q(q3, q4, qShift))
            1 -> valueOf(0, q1 ushr qShift, q(q1, q2, qShift), q(q2, q3, qShift))
            2 -> valueOf(0, 0, q1 ushr qShift, q(q1, q2, qShift))
            else /* = 3 */ -> valueOf(0, 0, 0, q1 ushr qShift)
        } else when (qMove) {
            1 -> valueOf(0, q1, q2, q3)
            2 -> valueOf(0, 0, q1, q2)
            else /* = 3 */ -> valueOf(0, 0, 0, q1)
        }
    }

    /**
     * Computes the bitwise `and` of the two values and returns the result.
     */
    @Suppress("unused")
    @Cumulative
    public infix fun and(other: Int128): Int128 {
        return valueOf(q1 and other.q1, q2 and other.q2, q3 and other.q3, q4 and other.q4)
    }

    /**
     * Computes the bitwise `or` of the two values and returns the result.
     */
    @Suppress("unused")
    @Cumulative
    public infix fun or(other: Int128): Int128 {
        return valueOf(q1 or other.q1, q2 or other.q2, q3 or other.q3, q4 or other.q4)
    }

    /**
     * Computes the bitwise `xor` of the two values and returns the result.
     */
    @Suppress("unused")
    @Cumulative
    public infix fun xor(other: Int128): Int128 {
        return valueOf(q1 xor other.q1, q2 xor other.q2, q3 xor other.q3, q4 xor other.q4)
    }

    // ---------------------------------------- arithmetic ----------------------------------------

    @Cumulative
    final override operator fun unaryMinus(): Int128 {
        if (this stateEquals MIN_VALUE) {
            raiseOverflow("-Int128.MIN_VALUE")
        }
        return addOne(q1.inv(), q2.inv(), q3.inv(), q4.inv())
    }

    private fun addOne(q1: Int, q2: Int, q3: Int, q4: Int): Int128 {
        val q4plus1 = q4 + 1
        var q3plus1 = q3
        var q2plus1 = q2
        var q1plus1 = q1
        if (addOverflowsInt(q4, 1)) {
            ++q3plus1
            if (addOverflowsInt(q3, 1)) {
                ++q2plus1
                if (addOverflowsInt(q2, 1)) {
                    ++q1plus1
                }
            }
        }
        return valueOf(q1plus1, q2plus1, q3plus1, q4plus1)
    }

    final override fun pow(power: Int): Int128 {
        when {
            power < 0 -> return ZERO
            power == 1 -> return this
            power == 0 -> return ONE
        }
        var pow = power
        val result = ONE.mutable()
        val product = this.mutable()
        while (pow != 0) {
            if (pow and 1 == 1) {
                result */* = */ product
            }
            product */* = */ product
            pow = pow ushr 1
        }
        return out(result)
    }

    final override fun signum(): Int = if (q4 == 0 && q3 == 0 && q2 == 0 && q1 == 0) 0 else sign
    
    // ((a << 32) + b) + ((c << 1) + d) = ((a + c) << 32) + (b + d)
    @Cumulative
    final override fun plus(other: Int128): Int128 {
        /**
         * Returns true if any of its most significant 32 bits are 1.
         */
        fun Long.isNotI32() = this.toULong() > UInt.MAX_VALUE

        val q4 = q4w() + other.q4w()
        val q3 = q3w() + other.q3w() + q4.isNotI32().toInt()
        val q2 = q2w() + other.q2w() + q3.isNotI32().toInt()
        val q1 = q1w() + other.q1w() + q2.isNotI32().toInt()
        val q1Low = q1.low
        if (this.sign == other.sign) {
            if (q1.isNotI32() || (q1Low < 0) xor this.isNegative /* sign bit overflow */) {
                raiseOverflow("$this + $other")
            }
        }
        return valueOf(q1Low, q2.low, q3.low, q4.low)
    }

    /*
        0(1) Multiplication of 128-Bit Integers
        See https://web.archive.org/web/20240609155726/https://cs.stackexchange.com/questions/140881/how-do-computers-perform-operations-on-numbers-that-are-larger-than-64-bits/140950#140950

        Definitions:
            - x & y are 128-bit integers
            - a, b, c, ..., h are 32-bit integers, and
            - The product of two 32-bit integers is a 64-bit integer

        Setup:
            x = (a << 96) + (b << 64) + (c << 32) + d
            y = (e << 96) + (f << 64) + (g << 32) + h,

            or in other words,

            x = a<<32*3 + b<<32*2 + c<<32*1 + d
            y = e<<32*3 + f<<32*2 + g<<32*1 + h

        Proof:
            xy = ((a * 2^96) + (b * 2^64) + (c * 2^32) + d)((e * 2^96) + (f * 2^64) + (g * 2^32) + h)
               = e(2^192a + 2^160b + 2^128c + 2^96d) +
                 f(2^160a + 2^128b + 2^96c  + 2^64d) +
                 a(2^128g + 2^96h) +
                 g(2^96b  + 2^64c  + 2^32d) +
                 2^64bh   +
                 2^32ch   +
                 dh
               = e(a<<32*6 + b<<32*5  + c<<32*4  + d<<32*3) +
                 f(a<<32*5 + b<<32*4  + c<<32*3  + d<<32*2) +
                 a(g<<32*4 + h<<32*3) +
                 g(b<<32*3 + c<<32*2  + d<<32*1) +
                 bh<<32*2  +
                 ch<<32*1  +
                 dh

            if a, b, e, & f are 0,    xy = cg<<32*2 + (dg + ch)<<32*1 + dh                     (i64 x i64)
            if a, b, & c are 0,       xy = de<<32*3 + df<<32*2 + dg<<32*1 + dh                 (i32 x i128)
            if a, b, & e are 0,       xy = cf<<32*3 + (df + cg)<<32*2 + (dg + ch)<<32*1 + dh   (i64 x i96)

        For xy to fit within a 128-bit integer:
            - a, b, e, & f must be 0 (given that the product can, at most, be 128 bits long), or
            - a, b, & c must be 0, or
            - a, b & e must be zero and the most significant 32 bits must not carry over a bit
     */
    final override fun times(other: Int128): Int128 {
        infix fun Int128.with(sign: Int) = if (sign == -1) -this else this

        fun addMultiply(q1q2partial: Long, q2q3partial: Long, q3q4partial: Long): Int128 {
            val (q1summand, q2summand1) = q1q2partial
            val (q2summand2, q3summand1) = q2q3partial
            val (q3summand2, q4) = q3q4partial
            val q3 = q3summand1 + q3summand2
            var carry = (addOverflowsInt(q3summand1, q3summand2)).toInt()
            val q2carry = q2summand2 + carry
            val q2 = q2summand1 + q2carry
            carry = (addOverflowsInt(q2summand1, q2carry) || addOverflowsInt(q2summand1, q2summand2)).toInt()
            if (addOverflowsValue(q1summand, carry)) {
                raiseOverflow()
            }
            return valueOf(q1summand + carry, q2, q3, q4)
        }

        fun addMultiply(q0q1partial: Long, q1q2partial: Long, q2q3partial: Long, q3q4partial: Long): Int128 {
            if (q0q1partial.low < 0) {  // Sign bit overflow
                raiseOverflow()
            }
            val (q1summand1, q2, q3, q4) = addMultiply(q1q2partial, q2q3partial, q3q4partial)
            val q1summand2 = q0q1partial.low
            if (addOverflowsValue(q1summand1, q1summand2)) {
                raiseOverflow()
            }
            return valueOf(q1summand1 + q1summand2, q2, q3, q4)
        }

        // dh
        fun int32TimesInt32(i32a: Int128, i32b: Int128 ): Int128 {
            val d = i32a.q4.toLong()
            val h = i32b.q4.toLong()
            val dh = d * h
            val blank = blank(dh.sign or 1 /* if zero */)
            return valueOf(blank, blank, dh.high, dh.low)
        }

        // cg<<32*2 + (dg + ch)<<32*1 + dh
        fun int64TimesInt64(i64a: Int128, i64b: Int128): Int128 {
            val g = i64b.q3w(); val c = i64a.q3w()
            val h = i64b.q4w(); val d = i64a.q4w()
            return addMultiply(c * g, (d * g) + (c * h), d * h)
        }

        // de<<32*3 + df<<32*2 + dg<<32*1 + dh
        fun int32TimesInt128(i32: Int128, i128: Int128): Int128 {
            val d = i32.q4w()
            return addMultiply(d * i128.q1w(), d * i128.q2w(), d * i128.q3w(), d * i128.q4w())
        }

        // cf<<32*3 + (df + cg)<<32*2 + (dg + ch)<<32*1 + dh
        fun int64TimesInt96(i64: Int128, i96: Int128): Int128 {
            val f = i96.q2w();  val c = i64.q3w()
            val g = i96.q3w();  val d = i64.q4w()
            val h = i96.q4w()
            return addMultiply(c * f, (d * f) + (c * g), (d * g) + (c * h), d * h)
        }

        val sign = productSign(sign, other.sign)
        val abs = abs()
        val mag = abs.magnitude()
        val otherAbs = other.abs()
        val otherMag = otherAbs.magnitude()
        try {
            if (otherMag <= 2) when {
                otherMag == 1 -> {
                    val product = if (mag == 1) int32TimesInt32(abs, otherAbs) else int32TimesInt128(otherAbs, abs)
                    return product with sign
                }
                mag == 2 -> return int64TimesInt64(abs, otherAbs) with sign
                mag == 3 -> return int64TimesInt96(otherAbs, abs) with sign
                otherAbs stateEquals ONE -> return abs with sign
                otherMag == 0 -> return ZERO
            }
            // ...otherMag == 3 || otherMag == 4
            if (mag <= 2) when {
                mag == 1 -> return int32TimesInt128(abs, otherAbs) with sign
                otherMag == 3 -> return int64TimesInt96(abs, otherAbs) with sign
                abs stateEquals ONE -> return otherAbs with sign
                mag == 0 -> return ZERO
            }
        } catch (e: CompositeOverflowException) {
            raiseOverflow("$this * $otherAbs", e)
        }
        raiseOverflow("$this * $otherAbs")
    }

    /**
     * Returns the [quotient][div] paired to the [remainder][rem], respectively.
     *
     * The returned remainder is always non-negative.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    public infix fun divAndRem(other: Int128): Pair<Int128, Int128> = divide(other, DivisionType.BOTH)

    /**
     * Returns the [quotient][div].
     */
    public infix fun divAndRound(other: Int128): Int128 {
        val (quotient, remainder) = this divAndRem other
        val otherDiv2 = other.rightShift(1)
        return if (remainder < otherDiv2) quotient else with (quotient) { addOne(q1, q2, q3, q4) }
    }

    final override fun div(other: Int128): Int128 = divide(other, DivisionType.QUOTIENT)

    final override fun rem(other: Int128): Int128 = divide(other, DivisionType.REMAINDER)

    // Shift-subtract algorithm
    private fun <T : Any> divide(other: Int128, division: DivisionType): T {
        /**
         * Assumes receiver is positive and magnitude is not 0.
         */
        fun Int128.countLeadingZeroBits(): Int {
            val mag = magnitude()
            val i32ZeroBitCount = when (mag) {
                1 -> q4
                2 -> q3
                3 -> q2
                else /* = 4 */ -> q1
            }.countLeadingZeroBits()
            return Int.SIZE_BITS * (4 - mag) + i32ZeroBitCount
        }

        if (other stateEquals ONE) {
            return division.result(
                quotient = { this },
                remainder = { ZERO }
            )
        }
        if (other stateEquals ZERO) {
            raiseUndefined("Divisor cannot be 0 (dividend = $this)")
        }
        if (this stateEquals other) {
            return division.result(
                quotient = { ONE },
                remainder = { ZERO }
            )
        }
        if (this.isLong() && other.isLong()) {
            return division.result(
                quotient = { valueOf(this.toLong() / other.toLong()) },
                remainder = { valueOf(this.toLong() % other.toLong()) }
            )
        }
        val sign = productSign(sign, other.sign)
        val dividend = this.mutable().abs()
        val divisor = other.mutable().abs()
        if (divisor > dividend) {
            return division.result(
                quotient = { ZERO },
                remainder = { out(dividend) }
            )
        }
        var bitAlign = divisor.countLeadingZeroBits() - dividend.countLeadingZeroBits()
        divisor/* = */.leftShift(bitAlign)
        val quotient: Int128 = MutableInt128(ZERO)
        do {
            quotient/* = */.leftShift(1)
            if (dividend.valueCompareTo(divisor) >= 0) {
                dividend -/* = */ divisor.immutable()   // May be mutated by minus()
                with (quotient) { addOne(q1, q2, q3, q4) }  // ++quotient
            }
            divisor/* = */.unsignedRightShift(1)
           --bitAlign
        } while (bitAlign >= 0)
        return division.result(
            quotient = {
                if (sign == -1) {
                    /* quotient = */ -quotient
                }
                out(quotient)
            },
            remainder = { out(dividend) }
        )
    }

    // ---------------------------------------- comparison ----------------------------------------

    /**
     * Assumes that [isPositive]` == other.isPositive`.
     */
    private fun valueCompareTo(other: Int128): Int {
        var difference = q1.compareTo(other.q1)
        if (difference != 0) {
            return difference
        }
        difference = q2.toUInt().compareTo(other.q2.toUInt())
        if (difference != 0) {
            return difference
        }
        difference = q3.toUInt().compareTo(other.q3.toUInt())
        if (difference != 0) {
            return difference
        }
        return q4.toUInt().compareTo(other.q4.toUInt())
    }

    final override fun compareTo(other: Int128): Int {
        return when {
            this.isNegative && other.isPositive -> -1
            this.isPositive && other.isNegative -> 1
            else -> valueCompareTo(other)
        }
    }

    final override fun hashCode(): Int {
        var hash = 7
        hash = 31 * hash + q1
        hash = 31 * hash + q2
        hash = 31 * hash + q3
        hash = 31 * hash + q4
        return hash
    }

    final override fun isLong(): Boolean {
        val blank = blank()
        return q1 == blank && q2 == blank && q3 and Int.MIN_VALUE == blank shl (Int.SIZE_BITS - 1)
    }

    final override fun stateEquals(other: Int128): Boolean {
        return q1 == other.q1 && q2 == other.q2 && q3 == other.q3 && q4 == other.q4
    }

    // ---------------------------------------- conversions ----------------------------------------

    /**
     * Returns a string representation of this with radix 2 in two's complement form.
     *
     * The returned string is not guaranteed to be a legal parameter to the `String`-arg pseudo-constructor.
     * It is padded at the start with zeroes, if necessary, to be exactly 128 digits long.
     * Additionally, for every 32 digits, an underscore is appended.
     *
     * To get an ordinary binary representation with an optional negative sign, use [toString] with radix 2.
     */
    public fun binaryString(): String {
        fun Int.to2c() = this.toUInt().toString(radix = 2).padStart(Int.SIZE_BITS, '0')

        return "${q1.to2c()}${q2.to2c()}${q3.to2c()}${q4.to2c()}"
    }

    /**
     * Returns a string representation of this value with the given [radix].
     *
     * When passed to the string constructor, creates an instance equal in value to this.
     * The result of this function is cached when `radix == 10`.
     *
     * To get a binary representation of this value in two's complement form, use [binaryString].
     * @throws IllegalArgumentException `radix` is not between 2 and 36, inclusive
     * @see binaryString
     */
    public fun toString(radix: Int): String {
        require(radix in 2..36) { "Invalid radix (n = $radix)" }
        return toStringRepresentation(radix)
    }

    private fun toStringRepresentation(radix: Int): String {
        if (this stateEquals ZERO) {
            return "0"
        }
        val base = radix.toInt128()
        val sign = this.sign    // May be mutated by abs()
        var value = this.abs().immutable()
        return buildString {
            while (!value.stateEquals(ZERO)) {
                val (shiftedValue, digit) = value.divAndRem(base)
                value = shiftedValue
                insert(0, digit.toInt().digitToChar(radix))
            }
            if (sign == -1) {
                insert(0, '-')
            }
        }
    }

    final override fun toInt(): Int = q4

    final override fun toLong(): Long = (q3w() shl Int.SIZE_BITS) or q4w()

    final override fun toDouble(): Double {
        return ((q1w() shl Int.SIZE_BITS) or q2w()) * 1.8446744073709552E19 /* 2^64 */ + toLong().toDouble()
    }

    final override fun toComplex(): Complex = Complex(toRational(), Rational.ZERO)

    final override fun toRational(): Rational {
        return ScaledLong(this).let { (numer, scale) -> Rational(numer, 1L, scale, sign) }
    }

    /**
     * Returns this instance.
     */
    final override fun toInt128(): Int128 = this

    /**
     * Returns a string representation of this value in base 10.
     *
     * When passed to the string constructor of the inheritor, creates an instance equal in value to this.
     * The returned string is equivalent to that of `toString(radix = 10)`.
     */
    override fun toString(): String = toStringRepresentation(10)

    public companion object {
        @JvmStatic public val NEGATIVE_ONE: Int128 = ConstantInt128(-1, -1, -1, -1, "-1")
        @JvmStatic public val ZERO: Int128 = ConstantInt128(0, 0, 0, 0, "0")
        @JvmStatic public val ONE: Int128 = ConstantInt128(0, 0, 0, 1, "1")
        @JvmStatic public val TWO: Int128 = ConstantInt128(0, 0, 0, 2, "2")
        @JvmStatic public val TEN: Int128 = ConstantInt128(0, 0, 0, 10, "10")
        @JvmStatic public val SIXTEEN: Int128 = ConstantInt128(0, 0, 0, 16, "16")

        @JvmStatic public val MIN_VALUE: Int128
                = ConstantInt128(Int.MIN_VALUE, 0, 0, 0, "−170141183460469231731687303715884105728")
        @JvmStatic public val MAX_VALUE: Int128
                = ConstantInt128(Int.MAX_VALUE, -1, -1, -1, "170141183460469231731687303715884105727")

        public const val SIZE_BYTES: Int = 128 / Byte.SIZE_BITS

        @Suppress("ConstPropertyName")
        private const val serialVersionUID = 1L

        private class ConstantInt128(
            q1: Int,
            q2: Int,
            q3: Int,
            q4: Int,
            override val stringLiteral: String
        ) : Int128(q1, q2, q3, q4, PrivateAPIFlag), Constant {
            override fun toString() = stringLiteral
        }

        /**
         * Returns the factorial of [x] as a 128-bit integer.
         *
         * @throws CompositeUndefinedException x is non-negative
         * @throws CompositeOverflowException the result overflows
         */
        @JvmStatic
        public fun factorial(x: Int): Int128 {
            if (x < 0) {
                raiseUndefined("Factorial of $x is undefined")
            }
            return try {
                signallingFactorial(x)
            } catch (e: FactorialOverflowSignal) {
                raiseOverflow("$x!", e)
            }
        }

        // ---------------------------------------- destructuring ----------------------------------------

        /**
         * The value of a quarter in a 128-bit integer when there is no information stored in it.
         *
         * Outside of `Int128`, is intended for access by `BigInteger` pseudo-constructor only.
         * @return a value where all bits are 1 or 0 depending on whether [sign] is -1 or 1, respectively
         */
        internal fun blank(sign: Int) = sign shr 1

        /**
         * Returns the quarter at the current position after a bitwise right shift.
         */
        private fun q(qLast: Int, qCur: Int, qShift: Int) = (qLast shl (Int.SIZE_BITS - qShift)) or (qCur ushr qShift)

        // ---------------------------------------- string conversion ----------------------------------------

        /**
         * Intended for access by `String`-arg pseudo-constructor and [ScaledLong] constructor only.
         */
        internal fun parse(
            s: String,
            radix: Int,
            start: Int,
            endExclusive: Int
        ): MutableInt128 {
            val first = try {
                s[start]
            } catch (e: StringIndexOutOfBoundsException) {
                raiseIncorrectFormat("empty string", s)
            }
            var cursor = endExclusive - 1
            val startIndex = start + (first == '-').toInt()
            val digit = MutableInt128(ZERO)
            val value = MutableInt128(ZERO)
            var pow = ONE
            val increment = radix.toInt128()
            while (cursor >= startIndex) try {
                if (s[cursor] !in '0'..'9') {
                    raiseIncorrectFormat("illegal embedded character", s)
                }
                digit/* = */.valueOf(s[cursor].digitToInt(radix))
                value +/* = */ (digit */* (maybe) = */ pow)
                pow *= increment
                --cursor
            } catch (e: IllegalArgumentException) {
                raiseIncorrectFormat("illegal digit", s, e)
            } catch (e: CompositeOverflowException) {
                raiseOverflow(s.substring(start, endExclusive), e)
            }
            val result = if (startIndex != start) /* value = */ -value else value
            return result as MutableInt128
        }

        // ---------------------------------------- helpers ----------------------------------------

        /**
         * The most significant 32 bits of this value.
         */
        @JvmStatic protected val Long.high: Int inline get() = (this ushr 32).toInt()

        /**
         * The least significant 32 bits of this value.
         */
        @JvmStatic protected val Long.low: Int inline get() = this.toInt()

        /**
         * The [upper half][high] of this value.
         */
        private operator fun Long.component1() = high

        /**
         * The [lower half][low] of this value.
         */
        private operator fun Long.component2() = low

        private fun Int.widen() = this.toUInt().toLong()

        private fun ensureValidShift(bitCount: Int) {
            require(bitCount >= 0) { "Bitwise shift cannot be negative (bitCount = $bitCount)" }
        }

        /**
         * Returns true if the sum is the result of an unsigned integer overflow.
         */
        private fun addOverflowsInt(x: Int, y: Int) = (x.widen() + y.widen()) > UInt.MAX_VALUE.toLong()

        /**
         * Returns true if the sum is the result of a signed integer overflow.
         *
         * If a result of multiple additions must be checked, this function must be called for each intermediate sum.
         * Also checks for the case [Int.MIN_VALUE] - 1.
         */
        private fun addOverflowsValue(x: Int, y: Int): Boolean {
            val sum = x.toLong() + y
            return if (sum < 0L) sum < Int.MIN_VALUE else sum > Int.MAX_VALUE
        }
    }
}