package takutility.dubdb.db.codec

import org.bson.BsonWriter
import org.bson.codecs.DecoderContext
import org.bson.codecs.EncoderContext
import org.bson.json.JsonReader
import org.bson.json.JsonWriter
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import takutility.dubdb.entities.*
import java.io.StringWriter

internal class ActorCodecTest {
    lateinit var codec: ActorCodec
    lateinit var jsonWriter: StringWriter
    lateinit var w: BsonWriter

    @BeforeEach
    fun setup() {
        codec = ActorCodec(codecRegistry)
        jsonWriter = StringWriter()
        w = JsonWriter(jsonWriter)
    }

    @Test
    fun encode() {
        val actor = Actor(
            name = "test name",
            ids = SourceIds.of(
                Source.TRAKT to "123456",
                Source.WIKI to "Wiki_Name",
            ),
            parsed = true,
            sources = mutableListOf(
                RawData(SourceId(Source.WIKI, "MovieName"), DataSource.MOVIE_ORIG, "test raw data")
            )
        )
        codec.encode(w, actor, EncoderContext.builder().build())

        Assertions.assertEquals(
            """{"name": "test name", "ids": {"TRAKT": "123456", "WIKI": "Wiki_Name"}, "parsed": true,"""
                +""" "sources": [{"source": "WIKI", "sourceId": "MovieName", "dataSource": "MOVIE_ORIG","""
                +""" "raw": "test raw data"}]}""",
            jsonWriter.toString()
        )
    }

    @Test
    fun encodeMinimal() {
        val actor = Actor(name = "test name")
        codec.encode(w, actor, EncoderContext.builder().build())

        Assertions.assertEquals(
            """{"name": "test name", "parsed": false}""",
            jsonWriter.toString()
        )
    }

    @Test
    fun encodeWithId() {
        val actor = Actor(
            name = "test name",
            ids = SourceIds.of(Source.DUBDB to "85786d0cd431d8a82be616e6", Source.WIKI to "Wiki_Name")
        )
        codec.encode(w, actor, EncoderContext.builder().build())

        Assertions.assertEquals(
            """{"_id": {"${'$'}oid": "85786d0cd431d8a82be616e6"}, "name": "test name", "ids": {"WIKI": "Wiki_Name"}, "parsed": false}""",
            jsonWriter.toString()
        )
    }

    @Test
    fun decode() {
        val decoded = codec.decode(
            JsonReader("""{"name": "test name", "ids": {"TRAKT": "123456", "WIKI": "Wiki_Name"}, "parsed": true, 
                "sources": [{"source": "WIKI", "sourceId": "MovieName", "dataSource": "MOVIE_ORIG",
                "raw": "test raw data"}]}"""),
            DecoderContext.builder().build())

        assertEquals("test name", decoded.name)
        assertEquals(SourceIds.of(Source.TRAKT to "123456", Source.WIKI to "Wiki_Name"), decoded.ids)
        assertEquals(true, decoded.parsed)
        assertEquals(1, decoded.sources.size)
        val source = decoded.sources[0]
        assertEquals(SourceId(Source.WIKI, "MovieName"), source.sourceId)
        assertEquals(DataSource.MOVIE_ORIG, source.dataSource)
        assertEquals("test raw data", source.raw)
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

    }

    @Test
    fun decodeWithId() {
        val decoded = codec.decode(
            JsonReader("""{"_id": {"${'$'}oid": "85786d0cd431d8a82be616e6"}, "name": "test name", "ids": {"WIKI": "Wiki_Name"} "parsed": false}"""),
            DecoderContext.builder().build())

        assertEquals("test name", decoded.name)
        assertEquals(SourceIds.of(Source.DUBDB to "85786d0cd431d8a82be616e6", Source.WIKI to "Wiki_Name"), decoded.ids)
        assertEquals(false, decoded.parsed)
        assertTrue(decoded.sources.isEmpty())

    }
}