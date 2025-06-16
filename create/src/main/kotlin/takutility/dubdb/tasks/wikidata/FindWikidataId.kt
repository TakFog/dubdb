package takutility.dubdb.tasks.wikidata

import takutility.dubdb.DubDbContext
import takutility.dubdb.entities.*
import takutility.dubdb.tasks.TaskResult
import takutility.dubdb.util.notEmpty
import takutility.dubdb.util.splitByType

class FindWikidataId(context: DubDbContext) {
    private val wikidata = context.wikidata

    fun run(entities: Collection<EntityRef>): TaskResult {
        if (entities.isEmpty()) return TaskResult.empty

        //split by available ids
        val wdid = mutableListOf<EntityRef>()
        val wiki = mutableMapOf<String, EntityRef>()
        val imdb = mutableMapOf<String, EntityRef>()
        entities.forEach {e ->
            if (Source.WIKIDATA in e.ids)
                wdid.add(e)
            else {
                e.wikiId?.let { wiki[it] = e }
                    ?: e.ids[Source.IMDB]?.id?.let { imdb[it] = e }
            }
        }

        // find ids
        val updated = (wdid.asSequence() + addWdids(wiki, wikidata::findIdsByItWiki) + addWdids(imdb, wikidata::findIdsByImdb)).toList()
        if (updated.isEmpty()) return TaskResult.empty

        // split by entity type
        val actors = mutableListOf<ActorRef>()
        val dubbers = mutableListOf<DubberRef>()
        val movies = mutableListOf<MovieRef>()

        updated.splitByType(actors, dubbers, movies)

        return TaskResult(actors = actors.notEmpty(), dubbers = dubbers.notEmpty(), movies = movies.notEmpty())
    }

    private fun addWdids(entityMap: Map<String, EntityRef>, action: (Collection<String>) -> Map<String, String>): Sequence<EntityRef> {
        if (entityMap.isEmpty()) return emptySequence()

        return action(entityMap.keys).asSequence().mapNotNull { (k, wdid) ->
            entityMap[k]?.let {
                it.ids[Source.WIKIDATA] = wdid
                return@mapNotNull it
            }
            return@mapNotNull null
        }
    }
}