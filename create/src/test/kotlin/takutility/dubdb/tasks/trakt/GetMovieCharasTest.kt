package takutility.dubdb.tasks.trakt

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import takutility.dubdb.TestContext
import takutility.dubdb.entities.*
import takutility.dubdb.service.CreditResults
import takutility.dubdb.tasks.TaskResult

internal abstract class GetMovieCharasBaseTest {
    lateinit var task: GetMovieCharas

    @Test
    fun ultron() {
        val movie = movieRefOf("Avengers: Age of Ultron", ids = SourceIds.of(Source.TRAKT to "71938"), type = MovieType.MOVIE)

        val result = task.run(movie)

        assertChara(result, "Tony Stark", "Robert Downey Jr.", 15987, "nm0000375")
        assertChara(result, "Iron Man", "Robert Downey Jr.", 15987, "nm0000375")
        assertChara(result, "Thor", "Chris Hemsworth", 425150, "nm1165110")
        assertChara(result, "Bruce Banner", "Mark Ruffalo", 406, "nm0749263")
        assertChara(result, "Hulk", "Mark Ruffalo", 406, "nm0749263")
        assertChara(result, "Ultron (voice)", "James Spader", 414968, "nm0000652")
        assertChara(result, "Jarvis", "Paul Bettany", 16158, "nm0079273")
        assertChara(result, "Dr. Cho's Assistant", "Chan Woo Lim", 809168)
        assertChara(result, "Dr. Cho's Assistant", "Minhee Yeo", 809169)
    }

    @Test
    fun reginaScacchi() {
        val movie = movieRefOf("La regina degli scacchi", ids = SourceIds.of(Source.TRAKT to "165792"), type = MovieType.SERIES)

        val result = task.run(movie)

        assertNotNull(result.dubbedEntities)
        val entities = result.dubbedEntities!!
        assertEquals(1, entities.count { it.name.startsWith("Methuen Orphanage Girl") }, "'Methuen Orphanage Girl' count")
        assertTrue(entities.none { it.name == "Methuen Orphanage Girl (voice" }, "'Methuen Orphanage Girl (voice' not present")
        assertTrue(entities.none { it.name == "uncredited)" }, "'uncredited)' not present")
        assertChara(result, "Methuen Orphanage Girl (voice, uncredited)", "Kyndra Sanchez", 2036629, "nm10421806")
    }

}

internal class GetMovieCharasTest: GetMovieCharasBaseTest() {

    @BeforeEach
    fun setup() {
        val trakt = mockTrakt {
            on { movieCredits(71938) } doReturn CreditResults(ultron, listOf())
            on { showCredits(any()) } doReturn CreditResults(listOf(), listOf())
            on { showCredits(165792) } doReturn CreditResults(listOf(), queens)
        }
        task = GetMovieCharas(TestContext.mocked { it.trakt = trakt })
    }
}

@Disabled
internal class GetMovieCharasIntegrationTest: GetMovieCharasBaseTest() {

    @BeforeEach
    fun setup() {
        task = GetMovieCharas(TestContext.mocked { it.trakt = traktImpl })
    }
}

private fun assertChara(result: TaskResult, charaName: String, actorName: String, actorTrakt: Int, actorImdb: String? = null) {
    assertNotNull(result.dubbedEntities, "charas")
    assertFalse(result.dubbedEntities!!.isEmpty(), "empty charas")
    val chara = try {
        result.dubbedEntities!!.first { it.name == charaName && it.actor?.name == actorName }
    } catch (e: NoSuchElementException) {
        fail("$charaName not found")
    }
    assertEquals(1, chara.sources.size)
    chara.sources[0].apply {
        assertEquals(DataSource.TRAKT_MOVIE, dataSource, "dataSource")
        assertEquals(Source.TRAKT, sourceId.source, "sourceId")
        assertEquals(chara.movie.ids[Source.TRAKT], sourceId, "sourceId")
        assertEquals(charaName, raw, "raw source")
    }

    assertEquals(actorTrakt, chara.actor?.traktId)
    assertEquals(actorImdb, chara.actor?.ids?.get(Source.IMDB)?.id)
}

private val ultron = listOf(
    newCast(listOf("Tony Stark","Iron Man"), person = newPerson {
        name = "Robert Downey Jr."
        ids = newPersonIds(15987, "nm0000375")
    }),
    newCast(listOf("Thor"), person = newPerson {
        name = "Chris Hemsworth"
        ids = newPersonIds(425150, "nm1165110")
    }),
    newCast(listOf("Bruce Banner", "Hulk"), person = newPerson {
        name = "Mark Ruffalo"
        ids = newPersonIds(406, "nm0749263")
    }),
    newCast(listOf("Ultron (voice)"), person = newPerson {
        name = "James Spader"
        ids = newPersonIds(414968, "nm0000652")
    }),
    newCast(listOf("Jarvis", "Vision"), person = newPerson {
        name = "Paul Bettany"
        ids = newPersonIds(16158, "nm0079273")
    }),
    newCast(listOf("Dr. Cho's Assistant"), person = newPerson {
        name = "Chan Woo Lim"
        ids = newPersonIds(809168)
    }),
    newCast(listOf("Dr. Cho's Assistant"), person = newPerson {
        name = "Minhee Yeo"
        ids = newPersonIds(809169)
    }),
)

private val queens = listOf(
    newCast(listOf("Methuen Orphanage Girl (voice","uncredited)"), person = newPerson {
        name = "Kyndra Sanchez"
        ids = newPersonIds(2036629, "nm10421806")
    }),
)