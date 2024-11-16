package takutility.dubdb.ops.dubber

import takutility.dubdb.DubDbContext
import takutility.dubdb.entities.Dubber
import takutility.dubdb.entities.Source
import takutility.dubdb.entities.SourceIds
import takutility.dubdb.tasks.wiki.FindPhoto
import takutility.dubdb.tasks.wiki.ReadDubberSection
import takutility.dubdb.tasks.wiki.ReadIds
import takutility.dubdb.wiki.WikiPage

class ExtractDubber(val context: DubDbContext) {

    fun run(page: WikiPage): Dubber {
        /*
        Leggi id wiki
        Leggi pagina wiki
        Leggi foto wiki
        Salva doppiatore
        Salva personaggi
         */

        val ids = SourceIds.of(Source.WIKI to page.title)
        ids += context[ReadIds::class].run(page).sourceIds

        val dubber = Dubber("", ids = ids)
        dubber.ids += context[FindPhoto::class].run(dubber).sourceIds

        context.dubberDb.save(dubber)

        context[ReadDubberSection::class].run(dubber, page).dubbedEntities
            ?.let(context.dubEntityDb::save)

        return dubber
    }
}