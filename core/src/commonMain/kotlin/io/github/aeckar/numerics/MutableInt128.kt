package io.github.aeckar.numerics

import kotlin.math.sign

/**
 * A mutable 128-bit integer.
 *
 * If a superclass operation is called, the mutability of its result cannot be guaranteed.
 */
internal class MutableInt128 : Int128 {
    constructor(unique: Int128) : super(unique.q1, unique.q2, unique.q3, unique.q4)

    constructor(lower: Long) : super(lower)

    fun store(q1: Int, q2: Int, q3: Int, q4: Int) = this.also {
        it.q1 = q1; it.q3 = q3
        it.q2 = q2; it.q4 = q4
    }

    fun store(q4: Int) = this.also {
        val blank = blank(q4.sign)
        it.q1 = blank
        it.q2 = blank
        it.q3 = blank
        it.q4 = q4
    }

    fun storeAbs() {
        if (this < 0) {
            storeNegate()
        }
    }

    fun storeLeftShift(bitCount: Int) {
        leftShift(bitCount, inPlace = true)
    }

    fun storeUnsignedRightShift(bitCount: Int) {
        unsignedRightShift(bitCount, inPlace = true)
    }

    fun storeNegate() {
        q1 = q1.inv()
        q2 = q2.inv()
        q3 = q3.inv()
        q4 = q4.inv()
        increment()
    }

    fun increment() = increment(q1, q2, q3, q4, inPlace = true) as MutableInt128

    operator fun minusAssign(other: Int128) {
        this += -other
    }

    operator fun plusAssign(other: Int128) {
        this.plus(other, inPlace = true)
    }

    operator fun timesAssign(other: Int128) {
        this.times(other, inPlace = true)
    }

    override fun toMutable() = this

    override fun copy() = Int128(this)
}