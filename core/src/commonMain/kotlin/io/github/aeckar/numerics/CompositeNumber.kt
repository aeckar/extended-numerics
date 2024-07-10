package io.github.aeckar.numerics

// TODO create checkers for annotations using Checker Framework

/**
 * A numeric value composed of multiple primitive values.
 *
 * Provides an interface for classes extending the capabilities of built-in [numeric][Number] types
 * without providing for arbitrary-precision arithmetic.
 *
 * Instances can be converted through truncation to these numeric types.
 * Additionally, composite numbers may also be converted to and from their respective string representation.
 * Conversions from other types using the constructors provided by implementations result in no information loss.
 *
 * Instances of this class are:
 * - Immutable: Public state cannot be modified
 * - Unique: There exists only one possible state for a given value
 * - Limited precision: Precision is fixed to a given number of binary digits (however, they may be scaled)
 * - Efficient: Fixed-precision allows for certain key optimizations to be made.
 * - Accurate: If the result of an operation is too large or small to be represented accurately
 * as a composite number, such as in the event of an integer overflow, a [CompositeOverflowException] is thrown
 *
 * Results of computationally expensive operations are not cached,
 * and should be stored in a variable if used more than once.
 * The one exception to this is [toString].
 *
 * When a value is described as being "too large", it is either
 * too high or too low to be accurately represented as the given composite number type.
 *
 * Composite numbers, like the other numeric classes, do not supply a `unaryPlus()` operator
 * because of its uselessness and tendency to create bugs.
 * @param T the inheritor of this class
 * @see Int128
 * @see Rational
 */
@Suppress("EqualsOrHashCode")
public sealed class CompositeNumber<T : CompositeNumber<T>> : Number(), Comparable<T> {
    /**
     * -1 if this value is negative, else 1.
     *
     * Should be used instead of [signum] when equality to 0 is irrelevant.
     */
    public abstract val sign: Int

    /**
     * True if this value is negative.
     */
    public abstract val isNegative: Boolean // Int128 uses special implementation

    /**
     * True if this value is positive.
     */
    public abstract val isPositive: Boolean // Int128 uses special implementation

    /**
     * Implemented by constants present within the companion object of [T].
     *
     * Overrides [toString] to avoid unnecessary cache validation.
     */
    protected interface Constant {
        public val stringLiteral: String
    }

    // ------------------------------ mutability --------------------

    /*
        Conversion between mutable and immutable instances should be restricted the specific inheritor.
        Operations with argument(s) of type T that utilize mutability will generally reside with the same class.
        Restricting this functionality from the user reduces the chances of mutability
        being used incorrectly by causing unwanted side effects.

        Instances where a value is being mutated should be explicitly stated using comments.
        Mutable instances should not be declared using `var`, as it may cause aliasing.
     */

    /**
     * Returns an immutable composite number equal in value to this.
     *
     * If chained to an operation, this function should be called second.
     * If the caller is guaranteed to be immutable, this function does nothing.
     *
     * Overrides of this function should never be marked final.
     */
    internal abstract fun immutable(): T

    /**
     * Returns a mutable composite number equal in value to this.
     *
     * If chained to an operation, this function should be called first.
     * If the caller is guaranteed to be mutable, this function does nothing.
     *
     * Overrides of this function should never be marked final.
     */
    @Cumulative
    internal abstract fun mutable(): T

    /**
     * Returns a unique mutable composite number equal in value to this.
     *
     * If chained to an operation, this function should be called first.
     */
    internal abstract fun uniqueMutable(): T

    /**
     * Value function that returns a new instance with the given value,
     * or if [mutable], the same instance with the value stored.
     */
    @Cumulative
    internal abstract fun valueOf(other: T): T

    // ------------------------------ arithmetic ------------------------------

    /**
     * Returns an instance equal in value to the absolute value of this
     */
    @Suppress("UNCHECKED_CAST")
    @Cumulative
    public fun abs(): T = if (sign < 0) -this else this as T

    /**
     * Returns an instance equal in value to the difference.
     */
    @Cumulative
    public operator fun minus(other: T): T = this + (-other)

    /**
     * Returns -1 if this is negative, 0 if zero, or 1 if positive.
     * @see sign
     */
    public abstract fun signum(): Int

    /**
     * Returns an instance equal in value to this, negated.
     */
    @Cumulative
    public abstract operator fun unaryMinus(): T

    /**
     * Returns an instance equal in value to the sum.
     */
    @Cumulative
    public abstract operator fun plus(other: T): T

    /**
     * Returns an instance equal in value to the product.
     *
     * This function is [cumulative][Cumulative] when neither argument is 0.
     */
    public abstract operator fun times(other: T): T

    /**
     * Returns an instance equal in value to the quotient.
     * @throws CompositeUndefinedException the other number is 0
     */
    public abstract operator fun div(other: T): T

    /**
     * Returns an instance equal in value to the remainder of the division.
     *
     * The returned value is always non-negative.
     */
    public abstract operator fun rem(other: T): T

    /**
     * Returns an instance equal in value to this raised to [power].
     *
     * When this and `power` are both 0, this function returns a value equal to 1.
     */
    public abstract fun pow(power: Int): T

    // ------------------------------ comparison ------------------------------

    /**
     * Compares this value to the other.
     *
     * See [compareTo][Comparable.compareTo] for details.
     */
    public operator fun compareTo(value: Int): Int = compareTo(value.toLong())

    /**
     * Compares this value to the other.
     *
     * See [compareTo][Comparable.compareTo] for details.
     */
    public operator fun compareTo(value: Long): Int = if (!this.isLong()) sign else toLong().compareTo(value)

    /**
     * Comparison to composite numbers, built-in [numbers][Number], and
     * strings agreeing with the format specified by the string constructor allowed.
     *
     * Does not test for equality to arbitrary-precision numbers. To do this, use compareTo instead.
     * @return true if the numerical values of the objects are equal
     */
    final override fun equals(other: Any?): Boolean = when (other) {
        is Rational -> other stateEquals toRational()
        is Int128 -> other stateEquals toInt128()

        is String -> {
            if (this is Rational) Rational(other) stateEquals this else Int128(other).stateEquals(this as Int128)
        }

        is Double, is Float -> (other as Number).toDouble() == toDouble()
        is Number -> isLong() && other.toLong() == toLong()
        else -> false
    }

    abstract override fun hashCode(): Int

    internal abstract infix fun stateEquals(other: T): Boolean

    /**
     * If true, this can be represented as a 64-bit integer without losing information.
     */
    internal abstract fun isLong(): Boolean

    // ------------------------------ conversions ------------------------------

    final override fun toByte(): Byte = toInt().toByte()
    final override fun toShort(): Short = toInt().toShort()
    final override fun toFloat(): Float = toDouble().toFloat()

    /**
     * Returns a complex number equal in value to this.
     */
    public abstract fun toComplex(): Complex

    /**
     * Returns a rational number equal in value to this.
     */
    public abstract fun toRational(): Rational

    /**
     * Returns a 128-bit integer equal in value to this.
     */
    public abstract fun toInt128(): Int128

    /**
     * Returns a string representation of this value.
     *
     * When passed to the string constructor of the inheritor, creates an instance equal in value to this.
     */
    abstract override fun toString(): String
}