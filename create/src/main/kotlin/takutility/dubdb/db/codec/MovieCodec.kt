package takutility.dubdb.db.codec

import org.bson.BsonReader
import org.bson.BsonWriter
import org.bson.codecs.Codec
import org.bson.codecs.DecoderContext
import org.bson.codecs.EncoderContext
import org.bson.codecs.configuration.CodecRegistry
import takutility.dubdb.entities.Movie
import takutility.dubdb.entities.MovieType

class MovieCodec(registry: CodecRegistry) : EntityCodec<Movie>(registry) {
    private val typeCodec: Codec<MovieType>

    init {
        typeCodec = registry[MovieType::class.java]
    }

    override fun encodeObject(w: BsonWriter, entity: Movie, ctx: EncoderContext?) {
        super.encodeObject(w, entity, ctx)
        w.writeName("type")
        if (entity.type != null)
            typeCodec.encode(w, entity.type, ctx)
        else
            w.writeNull()

        if (entity.year != null)
            w.writeInt32("year", entity.year!!)
    }

    override fun decodeField(key: String, r: BsonReader, ctx: DecoderContext?, inst: Movie) {
        when(key) {
            "type" -> inst.type = typeCodec.decode(r, ctx)
            "year" -> inst.year = r.readInt32()
        }
        super.decodeField(key, r, ctx, inst)
    }

    override fun newInstance(): Movie = Movie("")

    override fun getEncoderClass(): Class<Movie> = Movie::class.java
}