package takutility.dubdb.tasks.trakt

import takutility.dubdb.entities.ActorRef
import takutility.dubdb.entities.MovieRef
import takutility.dubdb.service.Trakt
import takutility.dubdb.tasks.TaskResult

class GetActorCharas(private val trakt: Trakt) {

    fun run(actor: ActorRef, movies: List<MovieRef>): TaskResult {
        return TaskResult.empty //TODO
    }
}