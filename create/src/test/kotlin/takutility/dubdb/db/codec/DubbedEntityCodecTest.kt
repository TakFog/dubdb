package takutility.dubdb.db.codec

import org.bson.BsonWriter
import org.bson.codecs.DecoderContext
import org.bson.codecs.EncoderContext
import org.bson.codecs.configuration.CodecRegistries
import org.bson.json.JsonReader
import org.bson.json.JsonWriter
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import takutility.dubdb.entities.*
import takutility.dubdb.entities.Source.*
import java.io.StringWriter

internal class DubbedEntityCodecTest {
    lateinit var codec: DubbedEntityCodec
    lateinit var jsonWriter: StringWriter
    lateinit var w: BsonWriter

    @BeforeEach
    fun setup() {
        codec = DubbedEntityCodec(
            CodecRegistries.fromRegistries(
                codecRegistry,
                CodecRegistries.fromCodecs(
                    MovieRefCodec(codecRegistry),
                    ActorRefCodec(codecRegistry),
                    DubberRefCodec(codecRegistry)
                )
            )
        )
        jsonWriter = StringWriter()
        w = JsonWriter(jsonWriter)
    }

    @Test
    fun encode() {
        val entity = DubbedEntity(
            name = "test name",
            movie = movieRefOf("movie name",
                type = MovieType.MOVIE,
                ids = SourceIds.of(WIKI to "Movie_Title"),
            ),
            dubber = Dubber("dubber name", ids = SourceIds.of(WIKI to "Dubber_Name", DUBDB to "85786d0cd431d8a82be616e6")),
            actor = Actor("actor name", ids = SourceIds.of(WIKI to "Actor_Name")),
            ids = SourceIds.of(
                MONDO_DOPPIATORI to "voci/testname",
                WIKI to "Wiki_Name",
                DUBDB to "75c337a9aca64000c637ba10",
            ),
            parsed = true,
            sources = mutableListOf(
                RawData(SourceId(WIKI, "Wiki_Name"), DataSource.DUBBER, "test raw data")
            )
        )

        codec.encode(w, entity, EncoderContext.builder().build())
        assertEquals("""{"_id": {"${'$'}oid": "75c337a9aca64000c637ba10"}, "name": "test name","""
                    +""" "ids": {"MONDO_DOPPIATORI": "voci/testname", "WIKI": "Wiki_Name"}, "parsed": true,"""
                    +""" "sources": [{"source": "WIKI", "sourceId": "Wiki_Name", "dataSource": "DUBBER", "raw": "test raw data"}],"""
                    +""" "movie": {"name": "movie name", "ids": {"WIKI": "Movie_Title"}, "type": "MOVIE"},"""
                    +""" "dubber": {"name": "dubber name", "ids": {"WIKI": "Dubber_Name", "DUBDB": "85786d0cd431d8a82be616e6"}},"""
                    +""" "actor": {"name": "actor name", "ids": {"WIKI": "Actor_Name"}}}""",
            jsonWriter.toString()
        )
    }

    @Test
    fun encodeMinimal() {
        val dubber = DubbedEntity(name = "test name", movieRefOf())
        codec.encode(w, dubber, EncoderContext.builder().build())

        assertEquals("""{"name": "test name", "parsed": false, "movie": {}}""",
            jsonWriter.toString()
        )
    }


    @Test
    fun decode() {
        val decoded = codec.decode(
            JsonReader("""{"_id": {"${'$'}oid": "75c337a9aca64000c637ba10"}, "name": "test name","""
                    +""" "ids": {"MONDO_DOPPIATORI": "voci/testname", "WIKI": "Wiki_Name"}, "parsed": true,"""
                    +""" "sources": [{"source": "WIKI", "sourceId": "Wiki_Name", "dataSource": "DUBBER", "raw": "test raw data"}],"""
                    +""" "movie": {"name": "movie name", "ids": {"WIKI": "Movie_Title"}, "type": "MOVIE"},"""
                    +""" "dubber": {"name": "dubber name", "ids": {"WIKI": "Dubber_Name", "DUBDB": "85786d0cd431d8a82be616e6"}},"""
                    +""" "actor": {"name": "actor name", "ids": {"WIKI": "Actor_Name"}}}"""),
            DecoderContext.builder().build())

        assertEquals("test name", decoded.name)
        assertEquals(SourceIds.of(
            MONDO_DOPPIATORI to "voci/testname",
            WIKI to "Wiki_Name",
            DUBDB to "75c337a9aca64000c637ba10"
        ), decoded.ids)
        assertEquals(true, decoded.parsed)
        assertEquals(1, decoded.sources.size)
        decoded.sources[0].apply {
            assertEquals(SourceId(WIKI, "Wiki_Name"), sourceId)
            assertEquals(DataSource.DUBBER, dataSource)
            assertEquals("test raw data", raw)
        }
        decoded.movie.apply {
            assertEquals("movie name", name)
            assertEquals("Movie_Title", wikiId)
            assertEquals(1, ids.size)
        }
        assertNull(decoded.chara)
        decoded.dubber!!.apply {
            assertEquals("dubber name", name)
            assertEquals("Dubber_Name", wikiId)
            assertEquals("85786d0cd431d8a82be616e6", id)
            assertEquals(2, ids.size)
        }
        decoded.actor!!.apply {
            assertEquals("actor name", name)
            assertEquals("Actor_Name", wikiId)
            assertEquals(1, ids.size)
        }
    }

    @Test
    fun decodeMinimal() {
        val decoded = codec.decode(
            JsonReader("""{"name": "test name", "parsed": false, "movie": {}}"""),
            DecoderContext.builder().build())

        assertEquals("test name", decoded.name)
        assertTrue(decoded.ids.isEmpty())
        assertEquals(false, decoded.parsed)
        assertTrue(decoded.sources.isEmpty())
        decoded.movie.apply {
            assertNull(name)
            assertNull(type)
            assertTrue(ids.isEmpty())
        }
        assertNull(decoded.chara)
        assertNull(decoded.dubber)
        assertNull(decoded.actor)
    }
}