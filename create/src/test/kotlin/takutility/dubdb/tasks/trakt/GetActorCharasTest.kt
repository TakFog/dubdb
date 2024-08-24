package takutility.dubdb.tasks.trakt

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import takutility.dubdb.entities.*
import takutility.dubdb.service.CreditResults
import takutility.dubdb.tasks.TaskResult

internal abstract class GetActorCharasBaseTest {
    lateinit var task: GetActorCharas

    @Test
    fun gillanManicone() {
        val actor = actor("Karen Gillan", 434958)
        val movies = listOf(
            series("Doctor Who", 56872),
            movie("Oculus", 102273),
            movie("Guardians of the Galaxy", 82405),
            series("Selfie", 60354),
            movie("The Big Short", 199563),
            movie("In a Valley of Violence", 188193),
            movie("Guardians of the Galaxy Vol. 2", 181256),
            movie("The Call of the Wild", 329002),
            series("What If...?", 146522),
        )
        val result = task.run(actor, movies)

        result.dubbedEntities?.forEach {
            assertNotNull(it.actor, "actor in ${it.name} from ${it.movie.name}")
            assertEquals(actor.ids, it.actor!!.ids, "actor ids in ${it.name} from ${it.movie.name}")
        }

        assertChara(result, "Self", "Doctor Who", 56872, "tt0436992")
        assertChara(result, "Amy Pond", "Doctor Who", 56872, "tt0436992")
        assertChara(result, "Soothsayer", "Doctor Who", 56872, "tt0436992")
        assertChara(result, "Kaylie Russell", "Oculus", 102273, "tt2388715")
        assertChara(result, "Nebula", "Guardians of the Galaxy", 82405, "tt2015381")
        assertChara(result, "Eliza Dooley", "Selfie", 60354, "tt3549044")
        assertChara(result, "Evie", "The Big Short", 199563, "tt1596363")
        assertChara(result, "Ellen", "In a Valley of Violence", 188193, "tt3608930")
        assertChara(result, "Nebula", "Guardians of the Galaxy Vol. 2", 181256, "tt3896198")
        assertChara(result, "Mercedes", "The Call of the Wild", 329002, "tt7504726")
        assertChara(result, "Nebula (voice)", "What If...?", 146522, "tt10168312")

        assertMissingChara(result, "Calls")
        assertMissingChara(result, "Jumanji: The Next Level")
        assertMissingChara(result, "Stuber")
        assertMissingChara(result, "Spies in Disguise")
    }


    @Test
    fun gillanDonati() {
        val actor = actor("Karen Gillan", 434958)
        val movies = listOf(movie("Jumanji: The Next Level", 360095))
        val result = task.run(actor, movies)

        result.dubbedEntities?.forEach {
            assertNotNull(it.actor, "actor in ${it.name} from ${it.movie.name}")
            assertEquals(actor.ids, it.actor!!.ids, "actor ids in ${it.name} from ${it.movie.name}")
        }

        assertChara(result, "Ruby Roundhouse", "Jumanji: The Next Level", 360095, "tt7975244")

        assertMissingChara(result,"Doctor Who")
        assertMissingChara(result,"Oculus")
        assertMissingChara(result,"Guardians of the Galaxy")
        assertMissingChara(result,"Selfie")
        assertMissingChara(result,"The Big Short")
        assertMissingChara(result,"In a Valley of Violence")
        assertMissingChara(result,"Guardians of the Galaxy Vol. 2")
        assertMissingChara(result,"The Call of the Wild")
        assertMissingChara(result,"What If...?")
        assertMissingChara(result, "Calls")
        assertMissingChara(result, "Stuber")
        assertMissingChara(result, "Spies in Disguise")
    }
}

internal class GetActorCharasTest: GetActorCharasBaseTest() {

    @BeforeEach
    fun setup() {
        val trakt = mock<TraktMock> {
            on { personCredits(434958) } doReturn CreditResults(gillanMovies, gillanShows)
        }
        task = GetActorCharas(trakt)
    }
}

@Disabled
internal class GetActorCharasIntegrationTest: GetActorCharasBaseTest() {

