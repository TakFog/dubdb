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
        fun with(sourceIds: ImmutableSourceIds) = TaskResult(sourceIds = sourceIds.toImmutable())
    }

    val actor get() = actors?.get(0)
    val dubber get() = dubbers?.get(0)
    val movie get() = movies?.get(0)
    val dubbedEntity get() = dubbedEntities?.get(0)
    val chara get() = charas?.get(0)
    fun id(source: Source) = sourceIds[source]?.id

    fun isEmpty() = actors == null
            && dubbers == null
            && movies == null
            && dubbedEntities == null
            && charas == null
            && sourceIds.isEmpty()
}
