package takutility.dubdb.tasks.internal

import takutility.dubdb.entities.DubbedEntity
import takutility.dubdb.entities.Source
import takutility.dubdb.tasks.TaskResult

class MergeEntityByName {

    fun run(entities: Collection<DubbedEntity>) : TaskResult {
        val merged = entities.groupBy(DubbedEntity::name).values.mapNotNull { merge(it) }
        return if (merged.isEmpty()) TaskResult.empty else TaskResult(dubbedEntities = merged)
    }

    private fun merge(entities: Collection<DubbedEntity>): DubbedEntity? {
        if (entities.size != 2) return null
        val withDubber = entities.find { it.dubber != null } ?: return null
        val withActor = entities.find { it.actor != null } ?: return null

        return DubbedEntity(
            name = withDubber.name,
            movie = withDubber.movie,
            dubber = withDubber.dubber,
            actor = withActor.actor,
            ids = (withDubber.ids + withActor.ids).apply { this.remove(Source.DUBDB) },
            parseTs = minOrNull(withDubber.parseTs, withActor.parseTs),
            sources = withDubber.sources.toMutableList().apply { this += withActor.sources}
        )
    }

    private fun <T: Comparable<T>> minOrNull(a: T?, b: T?): T? = if (a == null || b == null) null else minOf(a, b)
}