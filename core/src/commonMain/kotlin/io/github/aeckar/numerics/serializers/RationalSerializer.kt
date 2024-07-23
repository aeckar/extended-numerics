package io.github.aeckar.numerics.serializers

import io.github.aeckar.numerics.Rational
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.*

/**
 * *kotlinx.serialization* serializer for rational numbers.
 */
public object RationalSerializer : KSerializer<Rational> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Rational") {
        element<Long>("numer")
        element<Long>("denom")
        element<Int>("scale")
        element<Int>("sign")
    }

    override fun deserialize(decoder: Decoder): Rational = decoder.decodeStructure(descriptor) {
        var numer = 0L
        var denom = 0L
        var scale = 0
        var sign = 0
        while (true) {
            when (val index = decodeElementIndex(descriptor)) {
                0 -> numer = decodeLongElement(descriptor, 0)
                1 -> denom = decodeLongElement(descriptor, 1)
                2 -> scale = decodeIntElement(descriptor, 2)
                3 -> sign = decodeIntElement(descriptor, 3)
                CompositeDecoder.DECODE_DONE -> break
                else -> error("Unexpected index: $index")
            }
        }
        Rational(numer, denom, scale, sign)
    }

    override fun serialize(encoder: Encoder, value: Rational) {
        encoder.encodeStructure(descriptor) {
            encodeLongElement(descriptor, 0, value.numer)
            encodeLongElement(descriptor, 1, value.denom)
            encodeIntElement(descriptor, 2, value.scale)
            encodeIntElement(descriptor, 3, value.sign)
        }
    }
}