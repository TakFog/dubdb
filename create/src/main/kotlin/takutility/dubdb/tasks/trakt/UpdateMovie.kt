package takutility.dubdb.tasks.trakt

import takutility.dubdb.entities.Movie
import takutility.dubdb.entities.MovieType
import takutility.dubdb.entities.Source.TRAKT
import takutility.dubdb.service.Trakt

class UpdateMovie(private val trakt: Trakt) {

    fun run(movie: Movie) {
        // get trakt info from imdb, if available
        val results = trakt.search(movie) ?: return
        val tid: Int? = if (movie.type == null || TRAKT !in movie.ids) {
            // get the trakt id and discriminate between movie or series
            results.iterate(
                movie = {
                    movie.type = MovieType.MOVIE
                    true
                },
                show = {
                    movie.type = MovieType.SERIES
                    true
                }).also { movie.traktId = it }
        } else {
            movie.traktId
        }

        if (tid != null)
            movie.year = results.getYear(tid)
    }
}