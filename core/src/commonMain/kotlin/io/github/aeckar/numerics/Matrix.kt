@file:JvmName("Matrices")
package io.github.aeckar.numerics

import io.github.aeckar.numerics.Rational.Companion.ZERO
import io.github.aeckar.numerics.Rational.Companion.ONE
import io.github.aeckar.numerics.utils.deepCopyOf
import kotlinx.serialization.Contextual

/**
 * Returns a matrix equal in value to the table of rational numbers.
 *
 * Operations performed on [table] will not mutate the entries in the returned matrix.
 */
@JvmName("newInstance")
public fun Matrix(table: Table<Rational>): Matrix {
    return Matrix(if (table is MutableTable) table.backingArray.deepCopyOf() else table.backingArray)
}

/**
 * A 2-dimensional matrix of rational values.
 *
 * To create a new instance, use the following syntax:
 * ```kotlin
 * val myMatrix = Matrix[2,3](
 *     2, 6, 9
 *     4, 0, 1
 * )
 * ```
 *
 * Designed specifically for computation with minimal information loss.
 * Performance-sensitive applications should use a vector arithmetic library instead.
 *
 * Instances of this class are immutable.
 *
 * Results of computationally expensive operations are not cached,
 * and should be stored in a variable if used more than once.
 * The exceptions to this are [ref], [toString], and partly [rref].
 */
public class Matrix internal constructor(backingArray: Array<Array<@Contextual Any?>>) : Table<Rational>(backingArray) {
    private var lazyRowEchelonForm: Matrix? = null

    /**
     * Returns a string representation of this matrix.
     *
     * Each entry is transformed into its string representation using the given function.
     */
    @Suppress("RedundantOverride")
    override fun toString(transform: (Rational) -> String): String = super.toString(transform)

    /**
     * Returns a string representation of this matrix.
     *
     * Each entry is transformed into its string representation using [Any.toString].
     */
    override fun toString(): String = super.toString(Any::toString)

    /**
     * Used to create a matrix using the dimensions this instance was given.
     */
    public class Template(private val rows: Int, private val columns: Int) {
        /**
         * Returns a new matrix with the given entries.
         */
        @JvmName("withEntries")
        public operator fun invoke(vararg entries: Int): Matrix {
            ensureValidSize(entries.size)
            val entry = entries.iterator()
            val table = Table(rows, columns) { _, _ -> entry.nextInt().toRational() }
            return Matrix(table)
        }

        /**
         * Returns a new matrix with the given entries.
         */
        @JvmName("withEntries")
        public operator fun invoke(vararg entries: Rational): Matrix {
            ensureValidSize(entries.size)
            val entry = entries.iterator()
            val table = Table(rows, columns) { _, _ -> entry.next() }
            return Matrix(table)
        }

        private fun ensureValidSize(size: Int) {
            require (size != 0) { "Matrix cannot be empty" }
            require (size / rows == columns && size % columns == 0) {
                "Entries do not conform to matrix dimensions"
            }
        }
    }

    // ------------------------------ transformations ------------------------------

    /**
     * Returns the transpose of this matrix.
     *
     * The transpose is the matrix where the rows and columns are swapped.
     */
    public fun transpose(): Matrix {
        val table = Table(columns, rows) { rowIndex, columnIndex -> this[columnIndex, rowIndex] }
        return Matrix(table)
    }

    /**
     * Returns the inverse of this matrix.
     *
     * The inverse is the matrix, when multiplied by this matrix, results in an identity matrix.
     * @throws CompositeUndefinedException this matrix is not a square matrix
     */
    public fun inverse(): Matrix {
        "Inverse".requiresSquareMatrix()

        TODO("Not implemented yet")
    }

    /**
     * Returns this matrix as if it were in row echelon form (REF).
     *
     * The REF is the matrix, after applying elementary row operations, where:
     * - All entries below the main diagonal are zero
     * - All entries on the main diagonal are one
     * - All zero rows are on the bottom
     *
     * Furthermore, we can describe the elementary row operations as follows:
     * - Multiplication by a non-zero scalar
     * - Addition by another row
     * - Swap with another row
     * @see rref
     */
    public fun ref(): Matrix {
        lazyRowEchelonForm?.let { return it }

        TODO("Not implemented yet")
    }

