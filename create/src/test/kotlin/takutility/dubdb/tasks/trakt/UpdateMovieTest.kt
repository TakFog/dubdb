package takutility.dubdb.tasks.trakt

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import takutility.dubdb.entities.Movie
import takutility.dubdb.entities.MovieType
import takutility.dubdb.entities.Source
import takutility.dubdb.entities.Source.IMDB
import takutility.dubdb.entities.Source.TRAKT
import takutility.dubdb.entities.SourceIds
import takutility.dubdb.service.Trakt
import takutility.dubdb.service.TraktResults

private val avengers: TraktResults = TraktResults(listOf(
        newResult {
            type = "movie"
            score = 1000.0
            movie = newMovie {
                title = "Avengers: Endgame"
                year = 2019
                ids = newMovieIds {
                    slug = "avengers-endgame-2019"
                    trakt = 191798
                    imdb = "tt4154796"
                    tmdb = 299534
                }
            }
        },
        newResult {
            type = "episode"
            show = newShow {
                ids = newShowIds {
                    slug = "movies-are-not-allowed-2019"
                    tvdb = 356659
                    trakt = 141565
                    imdb = "https://m.imdb.com/list/l"
                }
            }
        }
    )
)

private val disincanto = TraktResults(listOf(newResult {
    type = "show"
    score = 1000.0
    show = newShow {
        title = "Disenchantment"
        year = 2018
        ids = newShowIds {
            slug = "disenchantment"
            tvdb = 340234
            trakt = 126558
            imdb = "tt5363918"
            tmdb = 73021
        }
    }
}))

internal class UpdateMovieTest {
    lateinit var trakt: Trakt
    lateinit var task: UpdateMovie

    @BeforeEach
    fun setup() {
        trakt = mock {
            on { searchImdb("tt4154796") } doReturn avengers
            on { searchTrakt("191798") } doReturn avengers
            on { searchImdb("tt5363918") } doReturn disincanto
            on { searchTrakt("126558") } doReturn disincanto
        }
        task = UpdateMovie(trakt)
    }

    @Test
    fun movieYearFromImdb() {
        val movie = Movie(name = "Avengers: Endgame", ids = SourceIds.of(IMDB to "tt4154796"))
        task.run(movie)

        assertEquals(2019, movie.year)
    }

    @Test
    fun movieYearFromTrakt() {
        val movie = Movie(name = "Avengers: Endgame", ids = SourceIds.of(TRAKT to "191798"))
        task.run(movie)

        assertEquals(2019, movie.year)
    }

    @Test
    fun movieTypeFromImdb() {
        val movie = Movie(name = "Avengers: Endgame", ids = SourceIds.of(IMDB to "tt4154796"))
        task.run(movie)

        assertEquals(MovieType.MOVIE, movie.type)
    }

    @Test
    fun movieTypeFromTrakt() {
        val movie = Movie(name = "Avengers: Endgame", ids = SourceIds.of(TRAKT to "191798"))
        task.run(movie)

        assertEquals(MovieType.MOVIE, movie.type)
    }

    @Test
    fun seriesTypeFromImdb() {
        val movie = Movie(name = "Disincanto", ids = SourceIds.of(IMDB to "tt5363918"))
        task.run(movie)

        assertEquals(MovieType.SERIES, movie.type)
    }

    @Test
    fun seriesTypeFromTrakt() {
        val movie = Movie(name = "Disincanto", ids = SourceIds.of(TRAKT to "126558"))
        task.run(movie)

        assertEquals(MovieType.SERIES, movie.type)
    }

    @Test
    fun seriesYearFromImdb() {
        val movie = Movie(name = "Disincanto", ids = SourceIds.of(IMDB to "tt5363918"))
        task.run(movie)

        assertEquals(2018, movie.year)
    }

    @Test
    fun seriesYearFromTrakt() {
        val movie = Movie(name = "Disincanto", ids = SourceIds.of(TRAKT to "126558"))
        task.run(movie)

        assertEquals(2018, movie.year)
    }

    @Test
    fun idsFromImdb() {
        val movie = Movie(name = "Avengers: Endgame", ids = SourceIds.of(IMDB to "tt4154796"))
        task.run(movie)

        assertIds(movie, IMDB to "tt4154796", TRAKT to "191798")
    }

    @Test
    fun idsFromTrakt() {
        val movie = Movie(name = "Avengers: Endgame", ids = SourceIds.of(TRAKT to "191798"))
        task.run(movie)

        assertIds(movie, IMDB to null, TRAKT to "191798")
    }

}

fun assertIds(movie: Movie, vararg ids: Pair<Source, String?>) {
    ids.forEach {
        assertEquals(it.second, movie.ids[it.first]?.id, it.first.name)
    }
    assertEquals(ids.filter { it.second != null }.size, movie.ids.size, "ids")
}