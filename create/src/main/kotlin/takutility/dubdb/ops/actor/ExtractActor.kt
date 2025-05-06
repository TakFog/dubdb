package takutility.dubdb.ops.actor

import takutility.dubdb.DubDbContext
import takutility.dubdb.entities.*
import takutility.dubdb.tasks.trakt.UpdateActor
import takutility.dubdb.tasks.wiki.FindPhoto
import takutility.dubdb.tasks.wiki.ReadIds
import takutility.dubdb.wiki.WikiPage
import java.time.Instant

class ExtractActor(val context: DubDbContext) {

    fun run(page: WikiPage): Actor {
        /*
        Leggi id wiki
        Leggi titolo wiki
        Leggi pagina wiki
        Leggi foto wiki
        Cerca su trakt
        Salva attore
        TODO Salva personaggi
         */

        val ids = SourceIds.of(Source.WIKI to page.title)
        ids += context[ReadIds::class].run(page).sourceIds

        val actor = getActor(ids)
        actor.ids += context[FindPhoto::class].run(actor).sourceIds
        context[UpdateActor::class].run(actor)

        actor.parseTs = Instant.now()
        context.actorDb.save(actor)
        context.dubEntityDb.updateRefIds(listOf(actor))

        return actor
    }

    private fun getActor(ids: SourceIds): Actor {
        val results = context.actorDb.findBySources(ids)

        return if (results.size == 1) {
            val found = results[0]
            found.ids += ids
            found
        } else {
            Actor("", ids = ids)
        }
    }

    private fun saveEntities(dubber: Dubber, entities: List<DubbedEntity>) {
        val oldEntities = context.dubEntityDb.findByRef(dubber).groupBy { it.movie }

        val toSave = if (oldEntities.isEmpty()) entities
        else
            // select entities not already present in the db
            entities.filter { e -> oldEntities[e.movie]?.none { o ->
                if (e.name == o.name) return@none true
                o.actor?.let { if (e.name == it.name || (e.ids.isNotEmpty() && it.matches(e))) return@none true }
                return@none false
            } ?: true }

        context.dubEntityDb.save(toSave)
    }

}