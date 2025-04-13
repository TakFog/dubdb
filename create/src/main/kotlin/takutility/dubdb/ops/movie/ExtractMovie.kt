package takutility.dubdb.ops.movie

import takutility.dubdb.DubDbContext
import takutility.dubdb.entities.Movie
import takutility.dubdb.entities.Source
import takutility.dubdb.entities.SourceIds
import takutility.dubdb.tasks.internal.MergeEntityByName
import takutility.dubdb.tasks.trakt.GetMovieCharas
import takutility.dubdb.tasks.trakt.UpdateMovie
import takutility.dubdb.tasks.wiki.ReadIds
import takutility.dubdb.tasks.wiki.ReadMovieInfobox
import takutility.dubdb.wiki.WikiPage
import java.time.Instant

class ExtractMovie(val context: DubDbContext) {

    fun run(page: WikiPage): Movie {
        /*
            Estrai id da wiki
            TODO Estrai titolo
            Estrai film da trakt
            Salva film

            Leggi infobox
            Estrai personaggi da trakt
            TODO Scarica doppiatori mancanti
            TODO Scarica attori mancanti
            Unisci personaggi
            Salva personaggi
         */
        val ids = SourceIds.of(Source.WIKI to page.title)
        ids += context[ReadIds::class].run(page).sourceIds

        val movie = getMovie(page.title, ids)
        context[UpdateMovie::class].run(movie)

        movie.parseTs = Instant.now()
        context.movieDb.save(movie)
        context.dubEntityDb.updateRefIds(listOf(movie))

        val infobox = context[ReadMovieInfobox::class].run(movie).dubbedEntities ?: listOf()
        val trakt = context[GetMovieCharas::class].run(movie).dubbedEntities ?: listOf()
        //TODO
        val fromDb = context.dubEntityDb.findByRef(movie)
        context[MergeEntityByName::class].run(infobox + trakt + fromDb, true).dubbedEntities
            ?.let { context.dubEntityDb.save(it) }

        return movie
    }

    private fun getMovie(name: String, ids: SourceIds): Movie {
        val results = context.movieDb.findBySources(ids)

        return if (results.size == 1) {
            val found = results[0]
            found.ids += ids
            found
        } else {
            Movie(name, ids = ids)
        }
    }
}