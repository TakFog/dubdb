package takutility.dubdb.db.codec

import com.mongodb.MongoClientSettings
import org.bson.codecs.configuration.CodecRegistries

val codecRegistry = CodecRegistries.fromRegistries(
    CodecRegistries.fromCodecs(SourceIdsCodec()),
    CodecRegistries.fromCodecs(RawDataCodec()),
    MongoClientSettings.getDefaultCodecRegistry()
)