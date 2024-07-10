package io.github.aeckar.numerics

/**
 * Indicates that if the caller is mutable, the result of this function will be stored in the same instance.
 *
 * Applies to operations that return only their caller or a value returned
 * by [valueOf][CompositeNumber.valueOf] (or some variant of it) from within the same scope.
 * Any operation that breaks this contract cannot be considered cumulative.
 * Overrides of cumulative functions must abide by the same contract.
 *
 * Mutability of composite numbers is necessary to keep allocations
 * to a minimum when performing intermediate operations.
 *
 * Instead of supplying separate functions for returning a unique value and modifying an existing value,
 * the business logic for both is shared within the same function.
 * To determine which will be returned, this responsibility is delegated to `valueOf`.
 *
 * There is no guarantee that the integrity of the state of a cumulative integer
 * will be maintained if it is passed as an argument to an operation.
 * It is for this reason that users are not allowed to interact with mutable composite numbers at all.
 *
 * Mutable subclasses are not necessary for super-composite numbers like [Complex] because
 * they already benefit from mutable instances of the classes they are composed of.
 *
 * Composite conversion functions (`toComplex`, `toRational`, `toInt128`) do not
 * account for mutability and thus should never be called from mutable instances.
 * @see MutableInt128
 * @see MutableRational
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
internal annotation class Cumulative