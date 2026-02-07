package takutility.dubdb.ops.dubber

import takutility.dubdb.DubDbContext
import takutility.dubdb.entities.DubbedEntity
import takutility.dubdb.entities.Dubber
import takutility.dubdb.entities.SourceIds
import takutility.dubdb.ops.ExtractPerson
import takutility.dubdb.tasks.wiki.ReadDubberSection
import takutility.dubdb.wiki.WikiHtmlPage

class ExtractDubber(context: DubDbContext): ExtractPerson<Dubber>(context) {
    override val db = context.dubberDb

    override fun updateRefIds(persons: List<Dubber>) = context.dubEntityDb.updateRefIds(persons)

    override fun newPerson(title: String, ids: SourceIds) = Dubber(title, ids = ids)

    override fun withPerson(person: Dubber, page: WikiHtmlPage) {
        context[ReadDubberSection::class].run(person, page).dubbedEntities
            ?.let { saveEntities(person, it) }
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