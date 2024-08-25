package takutility.dubdb.db

import org.bson.BsonWriter
import org.bson.codecs.DecoderContext
import org.bson.codecs.EncoderContext
import org.bson.json.JsonReader
import org.bson.json.JsonWriter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import takutility.dubdb.entities.Source
import takutility.dubdb.entities.SourceIds
import java.io.StringWriter

internal class SourceIdsCodecTest {
    lateinit var codec: SourceIdsCodec
    lateinit var jsonWriter: StringWriter
    lateinit var w: BsonWriter

    @BeforeEach
    fun setup() {
        codec = SourceIdsCodec()
        jsonWriter = StringWriter()
        w = JsonWriter(jsonWriter)
    }

    @Test
    fun encode() {
        val ref = SourceIds.of(
                Source.TRAKT to "123456",
                Source.WIKI to "Wiki_Name",
            )
        codec.encode(w, ref, EncoderContext.builder().build())

        assertEquals("""{"TRAKT": "123456", "WIKI": "Wiki_Name"}""", jsonWriter.toString())
    }

    @Test
    fun encodeEmpty() {
        codec.encode(w, SourceIds(), EncoderContext.builder().build())

        assertEquals("""{}""", jsonWriter.toString())
    }

    @Test
    fun decode() {
        val decoded = codec.decode(
            JsonReader("""{"TRAKT": "123456", "WIKI": "Wiki_Name"}"""),
            DecoderContext.builder().build())

        assertEquals(SourceIds.of(Source.TRAKT to "123456", Source.WIKI to "Wiki_Name"), decoded)
    }

    @Test
    fun decodeEmpty() {
        val decoded = codec.decode(
            JsonReader("""{}"""),
            DecoderContext.builder().build())

        assertEquals(SourceIds(), decoded)
    }

}