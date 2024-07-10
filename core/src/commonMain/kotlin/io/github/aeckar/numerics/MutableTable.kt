@file:JvmName("Tables")
@file:JvmMultifileClass
package io.github.aeckar.numerics

import io.github.aeckar.numerics.utils.twoDimensionalArray

/**
 * Returns a mutable table with the given dimensions, with each entry initialized according to the given logic.
 * @throws TableDimensionsException either dimension is negative or 0
 */
@JvmName("newMutableInstance")
public inline fun <E : Any> MutableTable(
    rows: Int,
    columns: Int,
    defaultEntry: (rowIndex: Int, columnIndex: Int) -> E
): MutableTable<E> {
    val table = MutableTable<E>(twoDimensionalArray(rows, columns))
    table.forEachRow {
        forEachInRow { table[rowIndex, columnIndex] = defaultEntry(rowIndex, columnIndex) }
    }
    return table
}

/**
 * Returns a mutable table with the given dimensions.
 *
 * If [defaultEntry] is not null, every entry is initialized to it.
 * @throws TableDimensionsException either dimension is negative or 0
 */
@JvmName("newMutableInstance")
public fun <E : Any> MutableTable(rows: Int, columns: Int, defaultEntry: E? = null): MutableTable<E> {
    return MutableTable(twoDimensionalArray(rows, columns, defaultEntry))
}

/**
 * A mutable table of entries.
 *
 * Instances are not thread-safe.
 */
public class MutableTable<E : Any> @PublishedApi internal constructor(
    backingArray: Array<Array<Any?>>
) : Table<E>(backingArray) {
    /**
     * Assigns [entry] to the entry at the specified index.
     * @throws NoSuchElementException the index lies outside the bounds of the table
     */
    public operator fun set(rowNumber: Int, columnNumber: Int, entry: E) {
        try {
            backingArray[rowNumber][columnNumber] = entry
        } catch (e: ArrayIndexOutOfBoundsException) {
            raiseOutOfBounds(rowNumber, columnNumber)
        }
    }
}