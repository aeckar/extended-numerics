package io.github.aeckar.numerics//import io.github.aeckar.serialization.Schema
//import io.github.aeckar.serialization.schema
//import io.github.aeckar.numerics.Int128
//import io.github.aeckar.numerics.Rational
//import io.github.aeckar.numerics.Table
//
///**
// * The dynamic serialization schema for [composite numbers][CompositeNumber] and [tables][Table].
// */
//public val schema: Schema = schema {
//    define<Int128> {
//        read { Int128(int, int, int, int) }
//        static write {
//            q1 to int
//            q2 to int
//            q3 to int
//            q4 to int
//        }
//    }
//
//    define<Rational> {
//        read { Rational(long, long, int, int) }
//        static write {
//            numer to long
//            denom to long
//            scale to int
//            sign to int
//        }
//    }
//
//    define<Table<Any>> {    // Works for matrices too
//        read { Table(reference as Array<Any>) }
//        static write { backingArray to reference }
//    }
//
//    define<Vector> {
//        // TODO
//    }
//}