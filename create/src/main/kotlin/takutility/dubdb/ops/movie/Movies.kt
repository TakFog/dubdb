package takutility.dubdb.ops.movie

import mu.KotlinLogging
import takutility.dubdb.DubDbContext
import takutility.dubdb.tasks.trakt.TraktMissingPopular
import takutility.dubdb.tasks.wikidata.FindWikidataId
import takutility.dubdb.tasks.wikidata.IdsFromWikidata

private val logger = KotlinLogging.logger {}

class Movies(val context: DubDbContext) {

    fun run(num: Int) {
        logger.info { "Extracting $num movies" }
        // get popular or trending movies
        var topMovies = context[TraktMissingPopular::class].run(num).movies
            ?.also { context[FindWikidataId::class].run(it) }
            ?.also { context[IdsFromWikidata::class].run(it) }
            ?: listOf()

        if (topMovies.size < num) {
            //get frequent movies from db
            logger.info { "Extracting ${num - topMovies.size} movies from DB" }
            val fromDb = context.dubEntityDb.findMostCommonMovies(num - topMovies.size)
            topMovies = topMovies + fromDb
        }

        val filtered = topMovies.asSequence()
            .filter { it.wikiId != null }
            .mapNotNull { m ->
                m.wikiId
                    ?.let { context.wikiPageLoader.page(it) }
                    ?.takeIf { it.exists() }
                    ?.let { p -> p to m }
            }
            .toList()

        var i = 0
        filtered.forEach {
            i++
            logger.info { "$i/${filtered.size} Extracting ${it.first.title}" }
            context[ExtractMovie::class].run(it.first, it.second)
        }
    }
}
