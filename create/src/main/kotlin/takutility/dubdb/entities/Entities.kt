package takutility.dubdb.entities

import takutility.dubdb.util.bow

interface EntityRef {
    val name: String?
    val ids: SourceIds

    val wiki: SourceId?
        get() = ids[Source.WIKI]

    var wikiId: String?
        get() = wiki?.id
        set(value) { ids[Source.WIKI] = value }

    fun get(): EntityRef?
}

open class BaseEntityRefImpl<E: EntityRef>(
    override val name: String?,
    override val ids: SourceIds = SourceIds()
) : EntityRef {
    override fun get(): E? = null
}

class EntityRefImpl(name: String?, ids: SourceIds = SourceIds()): BaseEntityRefImpl<EntityRefImpl>(name, ids) {
    override fun toString(): String {
        return name ?: wikiId
            ?: if (ids.isNotEmpty()) ids.first().toString() else super.toString()
    }
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