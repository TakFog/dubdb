package takutility.dubdb.tasks.trakt

import takutility.dubdb.DubDbContext
import takutility.dubdb.entities.MovieRef
import takutility.dubdb.mapper.toRef
import takutility.dubdb.service.MovieOrShow
import takutility.dubdb.tasks.TaskResult
import takutility.dubdb.util.limit

class TraktMissingPopular(private val context: DubDbContext) {
    private val trakt = context.trakt

    fun run(limit: Int): TaskResult {
        var result = unparsed(trakt.mostPopular(limit * 5)).limit(limit)
        if (result.size < limit) {
            val missing = limit - result.size
            result = result + unparsed(trakt.trending(limit * 5)).limit(missing)
        }

        return TaskResult(movies = result.map { it.toRef() })
    }

    private fun unparsed(list: List<MovieOrShow>): List<MovieRef> {
        if (list.isEmpty()) return listOf()
        return list.mapNotNull {
            val ref = it.toRef()
            val inDb = context.movieDb.findBySources(ref)
            if (inDb.isEmpty())
                ref
            else if (inDb.none(MovieRef::isParsed))
                inDb.first().toRef()
            else
                null
        }
    }
}