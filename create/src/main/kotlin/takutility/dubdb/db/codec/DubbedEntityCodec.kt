package takutility.dubdb.db.codec

import org.bson.BsonReader
import org.bson.BsonWriter
import org.bson.codecs.Codec
import org.bson.codecs.DecoderContext
import org.bson.codecs.EncoderContext
import org.bson.codecs.configuration.CodecRegistry
import takutility.dubdb.entities.*

private val emptyMovie = movieRefOf()

private const val MOVIE = "movie"
private const val CHARA = "chara"
private const val DUBBER = "dubber"
private const val ACTOR = "actor"


class DubbedEntityCodec : EntityCodec<DubbedEntity>() {
    private lateinit var movieRefCodec: Codec<MovieRef>
    private lateinit var dubberRefCodec: Codec<DubberRef>
    private lateinit var actorRefCodec: Codec<ActorRef>

    override fun init(registry: CodecRegistry) {
        super.init(registry)
        movieRefCodec = registry[MovieRef::class.java]
        dubberRefCodec = registry[DubberRef::class.java]
        actorRefCodec = registry[ActorRef::class.java]
    }

    override fun encodeObject(w: BsonWriter, entity: DubbedEntity, ctx: EncoderContext?) {
        super.encodeObject(w, entity, ctx)
        w.writeName(MOVIE)
        movieRefCodec.encode(w, entity.movie, ctx)
//        if (entity.chara != null) {
//            TODO("unsupported chara encode")
//        }
        dubberRefCodec.encodeNullableField(w, DUBBER, entity.dubber, ctx)
        actorRefCodec.encodeNullableField(w, ACTOR, entity.actor, ctx)
    }

    override fun decodeField(key: String, r: BsonReader, ctx: DecoderContext?, inst: DubbedEntity) {
        when(key) {
            MOVIE -> inst.movie = movieRefCodec.decode(r, ctx)
            CHARA -> TODO("unsupported chara decode")
            DUBBER -> inst.dubber = dubberRefCodec.decode(r, ctx)
            ACTOR -> inst.actor = actorRefCodec.decode(r, ctx)
            else -> super.decodeField(key, r, ctx, inst)
        }
    }

    override fun newInstance(): DubbedEntity = DubbedEntity("", emptyMovie)

    override fun getEncoderClass(): Class<DubbedEntity> = DubbedEntity::class.java
}