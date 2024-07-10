package io.github.aeckar.numerics

import io.github.aeckar.numerics.utils.PrivateAPIFlag

/**
 * A mutable 128-bit integer.
 *
 * See [Cumulative] for details on composite number mutability.
 */
internal class MutableInt128 : Int128 {
    constructor(unique: Int128) : super(unique.q1, unique.q2, unique.q3, unique.q4, PrivateAPIFlag)
    constructor(lower: Long) : super(lower, PrivateAPIFlag)

    override fun immutable() = Int128(q1, q2, q3, q4)

    @Cumulative
    override fun mutable() = this

    @Cumulative
    override fun valueOf(q1: Int, q2: Int, q3: Int, q4: Int) = this.also {
        it.q1 = q1; it.q3 = q3
        it.q2 = q2; it.q4 = q4
    }

    @Cumulative
    override fun valueOf(q3q4: Long) = this.also {
        it.q3 = q3q4.high
        it.q4 = q3q4.low
    }

    override fun out(operationResult: Int128) = operationResult
}