package takutility.dubdb.ops

import takutility.dubdb.DubDbContext
import takutility.dubdb.db.EntityRepository
import takutility.dubdb.entities.Entity
import takutility.dubdb.entities.EntityRef
import takutility.dubdb.entities.Source
import takutility.dubdb.entities.SourceIds
import takutility.dubdb.tasks.TaskResult
import takutility.dubdb.tasks.wiki.FindPhoto
import takutility.dubdb.tasks.wiki.ReadIds
import takutility.dubdb.tasks.wiki.ReadTitle
import takutility.dubdb.tasks.wikidata.IdsFromWikidata
import takutility.dubdb.wiki.WikiHtmlPage
import takutility.dubdb.wiki.WikiPage
import java.time.Instant

abstract class ExtractPerson<E>(val context: DubDbContext) where E: Entity, E: EntityRef {
    abstract val db: EntityRepository<E>

    fun runHtml(page: WikiHtmlPage): E {
        val person = run(page, page.title, ReadIds::run, ReadTitle::run, this::withPerson)
        withPerson(person, page)
        return person
    }

    fun run(page: WikiPage): E {
        val person = run(page, page.title, ReadIds::run, ReadTitle::run, this::withPerson)
        withPerson(person, page)
        return person
    }

    private fun <P> run(page: P, pageTitle: String,
                                readIds: (ReadIds, P) -> TaskResult,
                                readTitle: (ReadTitle, P) -> TaskResult,
                                withPerson: (E, P) -> Unit
                        ): E {
        /*
        Leggi id wiki
        Leggi titolo wiki
        Leggi pagina wiki
        Leggi foto wiki
        Altri id

        Salva persona
         */

        val ids = readIds(page, pageTitle, readIds)

        val title = readTitle(context[ReadTitle::class], page).string ?: pageTitle
        val person = getPerson(title, ids)
        person.ids += context[FindPhoto::class].run(person).sourceIds
        person.ids += context[IdsFromWikidata::class].run(person).sourceIds
        moreIds(person)

        person.parseTs = Instant.now()
        db.save(person)
        updateRefIds(listOf(person))

        withPerson(person, page)

        return person
    }

    private fun <P> readIds(page: P, title: String, func: (ReadIds, P) -> TaskResult): SourceIds {
        val ids = SourceIds.of(Source.WIKI to title)
        ids += func(context[ReadIds::class], page).sourceIds
        return ids
    }

    protected abstract fun updateRefIds(persons: List<E>)
    protected abstract fun newPerson(title: String, ids: SourceIds): E

    protected open fun moreIds(person: E) {}
    protected open fun withPerson(person: E, page: WikiHtmlPage) {}
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