package io.github.aeckar.numerics

import io.github.aeckar.numerics.Int128.Companion.MAX_VALUE
import io.github.aeckar.numerics.Int128.Companion.MIN_VALUE
import io.github.aeckar.numerics.Int128.Companion.NEGATIVE_ONE
import io.github.aeckar.numerics.Int128.Companion.ONE
import io.github.aeckar.numerics.Int128.Companion.TEN
import io.github.aeckar.numerics.Int128.Companion.TWO
import io.github.aeckar.numerics.Int128.Companion.ZERO
import io.github.aeckar.numerics.Int128.Companion.factorial
import kotlin.random.nextInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

private fun assertBitsEquals(x: Int128, y: Int128) {
    try {
        assertEquals(x, y)
    } catch (e: AssertionError) {
        println("Expected :" + x.binaryString())
        println("Actual   :" + y.binaryString())
        throw e
    }
}

class Int128Test {
    @Test
    fun comparison() {
        assertTrue { NEGATIVE_ONE < MAX_VALUE }
        assertTrue { ONE > MIN_VALUE }
    }

    // ------------------------------ string conversion ------------------------------

    private val posStr = random.nextInt(1..Int.MAX_VALUE).toString()
    private val negStr = random.nextInt(Int.MIN_VALUE..-1).toString()
    private val max32Str = Int.MAX_VALUE.toString()
    private val min32Str = Int.MIN_VALUE.toString()
    private val max64Str = Long.MAX_VALUE.toString()
    private val min64Str = Long.MIN_VALUE.toString()

    private val neg1 = Int128("-1")

    @Test
    fun to_base10_string() {
        assertEquals(posStr, Int128(posStr).toString())
        assertEquals(negStr, Int128(negStr).toString())
        assertEquals(max32Str, Int128(max32Str).toString())
        assertEquals(min32Str, Int128(min32Str).toString())
        assertEquals(max64Str, Int128(max64Str).toString())
        assertEquals(min64Str, Int128(min64Str).toString())
        assertEquals("1", Int128("1").toString())
        assertEquals("0", Int128("0").toString())
        assertEquals("-1", neg1.toString())

        assertEquals(NEGATIVE_ONE, neg1)

        assertFailsWith<NumericFormatException> { Int128("") }
        assertFailsWith<NumericFormatException> { Int128("3.14") }
        assertFailsWith<NumericOverflowException> { Int128(HUGE_STRING) }
    }

    // ------------------------------ bitwise shift ------------------------------

    private val pos16 = random.nextInt(0..Short.MAX_VALUE)
    private val neg16 = random.nextInt(Short.MIN_VALUE..-1)
    private val shift16 = random.nextInt(0..<16)
    private val gte128 = random.nextInt(128..160)

    @Test
    fun left_shift() {
        assertBitsEquals(Int128(pos16 shl shift16), Int128(pos16) shl shift16)
        assertBitsEquals(Int128(neg16 shl shift16), Int128(neg16) shl shift16)
        assertBitsEquals(ZERO, random.nextInt128() shl gte128)
        assertFailsWith<IllegalArgumentException> { ONE shl -1 }
    }

    @Test
    fun right_shift() {
        val value = random.nextInt128()

        assertBitsEquals(Int128(pos16 shr shift16), Int128(pos16) shr shift16)
        assertBitsEquals(Int128(neg16 shr shift16), Int128(neg16) shr shift16)
        assertBitsEquals(Int128(value.sign shr 1), value shr gte128)
        assertFailsWith<IllegalArgumentException> { ONE shr -1 }
    }

    @Test
    fun unsigned_right_shift() {
        assertBitsEquals(Int128(pos16 ushr shift16), Int128(pos16) ushr shift16)
        assertBitsEquals(Int128(-1 ushr shift16, -1, -1, neg16 shr shift16), Int128(neg16) ushr shift16)
        assertBitsEquals(ZERO, random.nextInt128() ushr gte128)
        assertFailsWith<IllegalArgumentException> { ONE ushr -1 }
    }

    // ------------------------------ arithmetic ------------------------------

    private val int64 = random.nextLong()

    @Test
    fun negation() {
        assertBitsEquals(Int128(-int64), -Int128(int64))
        assertEquals(-int64, -Int128(int64).toLong())
        assertFailsWith<NumericOverflowException> { -MIN_VALUE }
    }

    @Test
    fun addition() {
        do try {    // Ignore random tests where the result overflows
            val a = random.nextInt128()
            val b = random.nextInt128()
            val sum = a + b
            assertBitsEquals(a, sum - b)
            assertBitsEquals(b, sum - a)
            MAX_VALUE + ZERO            // Must not throw
            MAX_VALUE + NEGATIVE_ONE    // Must not throw
            assertFailsWith<NumericOverflowException> { MAX_VALUE + ONE }
            break
        } catch (_: NumericOverflowException) { /* no-op */ } while (true)
    }

    @Test
    fun subtraction() {
        do try {    // Ignore random tests where the result overflows
            val a = random.nextInt128()
            val b = random.nextInt128()
            val difference = a - b
            assertBitsEquals(a, difference + b)
            assertBitsEquals(b, a - difference)
            MIN_VALUE - ZERO            // Must not throw
            MIN_VALUE - NEGATIVE_ONE    // Must not throw
            assertFailsWith<NumericOverflowException> { MIN_VALUE - ONE }
            break
        } catch (_: NumericOverflowException) { /* no-op */ } while (true)
    }

    @Test
    fun multiplication() {
        val a = Int128(5000)
        val b = Int128(1250)
        val product = a * b

        assertBitsEquals(a, product / b)
        assertBitsEquals(b, product / a)
        assertFailsWith<NumericOverflowException> { MAX_VALUE * TWO }
        MAX_VALUE * ONE // Must not throw
    }

    @Test
    fun division() {
        val a = Int128(5000)
        val b = Int128(1250)
        val quotient = a / b
        assertBitsEquals(Int128(4), quotient)
        assertBitsEquals(a, quotient * b)
        assertBitsEquals(b, a / quotient)
        ONE / MAX_VALUE // Must not throw
        assertFailsWith<NumericUndefinedException> { ONE / ZERO }

        // Inexact results
        assertEquals(TEN, Int128(101) / TEN)
        assertEquals(TEN, Int128(109) / TEN)
    }

    @Test
    fun remainder() {
        val a = Int128(5000)
        val b = Int128(1267)
        val quotientWholePart = Int128(3)
        val remainder = a % b
        assertBitsEquals(Int128(1199), remainder)
        assertBitsEquals(a, (b * quotientWholePart) + remainder)
        assertBitsEquals(b, (a - remainder) / quotientWholePart)
        ONE % MAX_VALUE // Must not throw
        assertFailsWith<NumericUndefinedException> { ONE % ZERO }

        // Inexact results
        assertEquals(ONE, Int128(101) % TEN)
        assertEquals(Int128(9), Int128(109) % TEN)
    }

    @Test
    fun exponentiation() {
        val a = Int128(19)
        assertBitsEquals(Int128(130321), a.pow(4))
        assertFailsWith<NumericOverflowException> { a.pow(Int.MAX_VALUE) }
    }

    @Test
    fun factorial() {
        assertBitsEquals(ONE, factorial(0))
        assertBitsEquals(ONE, factorial(1))
        assertBitsEquals(Int128("8683317618811886495518194401280000000"), factorial(33))
        assertFailsWith<NumericOverflowException> { factorial(34) }
    }
}