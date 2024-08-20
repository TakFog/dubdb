package takutility.dubdb.tasks.trakt

import com.uwetrottmann.trakt5.entities.MovieIds
import com.uwetrottmann.trakt5.entities.SearchResult
import com.uwetrottmann.trakt5.entities.Show
import com.uwetrottmann.trakt5.entities.ShowIds

inline fun newResult(init: SearchResult.() -> Unit) = SearchResult().apply(init)

inline fun newMovie(init: com.uwetrottmann.trakt5.entities.Movie.() -> Unit) = com.uwetrottmann.trakt5.entities.Movie().apply(init)
inline fun newMovieIds(init: MovieIds.() -> Unit) = MovieIds().apply(init)

inline fun newShow(init: Show.() -> Unit) = Show().apply(init)
inline fun newShowIds(init: ShowIds.() -> Unit) = ShowIds().apply(init)
