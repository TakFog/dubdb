package takutility.dubdb.db.codec

import org.bson.BsonReader
import org.bson.BsonWriter
import org.bson.codecs.Codec
import org.bson.codecs.DecoderContext
import org.bson.codecs.EncoderContext
import org.bson.codecs.configuration.CodecRegistry
import takutility.dubdb.entities.*

private const val TYPE_FIELD = "type"
private const val YEAR_FIELD = "year"

class MovieRefCodec : AbstractEntityRefCodec<MovieRef>() {
    private lateinit var typeCodec: Codec<MovieType>
    override fun init(registry: CodecRegistry) {
        super.init(registry)
        typeCodec = registry[MovieType::class.java]
    }

    override fun encodeObject(w: BsonWriter, entity: MovieRef, ctx: EncoderContext?) {
        super.encodeObject(w, entity, ctx)
        if (entity.type != null) {
            w.writeName(TYPE_FIELD)
            typeCodec.encode(w, entity.type, ctx)
        }
    }

    override fun decodeAdditionalFields(fieldName: String, r: BsonReader, ctx: DecoderContext?): Any? {
        if (fieldName == TYPE_FIELD)
            return typeCodec.decode(r, ctx)
        return null
    }

    override fun createEntityRef(name: String?, ids: SourceIds, fields: Map<String, Any>?): MovieRef {
        return movieRefOf(
            name = name,
            ids = ids,
            type = fields?.get(TYPE_FIELD) as? MovieType
        )
    }

    override fun getEncoderClass(): Class<MovieRef> = MovieRef::class.java

}


class MovieCodec : EntityCodec<Movie>() {
    private lateinit var typeCodec: Codec<MovieType>

    override fun init(registry: CodecRegistry) {
        super.init(registry)
        typeCodec = registry[MovieType::class.java]
    }

    override fun encodeObject(w: BsonWriter, entity: Movie, ctx: EncoderContext?) {
        super.encodeObject(w, entity, ctx)
        w.writeName(TYPE_FIELD)
        if (entity.type != null)
            typeCodec.encode(w, entity.type, ctx)
        else
            w.writeNull()

        if (entity.year != null)
            w.writeInt32(YEAR_FIELD, entity.year!!)
    }

    override fun decodeField(key: String, r: BsonReader, ctx: DecoderContext?, inst: Movie) {
        when(key) {
            TYPE_FIELD -> inst.type = typeCodec.decode(r, ctx)
            YEAR_FIELD -> inst.year = r.readInt32()
            else -> super.decodeField(key, r, ctx, inst)
        }
    }

    override fun newInstance(): Movie = Movie("")

    override fun getEncoderClass(): Class<Movie> = Movie::class.java
}