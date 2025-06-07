package takutility.dubdb.tasks.wikidata

import takutility.dubdb.DubDbContext
import takutility.dubdb.entities.*
import takutility.dubdb.tasks.TaskResult

class FindWikidataId(context: DubDbContext) {
    private val wikidata = context.wikidata

    fun run(entities: Collection<EntityRef>): TaskResult {
        if (entities.isEmpty()) return TaskResult.empty

        //split by available ids
        val wiki = mutableMapOf<String, EntityRef>()
        val imdb = mutableMapOf<String, EntityRef>()
        entities.forEach {e ->
            e.wikiId?.let { wiki[it] = e }
                ?: e.ids[Source.IMDB]?.id?.let { imdb[it] = e }
        }

        // find ids
        val updated = (addWdids(wiki, wikidata::findIdsByItWiki) + addWdids(imdb, wikidata::findIdsByImdb)).toList()
        if (updated.isEmpty()) return TaskResult.empty

        // split by entity type
        val actors = mutableListOf<ActorRef>()
        val dubbers = mutableListOf<DubberRef>()
        val movies = mutableListOf<MovieRef>()

        updated.forEach {
            when(it) {
                is ActorRef -> actors.add(it)
                is DubberRef -> dubbers.add(it)
                is MovieRef -> movies.add(it)
            }
        }

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

    private fun <E>  List<E>.notEmpty() = this.ifEmpty { null }
}