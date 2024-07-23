package io.github.aeckar.numerics.utils

import io.github.aeckar.numerics.Int128
import io.github.aeckar.numerics.Rational
import io.github.aeckar.numerics.raiseIncorrectFormat

/**
 * Destructuring of a non-negative value into the closest scaled 64-bit integer to this and its scale.
 */
internal class ScaledLong {
    private val value: Long
    private val scale: Int

    /**
     * Does not preserve the state of mutable values.
     *
     * Some information may be lost during conversion.
     */
    constructor(i128: Int128, scaleAugment: Int = 0) {
        if (i128.compareTo(Long.MIN_VALUE) == 0) {
            this.value = LONG_MIN.value
            this.scale = LONG_MIN.scale
            return
        }
        if (i128.isLong()) {
            val scaledValue = i128.toLong()
            val valueScale = scaleOf(scaledValue)
            this.value = scaledValue / tenPow(valueScale)
            this.scale = valueScale + scaleAugment
            return
        }
        val sign = i128.sign
        var value = /* (maybe) i128 = */ i128.abs()
        var scale = scaleAugment
        while (value > Long.MAX_VALUE) {
            /* (maybe) value = */ value /= Int128.TEN
            ++scale
        }
        this.value = value.toLong() * sign
        this.scale = scale * sign
    }

    constructor(value: Long, scale: Int) {
        this.value = value
        this.scale = scale
    }

    /**
     * The closest value n, for x=n*10^scale, where n <= [Int.MAX_VALUE].
     */
    operator fun component1() = value

    /**
     * See [Rational.scale][io.github.aeckar.numerics.Rational.scale] for details.
     */
    operator fun component2() = scale

    override fun toString() = if (scale < 0) "$value * 10^($scale)" else "$value * 10^$scale"

    companion object {
        internal val LONG_MIN = ScaledLong(92233720368547758, 2)

        private val ZERO = ScaledLong(0, 0)

        /**
         * Assumes that [view] is within bounds and all leading `0`s have been skipped over.
         *
         * Intended for access by `String`-arg constructor of Rational only.
         */
        fun parse(view: StringView, stop: String): ScaledLong = with(view) {
            fun parseLong(start: Int, rightmostDigitOrDot: Int, scale: Int): ScaledLong {
                move(start - index())
                var value = 0L
                val totalChars = (rightmostDigitOrDot - start) + 1
                var parsedChars = 0
                do {
                    if (char() != '.') {
                        value *= 10
                        value += char().digitToInt()
                    }
                    ++parsedChars
                    move(1)
                } while (parsedChars < totalChars)
                return ScaledLong(value, scale)
            }

            val start = index()
            while (satisfies { it != '.' && it !in stop }) {  // Iterate until end of whole part...
                if (char() !in '0'..'9') {              // ...ensuring every whole digit is valid
                    raiseIncorrectFormat("illegal embedded character", view.string)
                }
                move(1)
            }
            var scale = 0
            val dotIndex: Int
            if (satisfies { it == '.' }) {   // If value has a fractional part...
                dotIndex = index()
                do {
                    move(1)
                    if (isWithinBounds()) {
                        if (char() !in '0'..'9') {  // ...ensure every fractional digit is valid...
                            raiseIncorrectFormat("illegal embedded character", view.string)
                        }
                        if (char() != '0') {    // ...and set scale according to rightmost non-zero fractional digit...
                            scale = dotIndex - index()
                        }
                    }
                } while (satisfies { it !in stop }) // ...while the end of the value is not reached
            } else {    // char() is rightmost whole digit
                dotIndex = -1
            }
            val stopIndex: Int = index()
            do {
                move(-1)
            } while (satisfies { it == '0' || it == '.' })
            // char() is rightmost non-zero digit
            if (isNotWithinBounds()) {
                return ZERO
            }
            val trailingZeros = stopIndex - index() - 1
            scale += trailingZeros + (dotIndex in index()..stopIndex).toInt()
            var length = index() - start + (dotIndex != -1).toInt() + 1
            if (length >= LONG_MAX_STRING.length) {
                val startingLength = length
                while (length != LONG_MAX_STRING.length) {  // Scale down to a length that can possibly fit a Long
                    if (char() == '.') {                    // Skip digit to the left
                        move(-2)                            // Always within bounds
                    } else {
                        move(-1)
                    }
                    --length
                }
                var rightmostDigitOrDot = index()
                move(start - index())
                do {    // Ensure possible Long value does not overflow
                    if (char() == '.') {
                        move(1) // Always within bounds
                        if (isNotWithinBounds()) {
                            break
                        }
                    }
                    if (char() > LONG_MAX_STRING[index() - start]) {   // Value overflows, scale down by 1
                        --rightmostDigitOrDot
                        --length
                        break
                    }
                    if (index() == rightmostDigitOrDot) {
                        break
                    }
                    move(1)
                } while (true)
                scale += startingLength - length
                move(rightmostDigitOrDot - index())
            }
            parseLong(start, index(), scale).also {
                move(stopIndex - index())   // Ensure characters are only parsed once
            }
        }
    }
}