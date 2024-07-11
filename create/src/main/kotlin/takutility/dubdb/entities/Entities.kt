package takutility.dubdb.entities

import takutility.dubdb.util.bow

interface EntityRef<E> {
    val name: String?
    val ids: SourceIds

    fun get(): E?
}

abstract class Entity<E>(
    override val name: String,
    override val ids: SourceIds = SourceIds(),
    var parsed: Boolean = false,
    val sources: MutableList<DataSource> = mutableListOf(),
): EntityRef<E> {
    val tokens: List<String> = bow(name).toList()

}