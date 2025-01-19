package takutility.dubdb.ops.dubber

import takutility.dubdb.DubDbContext
import takutility.dubdb.entities.DubbedEntity
import takutility.dubdb.entities.Dubber
import takutility.dubdb.entities.Source
import takutility.dubdb.entities.SourceIds
import takutility.dubdb.tasks.wiki.FindPhoto
import takutility.dubdb.tasks.wiki.ReadDubberSection
import takutility.dubdb.tasks.wiki.ReadIds
import takutility.dubdb.wiki.WikiPage
import java.time.Instant

class ExtractDubber(val context: DubDbContext) {

    fun run(page: WikiPage): Dubber {
        /*
        Leggi id wiki
        Leggi titolo wiki
        Leggi pagina wiki
        Leggi foto wiki
        Salva doppiatore
        Salva personaggi
         */

        val ids = SourceIds.of(Source.WIKI to page.title)
        ids += context[ReadIds::class].run(page).sourceIds

        val dubber = getDubber(ids)
        dubber.ids += context[FindPhoto::class].run(dubber).sourceIds

        dubber.parseTs = Instant.now()
        context.dubberDb.save(dubber)
        context.dubEntityDb.updateRefIds(listOf(dubber))

        context[ReadDubberSection::class].run(dubber, page).dubbedEntities
            ?.let { saveEntities(dubber, it) }

        return dubber
    }

    private fun getDubber(ids: SourceIds): Dubber {
        val results = context.dubberDb.findBySources(ids)

        return if (results.size == 1) {
            val found = results[0]
            found.ids += ids
            found
        } else {
            Dubber("", ids = ids)
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