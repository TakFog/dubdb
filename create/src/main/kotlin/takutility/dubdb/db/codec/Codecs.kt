package takutility.dubdb.db.codec

import org.bson.BsonReader
import org.bson.BsonType
import org.bson.BsonWriter
import org.bson.codecs.*
import org.bson.codecs.configuration.CodecRegistry
import org.bson.types.ObjectId
import takutility.dubdb.entities.*

class SourceIdsCodec(val ignoreId: Boolean): Codec<SourceIds> {
    override fun encode(writer: BsonWriter?, value: SourceIds?, encoderContext: EncoderContext?) {
        if (value == null) {
            writer!!.writeNull()
            return
        }
        writer!!.writeStartDocument()
        value.forEach {
            if (!ignoreId || it.source != Source.DUBDB)
                writer.writeString(it.source.name, it.id)
        }
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

abstract class EntityBaseEncoder<E: EntityRef>(registry: CodecRegistry, ignoreId: Boolean): Encoder<E> {
    protected val idsCodec: SourceIdsCodec

    init {
        idsCodec = SourceIdsCodec(ignoreId)
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
        entity.name?.let { w.writeString("name", it) }
        if (containsSomething(entity.ids)) {
            w.writeName("ids")
            idsCodec.encode(w, entity.ids, ctx)
        }
    }

    private fun containsSomething(ids: SourceIds): Boolean {
        if (ids.isEmpty()) return false
        if (!idsCodec.ignoreId) return true
        if (Source.DUBDB !in ids) return true
        return ids.size > 1
    }
}

abstract class EntityCodec<E: Entity>(registry: CodecRegistry): EntityBaseEncoder<E>(registry, true), Codec<E> {
    private val srcCodec: Codec<List<RawData>>

    init {
        @Suppress("unchecked")
        srcCodec = registry[List::class.java, listOf(RawData::class.java)] as Codec<List<RawData>>
    }

    override fun encodeObject(w: BsonWriter, entity: E, ctx: EncoderContext?) {
        val id = entity.id
        if (id != null) {
            w.writeObjectId("_id", ObjectId(id))
        }
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
            "_id" -> inst.id = r.readObjectId().toHexString()
            "name" -> inst.name = r.readString()
            "ids" -> idsCodec.decode(r, ctx)?.let { inst.ids += it }
            "parsed" -> inst.parsed = r.readBoolean()
            "sources" -> inst.sources += srcCodec.decode(r, ctx) as List<RawData>
        }
    }
}

abstract class BaseEntityRefCodec<T : EntityRef>(registry: CodecRegistry): EntityBaseEncoder<T>(registry, false), Codec<T> {

    override fun decode(r: BsonReader?, ctx: DecoderContext?): T {
        var name: String? = null
        var ids: SourceIds? = null
        var otherFields: MutableMap<String, Any>? = null
        r!!.readStartDocument()
        while (r.readBsonType() != BsonType.END_OF_DOCUMENT) {
            val fieldName = r.readName()
            when (fieldName) {
                "name" -> name = r.readString()
                "ids" -> ids = idsCodec.decode(r, ctx)
                else -> {
                    var value = decodeAdditionalFields(fieldName, r, ctx)
                    if (value != null) {
                        if (otherFields == null) otherFields = mutableMapOf()
                        otherFields[fieldName] = value
                    }
                }
            }
        }
        r.readEndDocument()

        return createEntityRef(name, ids ?: SourceIds(), otherFields)
    }
    protected open fun decodeAdditionalFields(fieldName: String, r: BsonReader, ctx: DecoderContext?): Any? = null

    protected abstract fun createEntityRef(name: String?, ids: SourceIds, fields: Map<String, Any>?): T
}

class EntityRefCodec(registry: CodecRegistry) : BaseEntityRefCodec<EntityRef>(registry) {
    override fun getEncoderClass(): Class<EntityRef> = EntityRef::class.java

    override fun createEntityRef(name: String?, ids: SourceIds, fields: Map<String, Any>?): EntityRef {
        return EntityRefImpl(name = name, ids = ids)
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