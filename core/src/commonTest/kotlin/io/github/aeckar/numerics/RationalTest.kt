package io.github.aeckar.numerics

import io.github.aeckar.numerics.Rational.Companion.MAX_VALUE
import io.github.aeckar.numerics.Rational.Companion.MIN_VALUE
import io.github.aeckar.numerics.Rational.Companion.NEGATIVE_ONE
import io.github.aeckar.numerics.Rational.Companion.ONE
import io.github.aeckar.numerics.Rational.Companion.PI
import io.github.aeckar.numerics.Rational.Companion.TWO
import io.github.aeckar.numerics.Rational.Companion.ZERO
import io.github.aeckar.numerics.Rational.Companion.factorial
import io.github.aeckar.numerics.functions.ceil
import io.github.aeckar.numerics.functions.floor
import io.github.aeckar.numerics.functions.ln
import kotlin.random.Random
import kotlin.random.nextInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class RationalTest {
    @Test
    fun comparison() {
        assertTrue { NEGATIVE_ONE < MAX_VALUE }
        assertTrue { ONE > MIN_VALUE }
    }

    @Test
    fun equality() {
        val randA = Random.nextInt(0..Int.MAX_VALUE)
        val randB = Random.nextInt(0..Int.MAX_VALUE)
        assertEquals(Rational(randA, randA), Rational(randB, randB))
        assertEquals(Rational(0, randA), Rational(0, randB))
        assertEquals(PI, -(-PI))
        assertEquals(ZERO, -ZERO)
    }

    // ------------------------------ string conversion ------------------------------

    @Test
    fun to_components_string() {
        fun String.withExponent(): String {
            val scale = takeLastWhile { it == '0' }.count()
            return if (scale == 0) this else (this.take(length - scale) + "e$scale")
        }

        val posStr = Random.nextInt(1..Int.MAX_VALUE).toString().withExponent()
        val negStr = Random.nextInt(Int.MIN_VALUE..-1).toString().withExponent()
        val max32Str = Int.MAX_VALUE.toString()
        val min32Str = Int.MIN_VALUE.toString()
        val max64Str = Long.MAX_VALUE.toString()
        val min64Str = Long.MIN_VALUE.toString()
        val neg1 = Rational("-1")

        assertEquals(posStr, Rational(posStr).toString())
        assertEquals(negStr, Rational(negStr).toString())
        assertEquals(max32Str, Rational(max32Str).toString())
        assertEquals(min32Str, Rational(min32Str).toString())
        assertEquals(max64Str, Rational(max64Str).toString())
        assertEquals("-922337203685477580e1", Rational(min64Str).toString()) // Lossy
        assertEquals("1", Rational("1").toString())
        assertEquals("0", Rational("0").toString())
        assertEquals("-1", neg1.toString())
        assertEquals(NEGATIVE_ONE, neg1)

        assertEquals("17/31", Rational("17/31").toString())     // Fraction
        assertEquals("5e-2", Rational(".05").toString())        // Leading dot
        assertEquals("14e-367", Rational("14e-367").toString()) // Exponent

        assertFailsWith<NumericFormatException> { Rational("") }
        assertFailsWith<NumericFormatException> { Rational("3.1.4") }
        assertFailsWith<NumericFormatException> { Rational("--3.14") }
        assertFailsWith<NumericFormatException> { Rational("((3.14))") }
        assertFailsWith<NumericFormatException> { Rational("(3.14") }
        Rational(HUGE_STRING)   // Must not throw
    }

    @Test
    fun to_decimal_string() {
        // Mixed fraction
        assertEquals("67", Rational(1000, 15).decimalString(2))
        assertEquals("4.25", Rational("17/4").decimalString())
        assertEquals("1234", Rational("1234").decimalString())
        assertEquals("1230", Rational(1234).decimalString(3))

        // Terminating decimal
        assertEquals("0.1", Rational("1/10").decimalString(1))
        assertEquals("0.35", Rational("7/20").decimalString())
        assertEquals("0.125", Rational("1/8").decimalString())
        assertEquals("0.35", Rational("7/20").decimalString(2))
        assertEquals("0.13", Rational("1/8").decimalString(2))

        // Repeating decimal    // TODO LATER make it so these legal string-arg params
        assertEquals("0.(3)", Rational("1/3").decimalString())
        assertEquals("0.(81)", Rational("81/99").decimalString())
        assertEquals("11.(1886792452830)", Rational("593/53").decimalString())
        assertEquals("0.(50561797752808988764044943820224719101123595)", Rational("45/89").decimalString())
        assertEquals("0.58(3)", Rational("525/900").decimalString())
        assertEquals("0.58", Rational("525/900").decimalString(2))
        assertEquals("142.(857142)", Rational("(1/7)e3").decimalString())
    }

    @Test
    fun to_sci_notation_string() {
        assertEquals("1e-1", Rational("1/10").sciNotationString(1))
        assertEquals("6.7e1", Rational(1000, 15).sciNotationString(2))
        assertEquals("4.25", Rational("17/4").sciNotationString(4))
        assertEquals("1.234e3", Rational("1234").sciNotationString(5))
        assertEquals("1.23e3", Rational(1234).sciNotationString(3))
        assertEquals("1.2e3", Rational(1234).sciNotationString(2))
    }

    // ------------------------------ basic arithmetic ------------------------------

    @Test
    fun plus() {
        val a = Rational(1, 2)
        val b = Rational(1, 3)
        val sum = a + b
        assertEquals(Rational(5, 6), sum)
        assertEquals(b, sum - a)
        assertEquals(a, sum - b)
        MAX_VALUE + ZERO    // Must not throw
        MAX_VALUE + ONE     // Must not throw
        assertFailsWith<NumericOverflowException> { MAX_VALUE + MAX_VALUE }
    }

    @Test
    fun minus() {
        val a = Rational(3, 4)
        val b = Rational(1, 4)
        val difference = a - b
        val hugeNeg = Rational(Long.MIN_VALUE + 1, 1, Int.MAX_VALUE)
        assertEquals(Rational(1, 2), difference)
        assertEquals(a, b + difference)
        assertEquals(b, a - difference)
        MIN_VALUE - ZERO    // Must not throw
        MIN_VALUE - ONE     // Must not throw
        assertFailsWith<NumericOverflowException> { MIN_VALUE + MIN_VALUE }    // Long.MIN_VALUE is scaled up by 1
        assertFailsWith<NumericOverflowException> { hugeNeg + hugeNeg }
    }

    @Test
    fun times() {
        val a = Rational(2, 3)
        val b = Rational(3, 4)
        val product = a * b
        assertEquals(Rational(1, 2), product)
        assertEquals(a, product / b)
        assertEquals(b, product / a)
        MAX_VALUE * ONE // Must not throw
        assertFailsWith<NumericOverflowException> { MAX_VALUE * TWO }
    }

    @Test
    fun div() {
        val a = Rational(2, 3)
        val b = Rational(3, 4)
        val quotient = a / b
        assertEquals(Rational(8, 9), quotient)
        assertEquals(a, quotient * b)
        assertFailsWith<NumericUndefinedException> { a / ZERO }
    }

    @Test
    fun rem() {
        val a = Rational(5, 3)
        val b = Rational(2, 3)
        assertEquals(Rational(1, 3), a % b)
        assertEquals(ZERO, a % a)
        assertFailsWith<NumericUndefinedException> { a % ZERO }
    }

    @Test
    fun pow() {
        val base = Rational(2, 3)
        assertEquals(ONE, base.pow(0))
        assertEquals(base, base.pow(1))
        assertEquals(Rational(8, 27), base.pow(3))
        assertEquals(Rational(27, 8), base.pow(-3))
    }

    @Test
    fun factorial() {
        assertEquals(ONE, factorial(0))
        assertEquals(ONE, factorial(1))
        assertEquals(Rational("8683317618811886483e18"), factorial(33)) // Some loss due to rounding
    }

    // ------------------------------ elementary functions ------------------------------

    @Test
    fun ln() {
        println(ln(Rational(5, 6)))
    }

    @Test
    fun sin() {
        TODO("Not implemented yet")
    }

    @Test
    fun cos() {
        TODO("Not implemented yet")
    }

    @Test
    fun sinh() {
        TODO("Not implemented yet")
    }

    @Test
    fun cosh() {
        TODO("Not implemented yet")
    }

    @Test
    fun arcsin() {
        TODO("Not implemented yet")
    }

    @Test
    fun arctan() {
        TODO("Not implemented yet")
    }

    // ------------------------------ rounding ------------------------------

    private val min64 = Rational(Long.MIN_VALUE)

    @Test
    fun ceil() {
        assertEquals(TWO, ceil(Rational(5, 3)))
        assertEquals(NEGATIVE_ONE, ceil(Rational(-5, 3)))
        assertEquals(min64, ceil(Rational(Long.MIN_VALUE, 1)))
    }

    @Test
    fun floor() {
        assertEquals(ONE, floor(Rational(5, 3)))
        assertEquals(-TWO, floor(Rational(-5, 3)))
        assertEquals(min64, ceil(Rational(Long.MIN_VALUE, 1)))
    }
}