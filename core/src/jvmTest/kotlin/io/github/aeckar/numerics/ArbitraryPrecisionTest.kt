package io.github.aeckar.numerics

import java.math.BigInteger
import kotlin.random.nextInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

class ArbitraryPrecisionTest {
    private val posInt = random.nextInt(0..Int.MAX_VALUE).toBigInteger()
    private val negInt = random.nextInt(Int.MIN_VALUE..-1).toBigInteger()
    private val max32Int = Int.MAX_VALUE.toBigInteger()
    private val min32Int = Int.MIN_VALUE.toBigInteger()
    private val max64Int = Long.MAX_VALUE.toBigInteger()
    private val min64Int = Long.MIN_VALUE.toBigInteger()
    private val oneInt = BigInteger.ONE
    private val zeroInt = BigInteger.ZERO
    private val neg1Int = (-1).toBigInteger()

    @Test
    fun int128ToBigInteger() {
        assertEquals(posInt, Int128(posInt).toBigInteger())
        assertEquals(negInt, Int128(negInt).toBigInteger())
        assertEquals(max32Int, Int128(max32Int).toBigInteger())
        assertEquals(min32Int, Int128(min32Int).toBigInteger())
        assertEquals(max64Int, Int128(max64Int).toBigInteger())
        assertEquals(min64Int, Int128(min64Int).toBigInteger())
        assertEquals(oneInt, Int128(oneInt).toBigInteger())
        assertEquals(zeroInt, Int128(zeroInt).toBigInteger())
        assertEquals(neg1Int, Int128(neg1Int).toBigInteger())
        assertFailsWith<ArithmeticException> { Int128(BigInteger(HUGE_STRING)) }
    }

    @Test
    fun rationalToBigInteger() {
        val min64Plus1Int = min64Int + BigInteger.ONE
        assertEquals(posInt, Rational(posInt).toBigInteger())
        assertEquals(negInt, Rational(negInt).toBigInteger())
        assertEquals(max32Int, Rational(max32Int).toBigInteger())
        assertEquals(min32Int, Rational(min32Int).toBigInteger())
        assertEquals(max64Int, Rational(max64Int).toBigInteger())
        assertEquals(min64Plus1Int, Rational(min64Plus1Int).toBigInteger())
        assertNotEquals(min64Int, Rational(min64Int).toBigInteger())
        assertEquals(oneInt, Rational(oneInt).toBigInteger())
        assertEquals(zeroInt, Rational(zeroInt).toBigInteger())
        assertEquals(neg1Int, Rational(neg1Int).toBigInteger())
        Rational(BigInteger(HUGE_STRING))   // Must not throw
    }
}