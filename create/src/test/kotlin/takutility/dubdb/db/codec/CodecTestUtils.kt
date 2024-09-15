package takutility.dubdb.db.codec

import com.mongodb.MongoClientSettings
import org.bson.codecs.configuration.CodecRegistries

const val oid = "\$oid"

val codecRegistry = CodecRegistries.fromRegistries(
    CodecRegistries.fromCodecs(
        SourceIdsCodec.getInstance(false),
        RawDataCodec(),
    ),
    MongoClientSettings.getDefaultCodecRegistry()
)

fun <E: DubDbCodec<*>> init(codec: E, vararg subcodecs: DubDbCodec<*>): E {
    if (subcodecs.isEmpty())
        codec.init(codecRegistry)
    else {
        val registry = CodecRegistries.fromRegistries(
            codecRegistry,
            CodecRegistries.fromCodecs(*subcodecs)
        )
        subcodecs.forEach { it.init(registry) }
        codec.init(registry)
    }
    return codec
}