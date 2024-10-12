package takutility.dubdb.db.codec

import com.mongodb.MongoClientSettings
import org.bson.codecs.configuration.CodecRegistries
import org.bson.codecs.configuration.CodecRegistry

val dubDbCodecs: List<DubDbCodec<*>> = listOf(
    EntityRefCodec(),
    ActorRefCodec(),
    DubberRefCodec(),
    MovieRefCodec(),
    ActorCodec(),
    DubberCodec(),
    MovieCodec(),
    DubbedEntityCodec(),
)

val codecRegistry = buildRegistry()

private fun buildRegistry(): CodecRegistry {
    val registry = CodecRegistries.fromRegistries(
        CodecRegistries.fromCodecs(
            SourceIdsCodec.getInstance(false),
            RawDataCodec(),
        ),
        CodecRegistries.fromCodecs(*dubDbCodecs.toTypedArray()),
        MongoClientSettings.getDefaultCodecRegistry()
    )

    dubDbCodecs.forEach { it.init(registry) }

    return registry
}