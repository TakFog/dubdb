package takutility.dubdb.ops

import takutility.dubdb.DubDbContext
import takutility.dubdb.db.EntityRepository
import takutility.dubdb.entities.Entity
import takutility.dubdb.entities.EntityRef
import takutility.dubdb.entities.Source
import takutility.dubdb.entities.SourceIds
import takutility.dubdb.tasks.wiki.FindPhoto
import takutility.dubdb.tasks.wiki.ReadIds
import takutility.dubdb.tasks.wiki.ReadTitle
import takutility.dubdb.wiki.WikiPage
import java.time.Instant

abstract class ExtractPerson<E>(val context: DubDbContext) where E: Entity, E: EntityRef {
    abstract val db: EntityRepository<E>

    fun run(page: WikiPage): E {
        /*
        Leggi id wiki
        Leggi titolo wiki
        Leggi pagina wiki
        Leggi foto wiki
        Altri id

        Salva persona
         */

        val ids = readIds(page)

        val title = context[ReadTitle::class].run(page).string ?: page.title
        val person = getPerson(title, ids)
        person.ids += context[FindPhoto::class].run(person).sourceIds
        moreIds(person)

        person.parseTs = Instant.now()
        db.save(person)
        updateRefIds(listOf(person))

        withPerson(person, page)

        return person
    }

    private fun readIds(page: WikiPage): SourceIds {
        val ids = SourceIds.of(Source.WIKI to page.title)
        ids += context[ReadIds::class].run(page).sourceIds
        return ids
    }

    protected abstract fun updateRefIds(persons: List<E>)
    protected abstract fun newPerson(title: String, ids: SourceIds): E

    protected open fun moreIds(person: E) {}
    protected open fun withPerson(person: E, page: WikiPage) {}

    private fun getPerson(title: String, ids: SourceIds): E {
        val results = db.findBySources(ids)

        return if (results.size == 1) {
            val found = results[0]
            found.ids += ids
            found
        } else {
            newPerson(title, ids)
        }
    }

}