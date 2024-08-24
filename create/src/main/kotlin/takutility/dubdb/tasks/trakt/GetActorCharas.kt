package takutility.dubdb.tasks.trakt

import takutility.dubdb.entities.*
import takutility.dubdb.entities.Source.TRAKT
import takutility.dubdb.service.Trakt
import takutility.dubdb.service.movieOrShow
import takutility.dubdb.tasks.TaskResult

class GetActorCharas(private val trakt: Trakt) {

    fun run(actor: ActorRef, movies: List<MovieRef>): TaskResult {
        val movieIds = movies.mapNotNull { it.traktId }.toSet()
        if (movieIds.isEmpty()) return TaskResult.empty // no movies with track id, can't filter

        val credits = actor.traktId?.let(trakt::personCredits) ?: return TaskResult.empty

        val actorOut = actor.toRef()

        val charas = credits.asSequence()
            .flatMap { member ->
                val movie = member.movieOrShow()
                    ?.takeIf { it.ids?.trakt in movieIds }
                    ?.let { mos ->
                        movieRefOf(
                            mos.title,
                            type = if (mos.isMovie()) MovieType.MOVIE else MovieType.SERIES,
                            ids = SourceIds.of(TRAKT to mos.ids?.trakt?.toString(), Source.IMDB to mos.ids?.imdb)
                        )
                    }
                    ?: return@flatMap sequenceOf()

                return@flatMap member.characters.asSequence().map { name ->
                    DubbedEntity(
                        name = name,
                        movie = movie,
                        actor = actorOut,
                        sources = mutableListOf(RawData(actorOut.ids[TRAKT]!!, DataSource.TRAKT, name))
                    )
                }
            }
            .toList()


        return TaskResult(dubbedEntities = charas)
    }

}