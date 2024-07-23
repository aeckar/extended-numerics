package io.github.aeckar.numerics.serializers

import io.github.aeckar.numerics.Int128
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.*

/**
 * *kotlinx.serialization* serializer for 128-bit integers.
 */
public object Int128Serializer : KSerializer<Int128> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Int128") {
        element<Int>("q1")
        element<Int>("q2")
        element<Int>("q3")
        element<Int>("q4")
    }

    override fun deserialize(decoder: Decoder): Int128 = decoder.decodeStructure(descriptor) {
        var q1 = 0
        var q2 = 0
        var q3 = 0
        var q4 = 0
        while (true) {
            when (val index = decodeElementIndex(descriptor)) {
                0 -> q1 = decodeIntElement(descriptor, 0)
                1 -> q2 = decodeIntElement(descriptor, 1)
                2 -> q3 = decodeIntElement(descriptor, 2)
                3 -> q4 = decodeIntElement(descriptor, 3)
                CompositeDecoder.DECODE_DONE -> break
                else -> error("Unexpected index: $index")
            }
        }
        Int128(q1, q2, q3, q4)
    }

    override fun serialize(encoder: Encoder, value: Int128) {
        encoder.encodeStructure(descriptor) {
            encodeIntElement(descriptor, 0, value.q1)
            encodeIntElement(descriptor, 1, value.q2)
            encodeIntElement(descriptor, 2, value.q3)
            encodeIntElement(descriptor, 3, value.q4)
        }
    }
}