package takutility.dubdb.tasks.wikidata

import takutility.dubdb.DubDbContext
import takutility.dubdb.entities.*
import takutility.dubdb.tasks.TaskResult
import takutility.dubdb.util.notEmpty
import takutility.dubdb.util.splitByType

class IdsFromWikidata(context: DubDbContext) {
    val wikidata = context.wikidata

    fun run(entity: EntityRef) = run(listOf(entity))

    fun run(entities: List<EntityRef>): TaskResult {
        if (entities.isEmpty()) return TaskResult.empty

        // find other ids
        val otherIds = wikidata.findIds(entities.mapNotNull { it.ids[Source.WIKIDATA]?.id }).filterValues { it.isNotEmpty() }
        if (otherIds.isEmpty()) return TaskResult.empty

        //update entities
        val updated = entities.filter { e -> e.ids[Source.WIKIDATA]?.id in otherIds }
        updated.forEach {e -> otherIds[e.ids[Source.WIKIDATA]?.id]?.let { e.ids += it }}

        // split by entity type
        val actors = mutableListOf<ActorRef>()
        val dubbers = mutableListOf<DubberRef>()
        val movies = mutableListOf<MovieRef>()
        updated.splitByType(actors, dubbers, movies)

        return TaskResult(actors = actors.notEmpty(), dubbers = dubbers.notEmpty(), movies = movies.notEmpty())
    }
}