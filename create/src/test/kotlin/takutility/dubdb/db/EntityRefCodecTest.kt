package takutility.dubdb.db

import com.mongodb.MongoClientSettings
import org.bson.BsonWriter
import org.bson.codecs.DecoderContext
import org.bson.codecs.EncoderContext
import org.bson.codecs.configuration.CodecRegistries
import org.bson.json.JsonReader
import org.bson.json.JsonWriter
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import takutility.dubdb.entities.EntityRefImpl
import takutility.dubdb.entities.Source
import takutility.dubdb.entities.SourceIds
import java.io.StringWriter

private val codecRegistry = CodecRegistries.fromRegistries(
    CodecRegistries.fromCodecs(SourceIdsCodec()),
    MongoClientSettings.getDefaultCodecRegistry()
)

internal class EntityRefCodecTest {
    lateinit var codec: EntityRefCodec
    lateinit var jsonWriter: StringWriter
    lateinit var w: BsonWriter

    @BeforeEach
    fun setup() {
        codec = EntityRefCodec(codecRegistry)
        jsonWriter = StringWriter()
        w = JsonWriter(jsonWriter)
    }

    @Test
    fun encode() {
        val ref = EntityRefImpl(
            "test name",
            SourceIds.of(
                Source.TRAKT to "123456",
                Source.WIKI to "Wiki_Name",
            ),
        )
        codec.encode(w, ref, EncoderContext.builder().build())

        assertEquals("""{"name": "test name", "ids": {"TRAKT": "123456", "WIKI": "Wiki_Name"}}""",
            jsonWriter.toString())
    }

    @Test
    fun encodeNoIds() {
        val ref = EntityRefImpl("test name")
        codec.encode(w, ref, EncoderContext.builder().build())

        assertEquals("""{"name": "test name"}""",
            jsonWriter.toString())
    }

    @Test
    fun encodeNoName() {
        val ref = EntityRefImpl(
            ids = SourceIds.of(
                Source.TRAKT to "123456",
                Source.WIKI to "Wiki_Name",
            ),
        )
        codec.encode(w, ref, EncoderContext.builder().build())

        assertEquals("""{"ids": {"TRAKT": "123456", "WIKI": "Wiki_Name"}}""",
            jsonWriter.toString())
    }

    @Test
    fun decode() {
        val decoded = codec.decode(JsonReader("""{"name": "test name", "ids": {"TRAKT": "123456", "WIKI": "Wiki_Name"}}"""),
            DecoderContext.builder().build())

        assertEquals("test name", decoded.name)
        assertEquals(SourceIds.of(Source.TRAKT to "123456", Source.WIKI to "Wiki_Name"), decoded.ids)
    }

    @Test
    fun decodeNoIds() {
        val decoded = codec.decode(JsonReader("""{"name": "test name"}"""),
            DecoderContext.builder().build())

        assertEquals("test name", decoded.name)
        assertTrue(decoded.ids.isEmpty())
    }

    @Test
    fun decodeNoName() {
        val decoded = codec.decode(JsonReader("""{"ids": {"TRAKT": "123456", "WIKI": "Wiki_Name"}}"""),
            DecoderContext.builder().build())

        assertNull(decoded.name)
        assertEquals(SourceIds.of(Source.TRAKT to "123456", Source.WIKI to "Wiki_Name"), decoded.ids)
    }
}