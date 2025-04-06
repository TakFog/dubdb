package takutility.dubdb.tasks.trakt

import takutility.dubdb.DubDbContext
import takutility.dubdb.entities.MovieRef
import takutility.dubdb.tasks.TaskResult

class GetMovieCharas(context: DubDbContext) {
    private val trakt = context.trakt

    fun run(movie: MovieRef): TaskResult {
        TODO()
    }
}