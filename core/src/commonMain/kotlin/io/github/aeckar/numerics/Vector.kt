package io.github.aeckar.numerics

import io.github.aeckar.numerics.functions.arctan
import io.github.aeckar.numerics.functions.sqrt
import kotlinx.serialization.Serializable

/**
 * Returns a vector with the given components.
 */
public fun Vector(component1: Rational, component2: Rational, vararg components: Rational): Vector {
    val array = Array(components.size + 2) { component1 }
    array[1] = component2
    components.copyInto(array, destinationOffset = 2)
    return Vector(array)
}

/**
 * A vector of rational components with magnitude and direction.
 */
@Serializable
public class Vector internal constructor(@PublishedApi internal val components: Array<Rational>) {
    /**
     * The total number of components in this vector.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    public val dimensions: Int get() = components.size

    private fun ensureEqualDimensions(other: Vector, operation: String) {
        if (this.dimensions != other.dimensions) {
            raiseUndefined("$operation is undefined for vectors of different dimensions")
        }
    }

    /**
     * Returns the value of the component at [index].
     * @throws NoSuchElementException the specified component does not exist
     */
    public operator fun get(index: Int): Rational {
        return try {
            components[index]
        } catch(e: NoSuchElementException) {
            throw NoSuchElementException("Component $index is undefined for vector of size $dimensions", e)
        }
    }

    /**
     * Returns the first component of this vector.
     */
    public operator fun component1(): Rational = this[0]

    /**
     * Returns the second component of this vector.
     */
    public operator fun component2(): Rational = this[1]

    /**
     * Returns the third component of this vector.
     * @throws NoSuchElementException the third component does not exist ([dimensions] = 2)
     */
    public operator fun component3(): Rational = this[2]

    /**
     * Returns the vector equal to the sum.
     * @throws CompositeUndefinedException the total dimensions of this vector is not the same as the other
     */
    public operator fun plus(other: Vector): Vector {
        ensureEqualDimensions(other, operation = "Addition")
        return Vector(Array(dimensions) { this[it] + other[it] })
    }

    /**
     * Returns the vector equal to the subtraction.
     * @throws CompositeUndefinedException the total dimensions of this vector is not the same as the other
     */
    public operator fun minus(other: Vector): Vector {
        ensureEqualDimensions(other, operation = "Subtraction")
        return Vector(Array(dimensions) { this[it] - other[it] })
    }

    /**
     * Returns the dot product (scalar product) of this vector times the other.
     * @throws CompositeUndefinedException the total dimensions of this vector is not the same as the other
     */
    public operator fun times(other: Vector): Rational {
        ensureEqualDimensions(other, operation = "Scalar product")
        val sum = MutableRational(Rational.ZERO)
        repeat(dimensions) { sum +/* = */ (this[it] * other[it]) }
        return sum.immutable()
    }

    /**
     * Returns the cross product (vector product) of this vector times the other.
     *
     * This operation is *not* commutative.
     * @throws CompositeUndefinedException either vector is not 3-dimensional
     */
    public fun cross(other: Vector): Vector {
        if (dimensions != 3 || other.dimensions != 3) {
            raiseUndefined("Cross product is undefined for vectors that are not 3-dimensional")
        }
        val components = Array(3) { Rational.ZERO }
        val u = this.components
        val v = other.components
        components[0] = u[1] * v[2] - u[2] * v[1]
        components[1] = u[2] * v[0] - u[0] * v[2]
        components[2] = u[0] * v[1] - u[1] * v[0]
        return Vector(components)
    }

    /**
     * Returns the magnitude (norm) of this vector.
     */
    public fun magnitude(): Rational {
        val sum = MutableRational(Rational.ZERO)
        components.forEach { sum +/* = */ it.pow(2) }
        return sqrt(sum).immutable()
    }

    /**
     * Returns the angle between this vector and the positive x-axis.
     *
     * Assumes that this vector has two components.
     * The returned value is within the range (`-`[pi][Rational.PI], `pi`].
     * @throws CompositeOverflowException this vector is not 2-dimensional
     */
    public fun angle(): Rational = arctan(components[1] / components[0])    // TODO check range

    /**
     * Returns the angle between this vector and the other.
     */
    public fun angleFrom(other: Vector): Rational = (this * other) / (this.magnitude() * other.magnitude())

    /**
     * Returns a string representation of this vector using angle brackets.
     *
     * Each component is transformed into its string representation using the given function.
     */
    public inline fun toString(transform: (Rational) -> String): String = buildString {
        append("<")
        repeat(dimensions) {
            append(transform(components[it]))
            append(", ")
        }
        delete(length - 2, length)  // Remove trailing comma and space
        append(">")
    }

    /**
     * Returns a string representation of this vector using angle brackets.
     *
     * Each component is transformed into its string representation using [Any.toString].
     */
    override fun toString(): String = toString(Rational::toString)

    override fun hashCode(): Int {
        var hash = 7
        components.forEach { hash = 31 * hash + it.hashCode() }
        return hash
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Vector) {
            return false
        }
        return components.contentEquals(other.components)
    }
}