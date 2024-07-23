package io.github.aeckar.numerics

import kotlin.test.Test
import kotlin.test.assertEquals

class MatrixTest {
    class Transformation {
        // TODO
    }

    class RationalComponentReturningOperations {
        @Test
        fun determinant() {
            val matrixA = Matrix[3,3](
                1, 5,  7,
                3, 1,  0,
                8, 9, 10
            )
            assertEquals(Rational(-7), matrixA.determinant())
        }
    }

    class MatrixReturningOperations {
        // TODO
    }
}