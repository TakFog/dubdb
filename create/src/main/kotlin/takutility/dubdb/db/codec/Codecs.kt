package takutility.dubdb.db.codec

import org.bson.BsonReader
import org.bson.BsonType
import org.bson.BsonWriter
import org.bson.codecs.*
import org.bson.codecs.configuration.CodecRegistry
import takutility.dubdb.entities.*

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

class RawDataCodec: Codec<RawData> {
    override fun encode(writer: BsonWriter?, value: RawData?, encoderContext: EncoderContext?) {
        if (value == null) {
            writer!!.writeNull()
            return
        }
        writer!!.writeStartDocument()
        writer.writeString("source", value.sourceId.source.name)
        writer.writeString("sourceId", value.sourceId.id)
        writer.writeString("dataSource", value.dataSource.name)
        writer.writeString("raw", value.raw)
        writer.writeEndDocument()
    }

    override fun decode(r: BsonReader?, decoderContext: DecoderContext?): RawData {
        var source: String? = null
        var sourceId: String? = null
        var dataSource: String? = null
        var raw: String? = null
        r!!.readStartDocument()
        while (r.readBsonType() != BsonType.END_OF_DOCUMENT) {
            when(r.readName()) {
                "source" -> source = r.readString()
                "sourceId" -> sourceId = r.readString()
                "dataSource" -> dataSource = r.readString()
                "raw" -> raw = r.readString()
            }
        }
        r.readEndDocument()

        return RawData(
            sourceId = SourceId(Source.valueOf(source!!), sourceId!!),
            dataSource = DataSource.valueOf(dataSource!!),
            raw = raw!!,
        )
    }

    override fun getEncoderClass(): Class<RawData> = RawData::class.java

}

abstract class EntityBaseEncoder<E: EntityRef>(registry: CodecRegistry): Encoder<E> {
    protected val idsCodec: Codec<SourceIds>

    init {
        idsCodec = registry[SourceIds::class.java]
    }

    override fun encode(w: BsonWriter?, ref: E?, ctx: EncoderContext?) {
        if (ref == null) {
            w!!.writeNull()
            return
        }
        w!!.writeStartDocument()
        encodeObject(w, ref, ctx)
        w.writeEndDocument()
    }

    protected open fun encodeObject(w: BsonWriter, entity: E, ctx: EncoderContext?) {
        encodeRef(w, entity, ctx)
    }

    protected fun encodeRef(w: BsonWriter, ref: EntityRef, ctx: EncoderContext?) {
        ref.name?.let { w.writeString("name", it) }
        if (ref.ids.isNotEmpty()) {
            w.writeName("ids")
            idsCodec.encode(w, ref.ids, ctx)
        }
    }
}

abstract class EntityCodec<E: Entity>(registry: CodecRegistry): EntityBaseEncoder<E>(registry), Codec<E> {
    private val srcCodec: Codec<List<RawData>>

    init {
        @Suppress("unchecked")
        srcCodec = registry[List::class.java, listOf(RawData::class.java)] as Codec<List<RawData>>
    }

    override fun encodeObject(w: BsonWriter, entity: E, ctx: EncoderContext?) {
        super.encodeObject(w, entity, ctx)
        w.writeBoolean("parsed", entity.parsed)
        if (entity.sources.isNotEmpty()) {
            w.writeName("sources")
            srcCodec.encode(w, entity.sources, ctx)
        }
    }

    override fun decode(r: BsonReader?, ctx: DecoderContext?): E {
        val inst = newInstance()
        r!!.readStartDocument()
        while (r.readBsonType() != BsonType.END_OF_DOCUMENT) {
            decodeField(r.readName(), r, ctx, inst)
        }
        r.readEndDocument()

        return inst
    }

    protected abstract fun newInstance(): E

    protected open fun decodeField(key: String, r: BsonReader, ctx: DecoderContext?, inst: E) {
        when (key) {
            "name" -> inst.name = r.readString()
            "ids" -> idsCodec.decode(r, ctx)?.let { inst.ids += it }
            "parsed" -> inst.parsed = r.readBoolean()
            "sources" -> inst.sources += srcCodec.decode(r, ctx) as List<RawData>
        }
    }
}

class EntityRefCodec(registry: CodecRegistry): EntityBaseEncoder<EntityRef>(registry), Codec<EntityRef> {
    override fun getEncoderClass(): Class<EntityRef> = EntityRef::class.java

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
}

class ActorCodec(registry: CodecRegistry) : EntityCodec<Actor>(registry) {
    override fun newInstance(): Actor = Actor("")

    override fun getEncoderClass(): Class<Actor> = Actor::class.java
}

class DubberCodec(registry: CodecRegistry) : EntityCodec<Dubber>(registry) {
    override fun newInstance(): Dubber = Dubber("")

    override fun getEncoderClass(): Class<Dubber> = Dubber::class.java
}