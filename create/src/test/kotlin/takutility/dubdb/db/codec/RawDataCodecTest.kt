package takutility.dubdb.db.codec

import org.bson.BsonWriter
import org.bson.codecs.DecoderContext
import org.bson.codecs.EncoderContext
import org.bson.json.JsonReader
import org.bson.json.JsonWriter
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import takutility.dubdb.entities.DataSource
import takutility.dubdb.entities.RawData
import takutility.dubdb.entities.Source
import takutility.dubdb.entities.SourceId
import java.io.StringWriter

class RawDataCodecTest {
    lateinit var codec: RawDataCodec
    lateinit var jsonWriter: StringWriter
    lateinit var w: BsonWriter

    @BeforeEach
    fun setup() {
        codec = RawDataCodec()
        jsonWriter = StringWriter()
        w = JsonWriter(jsonWriter)
    }

    @Test
    fun encode() {
        val ref = RawData(
            sourceId = SourceId(Source.TRAKT, "123456"),
            dataSource = DataSource.MOVIE_DUB,
            raw = "raw data",
        )
        codec.encode(w, ref, EncoderContext.builder().build())

        Assertions.assertEquals("""{"source": "TRAKT", "sourceId": "123456", "dataSource": "MOVIE_DUB", "raw": "raw data"}""", jsonWriter.toString())
    }

    @Test
    fun decode() {
        val decoded = codec.decode(
            JsonReader("""{"source": "TRAKT", "sourceId": "123456", "dataSource": "MOVIE_DUB", "raw": "raw data"}"""),
            DecoderContext.builder().build())

        assertEquals(SourceId(Source.TRAKT, "123456"), decoded.sourceId)
        assertEquals(DataSource.MOVIE_DUB, decoded.dataSource)
        assertEquals("raw data", decoded.raw)
    }

    @Test
    fun decodePartial() {
        val reader =
            JsonReader("""{"source": "TRAKT", "sourceId": "123456", "dataSource": "MOVIE_DUB"}""")

        assertThrows<Exception> { codec.decode(reader, DecoderContext.builder().build()) }

    }
}