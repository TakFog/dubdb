package takutility.dubdb.db.codec

import org.bson.BsonWriter
import org.bson.codecs.DecoderContext
import org.bson.codecs.EncoderContext
import org.bson.json.JsonReader
import org.bson.json.JsonWriter
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import takutility.dubdb.entities.MovieType
import takutility.dubdb.entities.Source
import takutility.dubdb.entities.SourceIds
import takutility.dubdb.entities.movieRefOf
import java.io.StringWriter

internal class MovieRefCodecTest {
    lateinit var codec: MovieRefCodec
    lateinit var jsonWriter: StringWriter
    lateinit var w: BsonWriter

    @BeforeEach
    fun setup() {
        codec = init(MovieRefCodec())
        jsonWriter = StringWriter()
        w = JsonWriter(jsonWriter)
    }

    @Test
    fun encode() {
        val ref = movieRefOf(
            "test name",
            type = MovieType.SERIES,
            SourceIds.of(
                Source.TRAKT to "123456",
                Source.WIKI to "Wiki_Name",
            ),
        )
        codec.encode(w, ref, EncoderContext.builder().build())

        assertEquals(
            """{"name": "test name", "ids": {"TRAKT": "123456", "WIKI": "Wiki_Name"}, "type": "SERIES"}""",
            jsonWriter.toString()
        )
    }

    @Test
    fun encodeInternalId() {
        val ref = movieRefOf(
            "test name",
            type = null,
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
        val ref = movieRefOf("test name", type = MovieType.MOVIE)
        codec.encode(w, ref, EncoderContext.builder().build())

        assertEquals(
            """{"name": "test name", "type": "MOVIE"}""",
            jsonWriter.toString()
        )
    }

    @Test
    fun encodeNoName() {
        val ref = movieRefOf(
            name = null,
            type = MovieType.MOVIE,
            ids = SourceIds.of(
                Source.TRAKT to "123456",
                Source.WIKI to "Wiki_Name",
            ),
        )
        codec.encode(w, ref, EncoderContext.builder().build())

        assertEquals(
            """{"ids": {"TRAKT": "123456", "WIKI": "Wiki_Name"}, "type": "MOVIE"}""",
            jsonWriter.toString()
        )
    }

    @Test
    fun encodeNoType() {
        val ref = movieRefOf(
            "test name",
            type = null,
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

    @Test
    fun decode() {
        val decoded = codec.decode(
            JsonReader("""{"name": "test name", "ids": {"TRAKT": "123456", "WIKI": "Wiki_Name"}, "type": "SERIES"}"""),
            DecoderContext.builder().build())

        assertEquals("test name", decoded.name)
        assertEquals(SourceIds.of(Source.TRAKT to "123456", Source.WIKI to "Wiki_Name"), decoded.ids)
        assertEquals(MovieType.SERIES, decoded.type)
    }

    @Test
    fun decodeNoIds() {
        val decoded = codec.decode(
            JsonReader("""{"name": "test name", "type": "MOVIE"}"""),
            DecoderContext.builder().build())

        assertEquals("test name", decoded.name)
        assertTrue(decoded.ids.isEmpty())
        assertEquals(MovieType.MOVIE, decoded.type)
    }

    @Test
    fun decodeNoName() {
        val decoded = codec.decode(
            JsonReader("""{"ids": {"TRAKT": "123456", "WIKI": "Wiki_Name"}, "type": "MOVIE"}"""),
            DecoderContext.builder().build())

        assertNull(decoded.name)
        assertEquals(SourceIds.of(Source.TRAKT to "123456", Source.WIKI to "Wiki_Name"), decoded.ids)
        assertEquals(MovieType.MOVIE, decoded.type)
    }

    @Test
    fun decodeNoType() {
        val decoded = codec.decode(
            JsonReader("""{"name": "test name", "ids": {"TRAKT": "123456", "WIKI": "Wiki_Name"}}"""),
            DecoderContext.builder().build())

        assertEquals("test name", decoded.name)
        assertEquals(SourceIds.of(Source.TRAKT to "123456", Source.WIKI to "Wiki_Name"), decoded.ids)
        assertNull(decoded.type)
    }

}