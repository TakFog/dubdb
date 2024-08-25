package takutility.dubdb.db

import org.bson.BsonReader
import org.bson.BsonType
import org.bson.BsonWriter
import org.bson.codecs.Codec
import org.bson.codecs.DecoderContext
import org.bson.codecs.EncoderContext
import org.bson.codecs.configuration.CodecRegistry
import takutility.dubdb.entities.EntityRef
import takutility.dubdb.entities.EntityRefImpl
import takutility.dubdb.entities.Source
import takutility.dubdb.entities.SourceIds

class SourceIdsCodec: Codec<SourceIds> {
    override fun encode(writer: BsonWriter?, value: SourceIds?, encoderContext: EncoderContext?) {
        if (value == null) {
            writer!!.writeNull()
            return
        }
        writer!!.writeStartDocument()
        value.forEach { writer.writeString(it.source.name, it.id) }
        writer.writeEndDocument()
    }

    override fun decode(reader: BsonReader?, decoderContext: DecoderContext?): SourceIds {
        val ids = SourceIds()
        reader!!.readStartDocument()
        while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
            ids[Source.valueOf(reader.readName())] = reader.readString()
        }
        reader.readEndDocument()
        return ids
    }

    override fun getEncoderClass(): Class<SourceIds> = SourceIds::class.java

}

class EntityRefCodec(registry: CodecRegistry): Codec<EntityRef> {
    private val idsCodec: Codec<SourceIds>

    init {
        idsCodec = registry[SourceIds::class.java]
    }

    override fun encode(w: BsonWriter?, ref: EntityRef?, ctx: EncoderContext?) {
        if (ref == null) {
            w!!.writeNull()
            return
        }
        w!!.writeStartDocument()
        ref.name?.let { w.writeString("name", it) }
        if (ref.ids.isNotEmpty()) {
            w.writeName("ids")
            idsCodec.encode(w, ref.ids, ctx)
        }
        w.writeEndDocument()
    }

    override fun decode(r: BsonReader?, ctx: DecoderContext?): EntityRef {
        var name: String? = null
        var ids: SourceIds? = null
        r!!.readStartDocument()
        while (r.readBsonType() != BsonType.END_OF_DOCUMENT) {
            when (r.readName()) {
                "name" -> name = r.readString()
                "ids" -> ids = idsCodec.decode(r, ctx)
            }
        }
        r.readEndDocument()

        return EntityRefImpl(name = name, ids = ids ?: SourceIds())
    }

    override fun getEncoderClass(): Class<EntityRef> = EntityRef::class.java

}