package takutility.dubdb.tasks.internal

import takutility.dubdb.entities.*
import takutility.dubdb.tasks.TaskResult
import takutility.dubdb.util.minOrNull
import java.util.*


class MergeEntityByActor {

    fun run(entities: Collection<DubbedEntity>, keepUnmerged: Boolean = false) : TaskResult {
        if (entities.size < 2) return TaskResult.empty
        val merged = if (!keepUnmerged) mutableListOf<DubbedEntity>() else null

        val sorted = entities.sortedBy { e -> e.sources.minOf {
            val i = sourceOrder.indexOf(it.dataSource)
            return@minOf if (i >= 0) i else 1000
        } }
        var partial = mergeByIds(sorted, merged)
        partial = mergeByName(partial, merged)

        if (merged == null) {
            return TaskResult(dubbedEntities = partial)
        } else {
            if (merged.isEmpty()) return TaskResult.empty
            merged.removeIf { it !in partial }
            return TaskResult(dubbedEntities = merged.toList())
        }
    }

    private fun mergeByIds(entities: List<DubbedEntity>, merged: MutableList<DubbedEntity>?) : List<DubbedEntity> {
        val used = BitSet()
        return entities.mapIndexedNotNull { index, e ->
            if (used[index]) return@mapIndexedNotNull null
            val actor = e.actor ?: return@mapIndexedNotNull e

            val group = mutableListOf<DubbedEntity>()
            val groupIds = mutableListOf<Int>()
            for (i2 in index+1 until entities.size) {
                if (used[i2]) continue
                val e2 = entities[i2]

                val match = e2.actor?.matches(actor)
                    ?: withoutDubId(e2.ids).let { ids -> ids.anyMatch(actor.ids) && ids.noMismatch(actor.ids) }
                if (match) {
                    if (group.isEmpty()) group.add(e)
                    group.add(e2)
                    groupIds.add(i2)
                }
            }
            groupIds.forEach { used[it] = true }

            if (group.size < 2 || group.all { it.actor == null })
                return@mapIndexedNotNull e

            val hasConflictingNames = group
                .flatMap { ge -> ge.sources.map { it.dataSource to ge.name } }
                .groupBy({ it.first }, { it.second })
                .any { (_, names) -> names.size > 1 && names.toSet().size > 1 }
            if (hasConflictingNames)
                return@mapIndexedNotNull e

            return@mapIndexedNotNull mergeGroup(group).apply { merged?.add(this) }
        }.toList()
    }

    private fun mergeByName(entities: List<DubbedEntity>, merged: MutableList<DubbedEntity>?): List<DubbedEntity> {
        return entities
            .groupBy { it.actor?.name ?: it.name }
            .values
            .flatMap {es -> if (es.size == 1 || es.all { it.actor == null } || !noMismatch(es))
                    es
                else
                    listOf(mergeGroup(es, true).apply { merged?.add(this) })
            }
    }

    private fun mergeGroup(entities: List<DubbedEntity>, promoteIds: Boolean = false): DubbedEntity {
        var actor = mergeRef(entities.mapNotNull { it.actor })!!
        if (promoteIds) {
            val aids = SourceIds()
            entities.forEach {e ->
                if (e.actor == null) {
                    e.ids.forEach {
                        if (it.source != Source.DUBDB && it !in actor.ids) aids += it
                    }
                }
            }
            if (aids.isNotEmpty()) {
                actor = actor.toRef()
                actor.ids += aids

            }
        }

        val ids = SourceIds()
        entities.forEach {e ->
            e.ids.forEach {
                if (it != actor.ids[it.source]) ids += it
            }
        }

        return DubbedEntity(
            name = entities[0].name,
            movie = entities.firstOrNull { it.movie.id != null }?.movie ?: entities[0].movie,
            dubber = mergeRef(entities.mapNotNull { it.dubber }),
            actor = actor,
            ids = ids,
            parseTs = minOrNull(entities.map { it.parseTs }),
            sources = entities.flatMap { it.sources }.distinct().toMutableList(),
        )
    }

    private fun withoutDubId(ids: SourceIds) =
        if (Source.DUBDB in ids) ids.toMutable().apply { remove(Source.DUBDB) }
        else ids

    private fun <T: TypedEntityRef<T>> mergeRef(refs: List<T>): T? {
        if (refs.isEmpty()) return null
        if (refs.size == 1) return refs[0]
        val sorted = refs.sortedByDescending { it.ids.size + (if (it.id != null) 100 else 0) + (if (it.isParsed) 1000 else 0) }
        val top = sorted[0]
        if (!sorted.all { top.ids.noMismatch(it.ids) }) return null

        val mergeIds = sorted.map { it.ids }.reduce { a, b -> a+b }
        if (top.ids.containsAll(mergeIds)) return top

        return top.toRef().apply { ids += mergeIds }
    }

    private fun noMismatch(entities: List<DubbedEntity>): Boolean {
        val ids = entities.map { e -> e.actor?.ids ?: withoutDubId(e.ids) }
        ids.forEachIndexed { i1, ids1 ->
            for (i2 in i1+1 until entities.size) {
                if (!ids1.noMismatch(ids[i2]))
                    return false
            }
        }
        return true
    }

    private val sourceOrder = listOf(DataSource.MOVIE_ORIG, DataSource.MOVIE_ORIG_DUB, DataSource.MOVIE_DUB,
        DataSource.TRAKT_MOVIE, DataSource.TRAKT_ACTOR)
}