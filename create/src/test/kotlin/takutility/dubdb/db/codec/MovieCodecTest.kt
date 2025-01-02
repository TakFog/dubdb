package takutility.dubdb.db.codec

import org.bson.BsonWriter
import org.bson.codecs.DecoderContext
import org.bson.codecs.EncoderContext
import org.bson.json.JsonReader
import org.bson.json.JsonWriter
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import takutility.dubdb.entities.*
import java.io.StringWriter
import java.time.Instant

internal class MovieCodecTest {
    lateinit var codec: MovieCodec
    lateinit var jsonWriter: StringWriter
    lateinit var w: BsonWriter

    @BeforeEach
    fun setup() {
        codec = init(MovieCodec())
        jsonWriter = StringWriter()
        w = JsonWriter(jsonWriter)
    }

    @Test
    fun encode() {
        val parseTs = Instant.parse("2025-01-02T15:48:30.763Z")
        val movie = Movie(
            name = "test name",
            ids = SourceIds.of(
                Source.TRAKT to "123456",
                Source.WIKI to "Wiki_Name",
            ),
            type = MovieType.MOVIE,
            year = 2019,
            parseTs = parseTs,
            sources = mutableListOf(
                RawData(SourceId(Source.TRAKT, "123456"), DataSource.TRAKT, "test raw data")
            )
        )
        codec.encode(w, movie, EncoderContext.builder().build())

        assertEquals(
            """{"name": "test name", "ids": {"TRAKT": "123456", "WIKI": "Wiki_Name"}, "parseTs": ${bdate(parseTs)},"""
                +""" "sources": [{"source": "TRAKT", "sourceId": "123456", "dataSource": "TRAKT","""
                +""" "raw": "test raw data"}], "type": "MOVIE", "year": 2019}""",
            jsonWriter.toString()
        )
    }

    @Test
    fun encodeMinimal() {
        val movie = Movie(name = "test name")
        codec.encode(w, movie, EncoderContext.builder().build())

        Assertions.assertEquals(
            """{"name": "test name", "type": null}""",
            jsonWriter.toString()
        )
    }

    @Test
    fun encodeMinimalWithYear() {
        val movie = Movie(name = "test name", year = 2019)
        codec.encode(w, movie, EncoderContext.builder().build())

        Assertions.assertEquals(
            """{"name": "test name", "type": null, "year": 2019}""",
            jsonWriter.toString()
        )
    }

    @Test
    fun encodeMinimalWithType() {
        val movie = Movie(name = "test name", type = MovieType.SERIES)
        codec.encode(w, movie, EncoderContext.builder().build())

        Assertions.assertEquals(
            """{"name": "test name", "type": "SERIES"}""",
            jsonWriter.toString()
        )
    }

    @Test
    fun decode() {
        val parseTs = Instant.parse("2025-01-02T15:48:30.763Z")
        val decoded = codec.decode(
            JsonReader("""{"name": "test name", "ids": {"TRAKT": "123456", "WIKI": "Wiki_Name"}, "parseTs": ${bdate(parseTs)},
                "sources": [{"source": "TRAKT", "sourceId": "123456", "dataSource": "TRAKT",
                "raw": "test raw data"}], "type": "MOVIE", "year": 2019}"""),
            DecoderContext.builder().build())

        assertEquals("test name", decoded.name)
        assertEquals(SourceIds.of(Source.TRAKT to "123456", Source.WIKI to "Wiki_Name"), decoded.ids)
        assertEquals(true, decoded.parsed)
        assertEquals(parseTs, decoded.parseTs)
        assertEquals(1, decoded.sources.size)
        val source = decoded.sources[0]
        assertEquals(SourceId(Source.TRAKT, "123456"), source.sourceId)
        assertEquals(DataSource.TRAKT, source.dataSource)
        assertEquals("test raw data", source.raw)
        assertEquals(MovieType.MOVIE, decoded.type)
        assertEquals(2019, decoded.year)
    }

    @Test
    fun decodeMinimal() {
        val decoded = codec.decode(
            JsonReader("""{"name": "test name", "parsed": false}"""),
            DecoderContext.builder().build())

        assertEquals("test name", decoded.name)
        assertTrue(decoded.ids.isEmpty())
        assertEquals(false, decoded.parsed)
        assertTrue(decoded.sources.isEmpty())
        assertNull(decoded.type)
        assertNull(decoded.year)
    }
}