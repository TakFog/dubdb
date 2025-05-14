package takutility.dubdb.ops.actor

import takutility.dubdb.DubDbContext
import takutility.dubdb.entities.Actor
import takutility.dubdb.entities.ActorRef
import takutility.dubdb.entities.DubbedEntity
import takutility.dubdb.entities.Source
import takutility.dubdb.tasks.internal.FindMissingActors
import takutility.dubdb.util.countInstances

class ExtractMissingActors(val context: DubDbContext) {

    fun run(entities: List<DubbedEntity>) {
        val result = context[FindMissingActors::class].run(entities)
        if (result.actors.isNullOrEmpty()) return

        val actorsWithWiki = result.actors.filter { Source.WIKI in it.ids }

        // limit the number of actors to extract
        val maxActors = context.config.wiki.maxMissingEntities
        val selectedActors = if (actorsWithWiki.size <= maxActors) actorsWithWiki
                else {
                    val byName = result.actors.map { it.name }.countInstances()
                    val byActor = context.dubEntityDb.countActors(actorsWithWiki)
                    val byEntity = context.dubEntityDb.countEntitiesBySource(Source.WIKI, actorsWithWiki.mapNotNull { it.wikiId })

                    actorsWithWiki
                        .sortedWith(Comparator
                            .comparingInt<ActorRef> { byActor[it] + byEntity[it.wikiId] }
                            .thenComparingInt { byActor[it] }
                            .thenComparingInt { byName[it.name] }
                            .reversed())
                        .take(maxActors)
                }

        // extract actors
        val savedActors = selectedActors.asSequence()
            .mapNotNull { it.wikiId }
            .map { context.wikiPageLoader.page(it) }
            .filter { it.exists() }
            .map { context[ExtractActor::class].run(it) }
            .toList()

        result.dubbedEntities?.forEach { e -> e.actor = savedActors.findActor(e.actor) }
    }

    private fun List<Actor>.findActor(actor: ActorRef?) = if (actor == null) null
            else this.find { actor.matches(it) } ?: actor
}