    /**
     * Returns this matrix as if it were in reduced row echelon form (RREF).
     *
     * The RREF is the matrix, after applying elementary row operations, where
     * the entries are in row echelon form and every entry above the main diagonal is zero.
     *
     * For augmented matrices in RREF, the rightmost column of the matrix will always contain
     * the solutions to the linear system which the matrix represents.
     *
     * For an explanation of elementary row operations, see [ref].
     */
    public fun rref(): Matrix {
        val ref = ref()

        TODO("Not implemented yet")
    }

    // ------------------------------ arithmetic --------------------

    /**
     * Returns the determinant of this matrix.
     *
     * The determinant can be defined recursively as
     * @throws CompositeUndefinedException this matrix is not square
     */
    public fun determinant(): Rational {
        "Determinant".requiresSquareMatrix()
        return determinant(0, -1).immutable()
    }

    /**
     * Assumes this matrix is square.
     */
    private fun determinant(rowPivot: Int, columnPivot: Int): Rational {
        val sideLength = rows - rowPivot
        if (sideLength == 2) {
            /*
                    | a b |
                A = | c d |
             */
            val column = if (columnPivot == 0) 1 else 0
            val nextColumn = column + if (columnPivot == column + 1) 2 else 1
            val ad = this[rowPivot, column] * this[rowPivot + 1, nextColumn]
            val bc = this[rowPivot, nextColumn] * this[rowPivot + 1, column]
            return ad - bc
        }
        var negateTerm = false
        var skippedColumn = false
        val result = MutableRational(ZERO)
        repeat(sideLength) {
            if (it == columnPivot) {
                skippedColumn = true
            }
            val column = if (skippedColumn) it + 1 else it
            val term = this[rowPivot, column] * determinant(rowPivot + 1, column)
            if (negateTerm) {
                result -/* = */ term
            } else {
                result +/* = */ term
            }
            negateTerm = !negateTerm
        }
        return result
    }

    /**
     * Returns the trace of this matrix.
     *
     * The trace is the sum of all entries on the main diagonal.
     * @throws CompositeUndefinedException this matrix is not square
     */
    public fun trace(): Rational {
        "Trace".requiresSquareMatrix()
        val result = ZERO.mutable()
        repeat(rows) { result +/* = */ this[it, it] }
        return result.immutable()
    }

    /**
     * Returns the rank of this matrix.
     *
     * The rank, informally, is the number of zero rows when a matrix is in [row echelon form][ref].
     */
    public fun rank(): Int {
        val ref = lazyRowEchelonForm ?: ref()
        var zeroRows = 0
        ref.forEachRow {
            forEachInRow { entry ->
                if (entry != ZERO) {
                    --zeroRows
                    return@forEachInRow
                }
            }
            ++zeroRows
        }
        return ref.rows - zeroRows
    }

    /**
     * Returns the minor, M, at the given entry.
     *
     * Since this implementation of the minor is by entry, the matrix must be square.
     * @throws CompositeUndefinedException this matrix is not square
     */
    @Suppress("MemberVisibilityCanBePrivate")
    public fun minor(rowIndex: Int, columnIndex: Int): Matrix {
        "Minor".requiresSquareMatrix()
        return getMinor(rowIndex, columnIndex)
    }

    /**
     * Returns the cofactor, C, at the given entry.
     *
     * For the specific properties that are required in order to return a cofactor, see [minor].
     * @throws CompositeUndefinedException this matrix is not square
     */
    public fun cofactor(rowNumber: Int, columnNumber: Int): Matrix {
        "Cofactor".requiresSquareMatrix()
        val minor = getMinor(rowNumber, columnNumber)
        return if ((rowNumber % 2 == 0) xor (columnNumber % 2 == 0)) -minor else minor
    }

    /**
     * Assumes this is a square matrix.
     */
    private fun getMinor(rowNumber: Int, columnNumber: Int): Matrix {
        val sideLength = rows - 1
        val table = MutableTable<Rational>(sideLength, sideLength)
        var minorRow = 0
        var minorColumn = 0
        forEachRow {
            forEachInRow { entry ->
                if (rowIndex != rowNumber && columnIndex != columnNumber) {
                    table[minorRow, minorColumn] = entry
                    if (minorColumn == sideLength - 1) {
                        minorColumn = 0
                        ++minorRow
                    } else {
                        ++minorColumn
                    }
                }
            }
        }
        return Matrix(table.backingArray)
    }

