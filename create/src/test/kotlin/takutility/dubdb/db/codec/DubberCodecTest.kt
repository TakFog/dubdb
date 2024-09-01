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

internal class DubberCodecTest {
    lateinit var codec: DubberCodec
    lateinit var jsonWriter: StringWriter
    lateinit var w: BsonWriter

    @BeforeEach
    fun setup() {
        codec = DubberCodec(codecRegistry)
        jsonWriter = StringWriter()
        w = JsonWriter(jsonWriter)
    }

    @Test
    fun encode() {
        val dubber = Dubber(
            name = "test name",
            ids = SourceIds.of(
                Source.MONDO_DOPPIATORI to "voci/testname",
                Source.WIKI to "Wiki_Name",
            ),
            parsed = true,
            sources = mutableListOf(
                RawData(SourceId(Source.WIKI, "Wiki_Name"), DataSource.DUBBER, "test raw data")
            )
        )
        codec.encode(w, dubber, EncoderContext.builder().build())

        Assertions.assertEquals(
            """{"name": "test name", "ids": {"MONDO_DOPPIATORI": "voci/testname", "WIKI": "Wiki_Name"}, "parsed": true,"""
                +""" "sources": [{"source": "WIKI", "sourceId": "Wiki_Name", "dataSource": "DUBBER","""
                +""" "raw": "test raw data"}]}""",
            jsonWriter.toString()
        )
    }

    @Test
    fun encodeMinimal() {
        val dubber = Dubber(name = "test name")
        codec.encode(w, dubber, EncoderContext.builder().build())

        Assertions.assertEquals(
            """{"name": "test name", "parsed": false}""",
            jsonWriter.toString()
        )
    }

    @Test
    fun decode() {
        val decoded = codec.decode(
            JsonReader("""{"name": "test name", "ids": {"MONDO_DOPPIATORI": "voci/testname", "WIKI": "Wiki_Name"}, "parsed": true, 
                "sources": [{"source": "WIKI", "sourceId": "Wiki_Name", "dataSource": "DUBBER",
                "raw": "test raw data"}]}"""),
            DecoderContext.builder().build())

        assertEquals("test name", decoded.name)
        assertEquals(SourceIds.of(Source.MONDO_DOPPIATORI to "voci/testname", Source.WIKI to "Wiki_Name"), decoded.ids)
        assertEquals(true, decoded.parsed)
        assertEquals(1, decoded.sources.size)
        val source = decoded.sources[0]
        assertEquals(SourceId(Source.WIKI, "Wiki_Name"), source.sourceId)
        assertEquals(DataSource.DUBBER, source.dataSource)
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
}