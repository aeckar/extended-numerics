package io.github.aeckar.numerics

/**
 * A real number.
 *
 * Provides an interface for classes extending the capabilities of built-in [numeric][Number] types
 * without providing for arbitrary-precision arithmetic.
 *
 * Instances can be converted through truncation to these numeric types.
 * They may also be converted to and from their respective string representation.
 * Conversions from other types using the constructors provided by implementations result in no information loss.
 *
 * Instances of this class are:
 * - Immutable: Public state cannot be modified
 * - Unique: There exists only one possible state for a given value
 * - Limited precision: Precision is fixed to a given number of binary digits (however, they may be scaled)
 * - Efficient: Fixed-precision allows for certain key optimizations to be made.
 * - Accurate: If the result of an operation is too large or small to be represented accurately
 * as a `Real`, such as in the event of an integer overflow, a [NumericOverflowException] is thrown
 *
 * Results of computationally expensive operations are not cached,
 * and should be stored in a variable if used more than once.
 * The one exception to this is [toString].
 *
 * When a value is described as being "too large", it is either
 * too high or too low to be accurately represented as the given real number type.
 *
 * This class, unlike other numeric classes, does not supply a `unaryPlus()` operator
 * because of its uselessness and tendency to create bugs.
 * @param T the inheritor of this class
 * @see Int128
 * @see Rational
 */
@Suppress("EqualsOrHashCode")
public sealed class Real<T : Real<T>> : Number(), Comparable<T> {
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

    // ------------------------------ arithmetic ------------------------------

    /**
     * Returns an instance equal in value to the absolute value of this
     */
    @Suppress("UNCHECKED_CAST")
    public fun abs(): T = if (sign < 0) -this else this as T

    /**
     * Returns an instance equal in value to the difference.
     */
    public operator fun minus(other: T): T = this + (-other)

    /**
     * Returns -1 if this is negative, 0 if zero, or 1 if positive.
     * @see sign
     */
    public abstract fun signum(): Int

    /**
     * Returns an instance equal in value to this, negated.
     */
    public abstract operator fun unaryMinus(): T

    /**
     * Returns an instance equal in value to the sum.
     */
    public abstract operator fun plus(other: T): T

    /**
     * Returns an instance equal in value to the product.
     */
    public abstract operator fun times(other: T): T

    /**
     * Returns an instance equal in value to the quotient.
     * @throws NumericUndefinedException the other number is 0
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
     * Comparison to `Real`s, built-in [numbers][Number], and
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