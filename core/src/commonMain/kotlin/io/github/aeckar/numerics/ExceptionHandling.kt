package io.github.aeckar.numerics

private fun Any.name() = when (this) {
    is Rational, is Rational.Companion -> "Rational number"
    is Int128, is Int128.Companion -> "128-bit integer"
    is Matrix -> "Matrix"
    is Table<*> -> "Table"
    else -> "Value" // Receiver is top-level function
}

/**
 * @throws NumericUndefinedException always
 */
internal fun raiseUndefined(message: String): Nothing = throw NumericUndefinedException(message)

/**
 * The name of the expected result may be inferred from the receiver type.
 * @throws NumericOverflowException always
 */
internal fun Any.raiseOverflow(
    additionalInfo: String? = null,
    cause: Throwable? = null
): Nothing {
    val info = additionalInfo?.let { " ($it)" }.orEmpty()
    throw NumericOverflowException("${name()} overflows$info", cause)
}

/**
 * The name of the expected result may be inferred from the receiver type.
 * @throws NumericFormatException always
 */
internal fun Any.raiseIncorrectFormat(
    reason: String,
    argument: String,
    cause: Throwable? = null
): Nothing {
    throw NumericFormatException(
            "String \"$argument\" does not contain a ${name().lowercase()} in the correct format ($reason)", cause)
}

/**
 * @throws TableDimensionsException always
 */
internal fun raiseInvalidDimensions(rows: Int, columns: Int, cause: Throwable? = null): Nothing {
    throw TableDimensionsException(rows, columns, cause)
}

/**
 * @throws NoSuchElementException always
 */
internal fun Table<*>.raiseOutOfBounds(rowIndex: Int, columnIndex: Int): Nothing {
    throw NoSuchElementException(
        "Index [$rowIndex, $columnIndex] lies outside the bounds of the table " +
                "(rows = $rows, columns = $columns)"
    )
}

/**
 * Thrown when an operation involving numeric types cannot proceed due to overflow or underflow.
 */
public class NumericOverflowException internal constructor(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

/**
 * Thrown when an operation involving numeric types has a result that is undefined.
 *
 * To derive a non-real result, consider using [Complex].
 */
public class NumericUndefinedException internal constructor(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

/**
 * Thrown from a `String`-arg pseudo-constructor for a numeric type to indicate that the supplied string is malformed.
 */
public class NumericFormatException internal constructor(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

/**
 * Thrown when a [table][Table] is initialized with invalid dimensions.
 */
public class TableDimensionsException internal constructor(
    rows: Int,
    columns: Int,
    cause: Throwable? = null
) : Exception("Table has invalid dimensions (rows = $rows, columns = $columns)", cause)