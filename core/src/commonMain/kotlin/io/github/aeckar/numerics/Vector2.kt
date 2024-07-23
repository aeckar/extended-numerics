package io.github.aeckar.numerics

import io.github.aeckar.numerics.functions.arctan
import io.github.aeckar.numerics.functions.sqrt

/**
 * A two-dimensional vector.
 */
public open class Vector2(public val first: Rational, public val second: Rational) {
    /**
     * Returns the vector equal to the sum.
     * @throws NumericUndefinedException the total dimensions of this vector is not the same as the other
     */
    public operator fun plus(other: Vector2): Vector2 = Vector2(first + other.first, second + other.second)

    /**
     * Returns the vector equal to the subtraction.
     * @throws NumericUndefinedException the total dimensions of this vector is not the same as the other
     */
    public operator fun minus(other: Vector2): Vector2 = Vector2(first - other.first, second - other.second)

    /**
     * Returns the dot (scalar) product of this vector times the other.
     * @throws NumericUndefinedException the total dimensions of this vector is not the same as the other
     */
    public infix fun dot(other: Vector2): Rational = (first * other.first) + (second * other.second)

    /**
     * Returns the magnitude (norm) of this vector.
     */
    public fun magnitude(): Rational = sqrt(first.pow(2) + second.pow(2))

    /**
     * Returns the angle between this vector and the positive x-axis.
     *
     * Assumes that this vector has two components.
     * The returned value is within the range (`-`[pi][Rational.PI], `pi`].
     * @throws NumericOverflowException this vector is not 2-dimensional
     */
    public fun angle(): Rational = arctan(second / first)

    /**
     * Returns the angle between this vector and the other.
     */
    public fun angleFrom(other: Vector2): Rational = (this dot other) / (this.magnitude() * other.magnitude())

    /**
     * Returns a string representation of this vector using angle brackets
     * according to the supplied function.
     */
    public inline fun toString(transform: (Rational) -> String): String = "<${transform(first)}, ${transform(second)}>"

    /**
     * Returns a string representation of this vector using angle brackets.
     */
    override fun toString(): String = toString { it.toString() }

    override fun hashCode(): Int {
        var hash = 7
        hash = 31 * hash + first.hashCode()
        hash = 31 * hash + second.hashCode()
        return hash
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Vector2) {
            return false
        }
        return first == other.first && second == other.second
    }
}