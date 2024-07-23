package io.github.aeckar.numerics

import io.github.aeckar.numerics.functions.sqrt

/**
 * A three-dimensional vector.
 */
public class Vector3(public val first: Rational, public val second: Rational, public val third: Rational) {
    /**
     * Returns the vector equal to the sum.
     * @throws NumericUndefinedException the total dimensions of this vector is not the same as the other
     */
    public operator fun plus(other: Vector3): Vector3 {
        return Vector3(first + other.first, second + other.second, third + other.third)
    }

    /**
     * Returns the vector equal to the subtraction.
     * @throws NumericUndefinedException the total dimensions of this vector is not the same as the other
     */
    public operator fun minus(other: Vector3): Vector3 {
        return Vector3(first - other.first, second - other.second, third - other.third)
    }

    /**
     * Returns the dot (scalar) product of this vector times the other.
     * @throws NumericUndefinedException the total dimensions of this vector is not the same as the other
     */
    public infix fun dot(other: Vector3): Rational {
        return (first * other.first) + (second * other.second) + (third * other.third)
    }

    /**
     * Returns the magnitude (norm) of this vector.
     */
    public fun magnitude(): Rational = sqrt(first.pow(2) + second.pow(2) + third.pow(2))

    /**
     * Returns the cross product (vector product) of this vector times the other.
     *
     * This operation is *not* commutative.
     * @throws NumericUndefinedException either vector is not 3-dimensional
     */
    public fun cross(other: Vector3): Vector3 {
        val first = (second * other.third) - (third * other.second)
        val second = (third * other.first) - (first * other.third)
        val third = (first * other.second) - (second * other.first)
        return Vector3(first, second, third)
    }

    /**
     * Returns a string representation of this vector using angle brackets
     * according to the supplied function.
     */
    public inline fun toString(transform: (Rational) -> String): String {
        return "<${transform(first)}, ${transform(second)}, ${transform(third)}>"
    }

    /**
     * Returns a string representation of this vector using angle brackets.
     */
    override fun toString(): String = toString { it.toString() }

    override fun hashCode(): Int {
        var hash = 7
        hash = 31 * hash + first.hashCode()
        hash = 31 * hash + second.hashCode()
        hash = 31 * hash + third.hashCode()
        return hash
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Vector3) {
            return false
        }
        return first == other.first && second == other.second && third == other.third
    }
}