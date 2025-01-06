package takutility.dubdb.tasks.trakt

import com.uwetrottmann.trakt5.entities.*
import org.mockito.internal.stubbing.answers.CallsRealMethods
import org.mockito.kotlin.mock
import takutility.dubdb.loadConfig
import takutility.dubdb.service.Trakt
import takutility.dubdb.service.TraktImpl

abstract class TraktMock: Trakt {}

fun mockTrakt(stubbing: org.mockito.kotlin.KStubbing<TraktMock>.(TraktMock) -> Unit) = mock(defaultAnswer = CallsRealMethods(), stubbing = stubbing)

public val traktImpl by lazy { TraktImpl(loadConfig()) }

inline fun newResult(init: SearchResult.() -> Unit) = SearchResult().apply(init)

inline fun newMovie(init: com.uwetrottmann.trakt5.entities.Movie.() -> Unit) = com.uwetrottmann.trakt5.entities.Movie().apply(init)
inline fun newMovieIds(init: MovieIds.() -> Unit) = MovieIds().apply(init)
fun newMovieIds(trakt: Int? = null, imdb: String? = null) = MovieIds().also {
    it.trakt = trakt
    it.imdb = imdb
}

inline fun newShow(init: Show.() -> Unit) = Show().apply(init)
inline fun newShowIds(init: ShowIds.() -> Unit) = ShowIds().apply(init)
fun newShowIds(trakt: Int? = null, imdb: String? = null) = ShowIds().also {
    it.trakt = trakt
    it.imdb = imdb
}

fun newCast(
    characters: List<String?>? = null,
    movie: Movie? = null,
    show: Show? = null,
    person: Person? = null,
): CastMember {
    val c = CastMember()
    c.characters = characters
    c.movie = movie
    c.show = show
    c.person = person
    if (characters != null) c.character = characters.joinToString(", ")
    return c
}