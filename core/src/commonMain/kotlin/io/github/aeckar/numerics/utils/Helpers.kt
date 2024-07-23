package io.github.aeckar.numerics.utils

internal const val LONG_MAX_STRING = "9223372036854775807"

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

/**
 * Returns true if the sum is the result of a signed integer overflow.
 *
 * If a result of multiple additions must be checked, this function must be called for each intermediate sum.
 * Also checks for the case [Int.MIN_VALUE] - 1.
 */
internal fun addOverflowsValue(x: Int, y: Int) = (x.toLong() + y) !in Int.MIN_VALUE..Int.MAX_VALUE

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