package takutility.dubdb.tasks.internal

import takutility.dubdb.entities.DubbedEntity
import takutility.dubdb.entities.Source
import takutility.dubdb.entities.SourceIds
import takutility.dubdb.tasks.TaskResult

class MergeEntityByName {

    fun run(entities: Collection<DubbedEntity>) : TaskResult {
        val merged = entities.groupBy(DubbedEntity::name).values.mapNotNull { merge(it) }
        return if (merged.isEmpty()) TaskResult.empty else TaskResult(dubbedEntities = merged)
    }

    private fun merge(entities: Collection<DubbedEntity>): DubbedEntity? {
        if (entities.size < 2) return null
        val dubbed = entities.filter { it.dubber != null }
        if (dubbed.size != 1) return null
        val withActor = entities.filter { it.actor != null }

        // skip if any entity with actor comes from the same source
        if (sameSources(withActor)) return null

        val withDubber = dubbed[0]
        val wikiActor = (withActor.find { it.actor?.wiki != null } ?: withActor[0]).actor!!

        val ids = SourceIds()
        withActor.forEach { ids += it.ids }
        ids += withDubber.ids
        ids.remove(Source.DUBDB)

        val sources = withDubber.sources.toMutableList()
        withActor.forEach { sources += it.sources }

        return DubbedEntity(
            name = withDubber.name,
            movie = withDubber.movie,
            dubber = withDubber.dubber,
            actor = wikiActor,
            ids = ids,
            parseTs = minOrNull(withDubber.parseTs, withActor.map { it.parseTs }.reduce(this::minOrNull)),
            sources = sources
        )
    }

    private fun sameSources(entities: List<DubbedEntity>): Boolean {
        if (entities.size < 2) return false
        val sources = entities.map { e -> e.sources.map { it.sourceId } }

        sources.forEachIndexed { i, es ->
            for (j in i+1 until sources.size)
                if (sources[j].any { es.contains(it) }) return true
        }
        return false
    }

    private fun <T: Comparable<T>> minOrNull(a: T?, b: T?): T? = if (a == null || b == null) null else minOf(a, b)
}