package takutility.dubdb.ops.movie

import takutility.dubdb.DubDbContext
import takutility.dubdb.db.EntityRepository
import takutility.dubdb.entities.*
import takutility.dubdb.tasks.internal.MergeEntityByActor
import takutility.dubdb.tasks.internal.MergeEntityByName
import takutility.dubdb.tasks.trakt.GetMovieCharas
import takutility.dubdb.tasks.trakt.UpdateMovie
import takutility.dubdb.tasks.wiki.ReadIds
import takutility.dubdb.tasks.wiki.ReadMovieInfobox
import takutility.dubdb.tasks.wiki.ReadTitle
import takutility.dubdb.tasks.wikidata.FindWikidataId
import takutility.dubdb.tasks.wikidata.IdsFromWikidata
import takutility.dubdb.wiki.WikiPage
import java.time.Instant
import kotlin.reflect.KMutableProperty1

class ExtractMovie(val context: DubDbContext) {

    fun run(source: MovieRef): Movie {
        val title = source.wikiId!!
        return run(context.wikiPageLoader.page(title), source)
    }

    fun run(page: WikiPage, source: MovieRef? = null): Movie {
        /*
            Estrai id da wiki
            Estrai titolo
            Estrai film da trakt
            Salva film

            Leggi infobox
            Estrai personaggi da trakt
            Scarica id attori mancanti
            Unisci personaggi
            Salva personaggi
         */
        val ids = source?.ids ?: SourceIds.of(Source.WIKI to page.title)
        ids += context[ReadIds::class].run(page).sourceIds

        val title = context[ReadTitle::class].run(page).string ?: page.title
        val movie = getMovie(title, ids)
        context[UpdateMovie::class].run(movie)

        movie.parseTs = Instant.now()
        context.movieDb.save(movie)
        context.dubEntityDb.updateRefIds(listOf(movie))

        val infobox = context[ReadMovieInfobox::class].run(movie).dubbedEntities ?: listOf()
        val trakt = context[GetMovieCharas::class].run(movie).dubbedEntities ?: listOf()
        val fromDb = context.dubEntityDb.findByRef(movie)
        val allEntities = infobox + trakt + fromDb

        loadRefs(context.dubberDb, allEntities, DubbedEntity::dubber)
        loadRefs(context.actorDb, allEntities, DubbedEntity::actor)

        findActorIds(allEntities)

        allEntities
            .let { context[MergeEntityByActor::class].run(it, true).dubbedEntities }
            ?.let { context[MergeEntityByName::class].run(it, true).dubbedEntities }
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

    private fun <E, R> loadRefs(db: EntityRepository<E>, entities: List<DubbedEntity>,
                                prop : KMutableProperty1<DubbedEntity, R?>
    ) where E: Entity, E: EntityOf<E,R>, R: EntityRefOf<E> {
        entities
            .filter { e -> prop.get(e)?.let { it.id == null } == true } //keep entities with the prop without id
            .groupBy { prop.get(it)?.ids }
            .forEach { (e, des) ->
                val found = db.findBySources(e ?: return@forEach)
                if (found.size == 1) des.forEach { de -> prop.set(de, found[0].asRef()) }
            }
    }

    /**
     * Add IMDB and WIKI ids to entity actors
     */
    private fun findActorIds(entities: List<DubbedEntity>) {
        entities.mapNotNull { it.actor }
            // exclude actors already filled
            .filterNot { Source.IMDB in it.ids && Source.WIKI in it.ids }
            .chunked(100)
            .forEach {
                context[FindWikidataId::class].run(it)
                context[IdsFromWikidata::class].run(it)
            }
    }
}