package pw.binom.gorep

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.jvm.JvmInline

object VersionSerializer : KSerializer<Version> {
    override fun deserialize(decoder: Decoder): Version =
        Version(decoder.decodeString())

    override val descriptor: SerialDescriptor
        get() = String.serializer().descriptor

    override fun serialize(encoder: Encoder, value: Version) {
        encoder.encodeString(value.body)
    }
}

@Serializable(VersionSerializer::class)
@JvmInline
value class Version(val body: String) {
    operator fun compareTo(other: Version): Int {
        val thisItems = body.split('.')
        val otherValue = other.body.split('.')
        val max = maxOf(thisItems.size, otherValue.size)
        repeat(max) { index ->
            val t = thisItems.getOrNull(index)?.toIntOrNull() ?: 0
            val o = otherValue.getOrNull(index)?.toIntOrNull() ?: 0
            if (t > o) {
                return 1
            }
            if (t < o) {
                return -1
            }
        }
        return 0
    }

    override fun toString(): String = body
}