    /**
     * Returns the result of this matrix when multiplied by -1.
     */
    public operator fun unaryMinus(): Matrix = this * Rational.NEGATIVE_ONE

    /**
     * Returns the result of this matrix added to the other.
     *
     * The addition is done by-entry, with the result having the same dimensions as the arguments.
     * @throws CompositeOverflowException the two matrices are of different sizes
     */
    public operator fun plus(other: Matrix): Matrix = add(other, Rational::plus)

    /**
     * Returns the result of this matrix subtracted by the other.
     *
     * The subtraction is done by-entry, with the result having the same dimensions as the arguments.
     * @throws CompositeUndefinedException the two matrices are of different sizes
     * @throws CompositeOverflowException the result overflows
     */
    public operator fun minus(other: Matrix): Matrix = add(other, Rational::minus)

    private inline fun add(other: Matrix, operator: (Rational, Rational) -> Rational): Matrix {
        if (rows != other.rows || columns != other.columns) {
            raiseUndefined("Addition is undefined for matrices of different dimensions")
        }
        val table = Table(rows, columns) { rowIndex, columnIndex ->
            operator(this[rowIndex, columnIndex], other[rowIndex, columnIndex])
        }
        return Matrix(table.backingArray)
    }

    /**
     * Returns the result of this matrix multiplied by the other.
     *
     * The multiplication is done as the dot product of rows of this matrix by the columns of the other.
     * In other words, each entry becomes the sum of it multiplied by
     * each entry in each column of the other matrix, per column.
     * This is done for each row in this matrix.
     * @throws CompositeUndefinedException the total columns in this matrix is not equal to the total rows of the other
     */
    public operator fun times(other: Matrix): Matrix {
        if (columns != other.rows) {
            raiseUndefined(
                "Multiplication is undefined when the # of columns of the left argument " +
                "is not equal to the # of rows of the right argument"
            )
        }
        val table = MutableTable<Rational>(rows, other.columns)
        val sum = MutableRational(ZERO)
        forEachRow self@ {
            other.forEachColumn other@ {
                forEachInRow entry@ { entry ->
                    sum +/* = */ (entry * other[this@other.columnIndex, this@entry.columnIndex])
                }
                table[this@self.rowIndex, columnIndex] = sum.immutable()
                sum/* = */.valueOf(ZERO)
            }
        }
        return Matrix(table.backingArray)
    }

    /**
     * Returns the result of this matrix multiplied by a scalar.
     *
     * The multiplication is done by-entry, with each entry being multiplied by the scalar value.
     */
    public operator fun times(scalar: Rational): Matrix {
        val table = Table(rows, columns) { rowIndex, columnIndex -> this[rowIndex, columnIndex] * scalar }
        return Matrix(table.backingArray)
    }

    // ------------------------------ eigenvalues ------------------------------

    // TODO implement eigenvalue, eigenvector functions

    // ------------------------------ miscellaneous --------------------

    private fun String.requiresSquareMatrix() {
        if (rows != columns) {
            raiseUndefined("$this is only defined for square matrices")
        }
    }

    public companion object {
        /**
         * The 2x2 identity matrix.
         *
         * All entries are zero except for those on the main diagonal, which are all one.
         */
        @JvmStatic public val I2: Matrix = identity(2)

        /**
         * The 3x3 identity matrix.
         *
         * All entries are zero except for those on the main diagonal, which are all one.
         */
        @JvmStatic public val I3: Matrix = identity(3)

        /**
         * Returns the identity matrix with the given number of rows and columns.
         * @see I2
         * @see I3
         */
        @JvmStatic
        public fun identity(sideLength: Int): Matrix {
            val table = MutableTable(sideLength, sideLength, defaultEntry = ZERO)
            repeat(sideLength) { table[it, it] = ONE }
            return Matrix(table.backingArray)
        }

        /**
         * Returns a new [matrix prototype][Template] with the given dimensions.
         */
        @JvmName("withSize")
        public operator fun get(rowCount: Int, columnCount: Int): Template = Template(rowCount, columnCount)
    }
}