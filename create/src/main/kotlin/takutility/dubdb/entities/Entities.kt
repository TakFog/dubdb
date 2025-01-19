package takutility.dubdb.entities

import takutility.dubdb.util.bow
import java.time.Instant

interface EntityRef {
    val name: String?
    val ids: SourceIds
    var parsed: Boolean?

    val isParsed: Boolean
        get() = parsed == true

    fun intId(source: Source) = ids[source]?.toInt()

    var id: String?
        get() = ids[Source.DUBDB]?.id
        set(value) { ids[Source.DUBDB] = value }

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
    fun equalIds(other: EntityRef?): Boolean {
        if (other == null) return false
        return ids == other.ids
    }
    fun matches(other: EntityRef): Boolean {
        if (id != null && other.id != null)
            return id == other.id
        return ids.isCompatible(other.ids)
    }
}

abstract class BaseEntityRefImpl<E: EntityRef>(
    override var name: String?,
    override val ids: SourceIds = SourceIds(),
    override var parsed: Boolean? = null,
) : EntityRef {
    override fun get(): E? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BaseEntityRefImpl<*>

        if (name != other.name) return false
        if (ids != other.ids) return false

        return true
    }

    override fun hashCode(): Int {
        return ids.hashCode()
    }

    override fun toString(): String {
        return "$name ($ids)"
    }
}

class EntityRefImpl(name: String? = null, ids: SourceIds = SourceIds(), parsed: Boolean? = null): BaseEntityRefImpl<EntityRefImpl>(name, ids, parsed) {
    override fun toString(): String {
        return name ?: wikiId
            ?: if (ids.isNotEmpty()) ids.first().toString() else super.toString()
    }
    override fun toRef(): EntityRef = EntityRefImpl(name, ids.toMutable())
}

abstract class Entity(
    final override var name: String,
    override val ids: SourceIds = SourceIds(),
    var parseTs: Instant? = null,
    val sources: MutableList<RawData> = mutableListOf(),
): EntityRef {
    val tokens: List<String> = bow(name).toList()

    override var parsed: Boolean?
        get() = parseTs != null
        set(value) {
            value?.apply { parseTs = if (this) Instant.now() else null }
        }

    override fun toString(): String {
        return name
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Entity

        if (name != other.name) return false
        if (parseTs != other.parseTs) return false
        if (ids != other.ids) return false
        if (sources != other.sources) return false

        return true
    }

    override fun hashCode(): Int {
        return ids.hashCode()
    }
}

class EntityIds<T: EntityRef>(val entity: T) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EntityIds<*>

        if (!entity.equalIds(other.entity)) return false

        return true
    }

    override fun hashCode(): Int {
        return entity.ids.hashCode()
    }

    override fun toString(): String {
        return entity.ids.toString()
    }
}