package takutility.dubdb.ops.movie

import takutility.dubdb.DubDbContext
import takutility.dubdb.entities.Movie
import takutility.dubdb.wiki.WikiPage

class ExtractMovie(val context: DubDbContext) {

    fun run(page: WikiPage): Movie {
        /*
            Estrai id da wiki
            Leggi infobox
            Estrai film da trakt
            Estrai personaggi da trakt
            Salva
         */
        TODO("not implemented yet")
    }
}