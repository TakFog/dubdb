package takutility.dubdb.db.codec

import com.mongodb.MongoClientSettings
import org.bson.codecs.configuration.CodecRegistries
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

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

private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX")
fun bdate(value: Instant): String = "{\"\$date\": \"${formatter.format(value.atZone(ZoneOffset.UTC))}\"}"