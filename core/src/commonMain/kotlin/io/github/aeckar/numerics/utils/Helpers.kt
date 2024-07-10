package io.github.aeckar.numerics.utils

internal const val LONG_MAX_STRING = "9223372036854775807"

/**
 * Passed to a constructor of a class to distinguish it from a pseudo-constructor with the same arguments.
 */
internal object PrivateAPIFlag

/**
 * C-style boolean-to-integer conversion. 1 if true, 0 if false.
 */
internal fun Boolean.toInt() = if (this) 1 else 0

/**
 * Returns the base-10 scale of the given value.
 *
 * Assumes [x] is non-negative.
 */
internal fun scaleOf(x: Long): Int {
    var value = x
    var scale = 0
    while (value != 0L && value % 10 == 0L) {
        value /= 10
        ++scale
    }
    return scale
}

internal fun tenPow(scale: Int): Long {
    var result = 1L
    repeat(scale) { result *= 10 }
    return result
}

/**
 * Resultant sign represented as 1 or -1.
 * @return the sign of the product/quotient of the two values
 */
internal fun productSign(x: Int, y: Int) = if ((x < 0) == (y < 0)) 1 else -1

@PublishedApi
internal fun twoDimensionalArray(rows: Int, columns: Int, defaultEntry: Any? = null): Array<Array<Any?>> {
    return Array(rows) {
        Array(columns) { defaultEntry }
    }
}

/**
 * Assumes the receiver is 2-dimensional.
 */
internal fun Array<Array<Any?>>.deepCopyOf() = Array(size) { rowIndex ->
    Array(this[rowIndex].size) { this[rowIndex][it] }
}