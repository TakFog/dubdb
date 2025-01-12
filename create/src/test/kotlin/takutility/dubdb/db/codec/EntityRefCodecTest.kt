package takutility.dubdb.db.codec

import org.bson.BsonWriter
import org.bson.codecs.DecoderContext
import org.bson.codecs.EncoderContext
import org.bson.json.JsonReader
import org.bson.json.JsonWriter
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import takutility.dubdb.entities.EntityRefImpl
import takutility.dubdb.entities.Source
import takutility.dubdb.entities.SourceIds
import java.io.StringWriter

internal class EntityRefCodecTest {
    lateinit var codec: EntityRefCodec
    lateinit var jsonWriter: StringWriter
    lateinit var w: BsonWriter

    @BeforeEach
    fun setup() {
        codec = init(EntityRefCodec())
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

        assertEquals(
            """{"name": "test name", "ids": {"TRAKT": "123456", "WIKI": "Wiki_Name"}}""",
            jsonWriter.toString()
        )
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun encodeParsed(parsedVal: Boolean) {
        val ref = EntityRefImpl(
            "test name",
            SourceIds.of(
                Source.TRAKT to "123456",
                Source.WIKI to "Wiki_Name",
            ),
            parsed = parsedVal
        )
        codec.encode(w, ref, EncoderContext.builder().build())

        assertEquals(
            """{"name": "test name", "ids": {"TRAKT": "123456", "WIKI": "Wiki_Name"}, "parsed": $parsedVal}""",
            jsonWriter.toString()
        )
    }

    @Test
    fun encodeInternalId() {
        val ref = EntityRefImpl(
            "test name",
            SourceIds.of(
                Source.DUBDB to "85786d0cd431d8a82be616e6",
            ),
        )
        codec.encode(w, ref, EncoderContext.builder().build())

        assertEquals(
            """{"name": "test name", "ids": {"DUBDB": "85786d0cd431d8a82be616e6"}}""",
            jsonWriter.toString()
        )
    }

    @Test
    fun encodeNoIds() {
        val ref = EntityRefImpl("test name")
        codec.encode(w, ref, EncoderContext.builder().build())

        assertEquals(
            """{"name": "test name"}""",
            jsonWriter.toString()
        )
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

        assertEquals(
            """{"ids": {"TRAKT": "123456", "WIKI": "Wiki_Name"}}""",
            jsonWriter.toString()
        )
    }

    @Test
    fun decode() {
        val decoded = codec.decode(
            JsonReader("""{"name": "test name", "ids": {"TRAKT": "123456", "WIKI": "Wiki_Name"}}"""),
            DecoderContext.builder().build())

        assertEquals("test name", decoded.name)
        assertEquals(SourceIds.of(Source.TRAKT to "123456", Source.WIKI to "Wiki_Name"), decoded.ids)
        assertNull(decoded.parsed)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun decodeParsed(parsedVal: Boolean) {
        val decoded = codec.decode(
            JsonReader("""{"name": "test name", "ids": {"TRAKT": "123456", "WIKI": "Wiki_Name"}, "parsed": $parsedVal}"""),
            DecoderContext.builder().build())

        assertEquals("test name", decoded.name)
        assertEquals(SourceIds.of(Source.TRAKT to "123456", Source.WIKI to "Wiki_Name"), decoded.ids)
        assertEquals(parsedVal, decoded.parsed)
    }

    @Test
    fun decodeNoIds() {
        val decoded = codec.decode(
            JsonReader("""{"name": "test name"}"""),
            DecoderContext.builder().build())

        assertEquals("test name", decoded.name)
        assertTrue(decoded.ids.isEmpty())
    }

    @Test
    fun decodeNoName() {
        val decoded = codec.decode(
            JsonReader("""{"ids": {"TRAKT": "123456", "WIKI": "Wiki_Name"}}"""),
            DecoderContext.builder().build())

        assertNull(decoded.name)
        assertEquals(SourceIds.of(Source.TRAKT to "123456", Source.WIKI to "Wiki_Name"), decoded.ids)
    }
}