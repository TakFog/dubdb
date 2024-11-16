package takutility.dubdb.ops.dubber

import takutility.dubdb.DubDbContext
import takutility.dubdb.entities.Dubber
import takutility.dubdb.entities.Source
import takutility.dubdb.entities.SourceIds
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
        ids += context.readIds.run(page).sourceIds

        val dubber = Dubber("", ids = ids)

        context.dubberDb.save(dubber)

        context.readDubberSection.run(dubber, page).dubbedEntities
            ?.let(context.dubEntityDb::save)

        return dubber
    }
}