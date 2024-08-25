package takutility.dubdb.entities

import org.bson.BsonType
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.codecs.pojo.annotations.BsonRepresentation
import takutility.dubdb.util.bow

interface EntityRef {
    val name: String?
    val ids: SourceIds

    fun intId(source: Source) = ids[source]?.toInt()

    @BsonId
    @BsonRepresentation(BsonType.OBJECT_ID)
    fun id() = ids[Source.DUBDB]?.id

    val wiki: SourceId?
        get() = ids[Source.WIKI]

    var wikiId: String?
        get() = wiki?.id
        set(value) { ids[Source.WIKI] = value }

    var traktId: Int?
        get() = ids[Source.TRAKT]?.toInt()
        set(value) { ids[Source.TRAKT] = value.toString() }

    fun get(): EntityRef?
    fun toRef(): EntityRef
}

abstract class BaseEntityRefImpl<E: EntityRef>(
    override val name: String?,
    override val ids: SourceIds = SourceIds()
) : EntityRef {
    override fun get(): E? = null
}

class EntityRefImpl(name: String? = null, ids: SourceIds = SourceIds()): BaseEntityRefImpl<EntityRefImpl>(name, ids) {
    override fun toString(): String {
        return name ?: wikiId
            ?: if (ids.isNotEmpty()) ids.first().toString() else super.toString()
    }
    override fun toRef(): EntityRef = EntityRefImpl(name, ids.toMutable())
}

abstract class Entity(
    final override val name: String,
    override val ids: SourceIds = SourceIds(),
    var parsed: Boolean = false,
    val sources: MutableList<RawData> = mutableListOf(),
): EntityRef {
    val tokens: List<String> = bow(name).toList()

    override fun toString(): String {
        return name
    }
}