package takutility.dubdb.tasks

import takutility.dubdb.entities.*

data class TaskResult(
    val actors: List<Actor>? = null,
    val dubbers: List<Dubber>? = null,
    val movies: List<Movie>? = null,
    val dubbedEntities: List<DubbedEntity>? = null,
    val charas: List<Chara>? = null,
    val sourceIds: ImmutableSourceIds = SourceIds.empty,
) {
    companion object {
        val empty = TaskResult()
    }

    fun actor() = actors?.get(0)
    fun dubber() = dubbers?.get(0)
    fun movie() = movies?.get(0)
    fun dubbedEntity() = dubbedEntities?.get(0)
    fun chara() = charas?.get(0)
    fun id(source: Source) = sourceIds[source]?.id

    fun isEmpty() = actors == null
            && dubbers == null
            && movies == null
            && dubbedEntities == null
            && charas == null
            && sourceIds.isEmpty()
}