    @BeforeEach
    fun setup() {
        task = GetActorCharas(traktImpl)
    }
}


private fun assertChara(result: TaskResult, charaName: String, movieName: String, movieTrakt: Int, movieImdb: String? = null) {
    assertNotNull(result.dubbedEntities, "charas")
    assertFalse(result.dubbedEntities!!.isEmpty(), "empty charas")
    val chara = try {
         result.dubbedEntities!!.first { it.name == charaName && it.movie.name == movieName }
    } catch (e: NoSuchElementException) {
        fail("$charaName not found")
    }

    assertEquals(1, chara.sources.size)
    chara.sources[0].apply {
        assertEquals(DataSource.TRAKT, dataSource, "dataSource")
        assertEquals(Source.TRAKT, sourceId.source, "sourceId")
        assertEquals(chara.actor?.ids?.get(Source.TRAKT), sourceId, "sourceId")
        assertEquals(charaName, raw, "raw source")
    }

    assertEquals(movieTrakt, chara.movie.traktId)
    if (movieImdb != null)
        assertEquals(movieImdb, chara.movie.ids[Source.IMDB]?.id)
}

private fun assertMissingChara(result: TaskResult, movieName: String) {
    assertNotNull(result.dubbedEntities, "charas")
    assertFalse(result.dubbedEntities!!.isEmpty(), "empty charas")
    assertEquals(0, result.dubbedEntities!!.filter { it.movie.name == movieName }.size, movieName)
}

private fun actor(name: String, trakt: Int) = ActorRefImpl(name, ids = SourceIds.of(Source.TRAKT to trakt.toString()))
private fun movie(name: String, trakt: Int) = Movie(name = name, type = MovieType.MOVIE, ids = SourceIds.of(Source.TRAKT to trakt.toString()))
private fun series(name: String, trakt: Int) = Movie(name = name, type = MovieType.SERIES, ids = SourceIds.of(Source.TRAKT to trakt.toString()))

private val gillanMovies = listOf(
    newCast(listOf("Kaylie Russell"), movie = newMovie {
        title = "Oculus"
        ids = newMovieIds(102273, "tt2388715")
    }),
    newCast(listOf("Nebula"), movie = newMovie {
        title = "Guardians of the Galaxy"
        ids = newMovieIds(82405, "tt2015381")
    }),
    newCast(listOf("Evie"), movie = newMovie {
        title = "The Big Short"
        ids = newMovieIds(199563, "tt1596363")
    }),
    newCast(listOf("Ellen"), movie = newMovie {
        title = "In a Valley of Violence"
        ids = newMovieIds(188193, "tt3608930")
    }),
    newCast(listOf("Nebula"), movie = newMovie {
        title = "Guardians of the Galaxy Vol. 2"
        ids = newMovieIds(181256, "tt3896198")
    }),
    newCast(listOf("Mercedes"), movie = newMovie {
        title = "The Call of the Wild"
        ids = newMovieIds(329002, "tt7504726")
    }),
    newCast(listOf("Ruby Roundhouse"), movie = newMovie {
        title = "Jumanji: The Next Level"
        ids = newMovieIds(360095, "tt7975244")
    }),
    newCast(listOf("Morris"), movie = newMovie {
        title = "Stuber"
        ids = newMovieIds(360637, "tt7734218")
    }),
    newCast(listOf("Eyes (voice)"), movie = newMovie {
        title = "Spies in Disguise"
        ids = newMovieIds(277572, "tt5814534")
    }),
)

private val gillanShows = listOf(
    newCast(listOf("Self", "Amy Pond", "Soothsayer"), show = newShow {
        title = "Doctor Who"
        ids = newShowIds(56872, "tt0436992")
    }),
    newCast(listOf("Eliza Dooley"), show = newShow {
        title = "Selfie"
        ids = newShowIds(60354, "tt3549044")
    }),
    newCast(listOf("Nebula (voice)"), show = newShow {
        title = "What If...?"
        ids = newShowIds(146522, "tt10168312")
    }),
    newCast(listOf("Sara (voice)"), show = newShow {
        title = "Calls"
        ids = newShowIds(174645, "tt9327706")
    }),
)

