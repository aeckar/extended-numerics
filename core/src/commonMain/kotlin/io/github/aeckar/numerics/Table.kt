@file:JvmName("Tables")
@file:JvmMultifileClass
package io.github.aeckar.numerics

import io.github.aeckar.numerics.utils.deepCopyOf
import io.github.aeckar.numerics.utils.twoDimensionalArray
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

/**
 * Returns an immutable table with the given dimensions, with each entry initialized according to the given logic.
 * @throws TableDimensionsException either dimension is negative or 0
 */
@JvmName("newInstance")
public inline fun <E : Any> Table(
    rows: Int,
    columns: Int,
    defaultEntry: (rowIndex: Int, columnIndex: Int) -> E
): Table<E> = Table(MutableTable(rows, columns, defaultEntry).backingArray)

/**
 * Returns an immutable table with the given dimensions.
 *
 * If [defaultEntry] is not null, every entry is initialized to it.
 * @throws TableDimensionsException either dimension is negative or 0
 */
@JvmName("newInstance")
public fun <E : Any> Table(rows: Int, columns: Int, defaultEntry: E? = null): Table<E> {
    return try {
        Table(twoDimensionalArray(rows, columns, defaultEntry))
    } catch (e: NegativeArraySizeException) {
        raiseInvalidDimensions(rows, columns, e)
    }
}

/**
 * An immutable table of entries.
 *
 * Entries use a zero-based indexing system.
 */
@Serializable
public open class Table<E : Any> @PublishedApi internal constructor(
    @PublishedApi internal val backingArray: Array<Array<@Contextual Any?>>
) {
    init {
        if (backingArray.isEmpty() || backingArray[0].isEmpty()) {
            val rowCount = backingArray.size
            val columnCount = if (backingArray.isNotEmpty()) backingArray[0].size else 0
            raiseInvalidDimensions(rowCount, columnCount)
        }
    }

    /**
     * The total number of rows in this table.
     */
    public val rows: Int get() = backingArray.size

    /**
     * The total number of columns in this table.
     */
    public val columns: Int get() = backingArray[0].size

    /**
     * Returns the entries in this table as a 2-dimensional, untyped array.
     *
     * Operations performed on the returned array will not mutate the entries in this table.
     */
    public fun toUntypedArray(): Array<Array<Any?>> = backingArray.deepCopyOf()

    // ------------------------------ access and modification ------------------------------

    /**
     * Returns the entry at the specified index.
     * @throws NoSuchElementException the specified entry has not been initialized or
     * the index lies outside the bounds of the table
     */
    @Suppress("UNCHECKED_CAST")
    public operator fun get(rowNumber: Int, columnNumber: Int): E {
        return try {
            backingArray[rowNumber][columnNumber] as E
        } catch (e: NullPointerException) {
            throw NoSuchElementException("Attempted access of uninitialized table element", e)
        } catch (_: ArrayIndexOutOfBoundsException) {
            raiseOutOfBounds(rowNumber, columnNumber)
        }
    }

    // ------------------------------ iteration ------------------------------

    /**
     * Passes each entry in the specified row, in order, to the given function.
     */
    public inline fun forEachInRow(rowNumber: Int, action: (entry: E) -> Unit) {
        repeat(columns) { action(this[rowNumber, it]) }
    }

    /**
     * Passes each entry in the specified column, in order, to the given function.
     */
    public inline fun forEachInColumn(columnNumber: Int, action: (entry: E) -> Unit) {
        repeat(rows) { action(this[columnNumber, it]) }
    }

    /**
     * Iterates through every row in this table.
     */
    public inline fun forEachRow(action: RowView.() -> Unit) {
        repeat(rows) { RowView(it).apply(action) }
    }

    /**
     * Iterates through every column in this table.
     */
    public inline fun forEachColumn(action: ColumnView.() -> Unit) {
        repeat(columns) { ColumnView(it).apply(action) }
    }

    /**
     * A row in this table whose entries can be iterated through.
     *
     * @param rowIndex the location of this row
     */
    public inner class RowView(rowIndex: Int) {
        public var rowIndex: Int = rowIndex
            private set

        /**
         * Iterates through each entry in this row.
         */
        public inline fun forEachInRow(action: ColumnView.(entry: E) -> Unit) {
            val column = ColumnView(0)
            do {
                action(column, this@Table[rowIndex, column.columnIndex])
                column.inc()
            } while (column.columnIndex < columns)
        }

        @PublishedApi internal fun inc() {
            ++rowIndex
        }
    }

    /**
     * A column in this table whose entries can be iterated through.
     *
     * @param columnIndex the location of this column
     */
    public inner class ColumnView(columnIndex: Int) {
        public var columnIndex: Int = columnIndex
            private set

        /**
         * Iterates through each entry in this column.
         */
        public inline fun forEachInColumn(action: RowView.(entry: E) -> Unit) {
            val row = RowView(0)
            do {
                action(row, this@Table[columnIndex, row.rowIndex])
                row.inc()
            } while (row.rowIndex < rows)
        }

        @PublishedApi internal fun inc() {
            ++columnIndex
        }
    }

    // ------------------------------ string conversion ------------------------------

    /**
     * Returns a string representation of this table in Markdown format.
     *
     * Each entry is transformed into its string representation using the given function.
     */
    public open fun toString(transform: (E) -> String): String {
        val columnCount = columns
        val entries = MutableTable<String>(rows, columnCount)
        val maxLengths = IntArray(columnCount)
        forEachRow {
            forEachInRow { entry ->
                entries[rowIndex, columnIndex] = transform(entry)
                maxLengths[columnIndex] = maxLengths[columnIndex].coerceAtLeast(entries[rowIndex, columnIndex].length)
            }
        }
        return buildString {
            entries.forEachRow {
                if (this@Table !is Matrix) {
                    maxLengths.forEach {
                        append("+-")
                        repeat(it) { append('-') }
                    }
                    append('+')
                }
                append("|")
                forEachInRow { entry ->
                    repeat(maxLengths[columnIndex] - entry.lastIndex) { append(' ') }  // Right-justify
                    append(entry)
                    if (this@Table !is Matrix) {
                        append("| ")
                    }
                }
                append(" |\n")
            }
            deleteAt(lastIndex) // Remove trailing newline
        }
    }

    /**
     * Returns a string representation of this table in Markdown format.
     *
     * Each entry is transformed into its string representation using [Any.toString].
     */
    override fun toString(): String = toString(Any::toString)

    // ------------------------------ comparison ------------------------------

    final override fun hashCode(): Int {
        var hash = 7
        var index = 0
        hash = hash * 31 + rows
        hash = hash * 31 + columns
        while (index < rows && index < columns) {   // Consider objects from main diagonal
            hash = hash * 31 + this[index, index].hashCode()
            ++index
        }
        return hash
    }

    final override fun equals(other: Any?): Boolean {
        if (other !is Table<*> || rows != other.rows || columns != other.columns) {
            return false
        }
        repeat(rows) { rowIndex ->
            repeat(columns) { columnIndex ->
                if (this[rowIndex, columnIndex] != other[rowIndex, columnIndex]) {
                    return false
                }
            }
        }
        return true
    }
}