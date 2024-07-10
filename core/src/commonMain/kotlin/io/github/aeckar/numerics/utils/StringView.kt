package io.github.aeckar.numerics.utils

/**
 * A view of a single character in [string].
 *
 * Enables idiomatic parsing of strings containing values.
 * Can be modified so that this view refers to a different character, or none at all.
 */
internal class StringView(val string: String) {
    private var index = 0

    fun index() = index
    fun char() = string[index]
    fun isWithinBounds() = index < string.length
    fun isNotWithinBounds() = index >= string.length

    inline infix fun satisfies(predicate: (Char) -> Boolean) = isWithinBounds() && predicate(string[index])

    fun move(indexAugment: Int) {
        index += indexAugment
    }

    override fun toString() = "\"$string\" (index = $index)"
}