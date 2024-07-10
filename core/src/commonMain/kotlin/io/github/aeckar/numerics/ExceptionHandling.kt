package io.github.aeckar.numerics


private fun Any.name() = when (this) {
    is Rational, is Rational.Companion -> "Rational number"
    is Int128, is Int128.Companion -> "128-bit integer"
    is Matrix -> "Matrix"
    is Table<*> -> "Table"
    is Vector -> "Vector"
    else -> "Value" // Receiver is top-level function
}

/**
 * @throws CompositeUndefinedException always
 */
internal fun raiseUndefined(message: String): Nothing = throw CompositeUndefinedException(message)

/**
 * The name of the expected result may be inferred from the composite number receiver type or companion.
 * @throws CompositeOverflowException always
 */
internal fun Any.raiseOverflow(
    additionalInfo: String? = null,
    cause: Throwable? = null
): Nothing {
    val info = additionalInfo?.let { " ($it)" }.orEmpty()
    throw CompositeOverflowException("${name()} overflows$info", cause)
}

/**
 * The name of the expected result may be inferred from the composite number receiver type or companion.
 * @throws CompositeFormatException always
 */
internal fun Any.raiseIncorrectFormat(
    reason: String,
    argument: String,
    cause: Throwable? = null
): Nothing {
    throw CompositeFormatException(
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
 * Thrown when an operation involving [composite numbers][CompositeNumber] or [matrices][Matrix]
 * cannot proceed due to overflow or an undefined result.
 */
public class CompositeOverflowException internal constructor(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

/**
 * Thrown when an operation involving [composite numbers][CompositeNumber],
 * vectors[Vector] or [matrices][Matrix] has a result that is undefined.
 *
 * To derive a non-real result, consider using [Complex].
 */
public class CompositeUndefinedException internal constructor(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

/**
 * Thrown from a `String`-arg pseudo-constructor for a [composite number][CompositeNumber] class to
 * indicate that the supplied string is malformed.
 */
public class CompositeFormatException internal constructor(
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