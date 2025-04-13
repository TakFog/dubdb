package takutility.dubdb.tasks.trakt

import takutility.dubdb.DubDbContext
import takutility.dubdb.entities.*
import takutility.dubdb.entities.Source.IMDB
import takutility.dubdb.entities.Source.TRAKT
import takutility.dubdb.tasks.TaskResult

class GetMovieCharas(context: DubDbContext) {
    private val trakt = context.trakt

    fun run(movie: MovieRef): TaskResult {
        val traktId = movie.traktId ?: return TaskResult.empty
        val trackSourceId = movie.ids[TRAKT]!!

        val entities = trakt.movieCredits(traktId)?.asSequence()?.flatMap { credit ->
            credit.person
                ?.run {
                    ActorRefImpl(
                        name, SourceIds.of(
                            TRAKT to ids.trakt?.toString(), IMDB to ids.imdb
                        )
                    )
                }
                ?.let { actor ->
                    credit.characters?.asSequence()?.map { name ->
                        DubbedEntity(
                            name = name,
                            movie = movie,
                            actor = actor,
                            sources = mutableListOf(RawData(trackSourceId, DataSource.TRAKT_MOVIE, name))
                        )
                    }
                }
                ?: sequenceOf()
        }?.toList() ?: return TaskResult.empty

        return TaskResult(dubbedEntities = entities)
    }
}