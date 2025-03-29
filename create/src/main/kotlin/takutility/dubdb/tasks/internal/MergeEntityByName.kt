package takutility.dubdb.tasks.internal

import takutility.dubdb.entities.*
import takutility.dubdb.tasks.TaskResult

class MergeEntityByName {

    fun run(entities: Collection<DubbedEntity>, keepUnmerged: Boolean = false) : TaskResult {
        val merged = entities.groupBy(DubbedEntity::name).values
            .flatMap { des -> merge(des)?.let { listOf(it) } ?: if (keepUnmerged) des else listOf() }
        return if (merged.isEmpty()) TaskResult.empty else TaskResult(dubbedEntities = merged)
    }

    private fun merge(entities: Collection<DubbedEntity>): DubbedEntity? {
        if (entities.size < 2) return null
        val withDubber = entities.filter { it.dubber != null }
        val mainSource = if (withDubber.size == 1)
            withDubber[0]
        else if (withDubber.isEmpty() || sameSources(withDubber))
            return null
        else
            selectDubbedMainSource(withDubber) ?: return null

        val withActor = entities.filter { it.actor != null }

        // skip if any entity with actor comes from the same source
        if (sameSources(withActor)) return null

        val wikiActor = (withActor.find { it.actor?.wiki != null } ?: withActor[0]).actor!!

        val ids = SourceIds()
        withActor.forEach { ids += it.ids }
        withDubber.forEach { ids += it.ids }
        ids[Source.DUBDB] = mainSource.id

        val sources = mutableListOf<RawData>()
        withDubber.forEach { sources += it.sources }
        withActor.forEach { sources += it.sources }

        return DubbedEntity(
            name = mainSource.name,
            movie = mainSource.movie,
            dubber = mainSource.dubber,
            actor = wikiActor,
            ids = ids,
            parseTs = minOrNull(withDubber.map { it.parseTs }, withActor.map { it.parseTs }),
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

    private fun selectDubbedMainSource(dubbed: List<DubbedEntity>): DubbedEntity? {
        //check if all dubbers are compatible
        dubbed.forEachIndexed { i, e ->
            for (j in i+1 until dubbed.size)
                if (e.dubber?.matches(dubbed[j]) != true) return null
        }
        val dubber = biggestDubber(dubbed.map { it.dubber!! })
        val id = dubbed.firstNotNullOfOrNull { it.id }
        // if there is an entity with the biggest dubber use it
        dubbed.firstOrNull { dubber == it.dubber && (id == null || it.id != null) }?.let { return it }
        //otherwise build one
        val fromMovie = dubbed.first { it.sources.any { it.dataSource.parent == ParentDataSource.MOVIE} }
        return DubbedEntity(
            name = fromMovie.name,
            movie = fromMovie.movie,
            dubber = dubber,
            ids = SourceIds.join(dubbed.map { it.ids }),
            sources = dubbed.flatMap { it.sources }.toMutableList(),
        )
    }

    private fun biggestDubber(dubbers: List<DubberRef>): DubberRef {
        val dubber = dubbers.maxBy { it.ids.size }
        if (dubbers.all { dubber.ids.containsAll(it.ids) })
            return dubber
        val fromDb = dubbers.firstOrNull { it.isParsed } ?: dubbers.firstOrNull { it.id != null }
        return DubberRefImpl(
            name = fromDb?.name ?: dubbers.maxBy { it.name?.length ?: 0 }.name,
            ids = SourceIds.join(dubbers.map { it.ids }),
            parsed = when {
                dubbers.any { it.isParsed } -> true
                dubbers.all { it.parsed == false } -> false
                else -> null
            }
        )
    }

    private fun <T: Comparable<T>> minOrNull(vararg a: Collection<T?>): T? = a.asSequence().flatMap { it }.reduce(this::minOrNull)
    private fun <T: Comparable<T>> minOrNull(a: T?, b: T?): T? = if (a == null || b == null) null else minOf(a, b)
}