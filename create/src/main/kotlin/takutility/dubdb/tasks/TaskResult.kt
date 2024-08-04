package takutility.dubdb.tasks

import takutility.dubdb.entities.*

data class TaskResult(
    val actors: List<ActorRef>? = null,
    val dubbers: List<DubberRef>? = null,
    val movies: List<MovieRef>? = null,
    val dubbedEntities: List<DubbedEntity>? = null,
    val charas: List<Chara>? = null,
    val sourceIds: ImmutableSourceIds = SourceIds.empty,
) {
    companion object {
        val empty = TaskResult()

        fun with(dubber: DubberRef) = TaskResult(dubbers = listOf(dubber))
        fun with(actor: ActorRef) = TaskResult(actors = listOf(actor))
        fun with(sourceIds: ImmutableSourceIds) = TaskResult(sourceIds = sourceIds)
        fun with(sourceIds: SourceIds) = TaskResult(sourceIds = sourceIds.toImmutable())
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
