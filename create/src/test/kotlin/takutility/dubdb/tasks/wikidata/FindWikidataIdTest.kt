package takutility.dubdb.tasks.wikidata

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.*
import takutility.dubdb.TestContext
import takutility.dubdb.entities.*
import takutility.dubdb.entities.Source.*
import takutility.dubdb.service.Wikidata
import takutility.dubdb.service.WikidataImpl

internal abstract class FindWikidataIdBaseTest {
    lateinit var task: FindWikidataId

    private fun run(vararg entities: EntityRef) = task.run(listOf(*entities))

    protected abstract fun doMock(action: (Wikidata) -> Unit)
    private fun doMock(id: String, wikidata: String) = doMock {
        whenever(it.findIdsByItWiki(setOf(id))).thenReturn(mapOf(id to wikidata))
        whenever(it.findIdsByImdb(setOf(id))).thenReturn(mapOf(id to wikidata))
    }
    protected abstract fun doVerify(action: (Wikidata) -> Unit)
    private fun doVerify(src: Pair<Source, String>) = doVerify {
        if (src.first == WIKI)
            verify(it).findIdsByItWiki(setOf(src.second))
        else
            verify(it).findIdsByImdb(setOf(src.second))
        verifyNoMoreInteractions(it)
    }

    @Test
    fun empty() {
        val result = task.run(listOf())
        assertTrue(result.isEmpty())
    }

    @Test
    fun notFound() {
        val result = run(actor(IMDB to "xy52123651"))
        assertTrue(result.isEmpty())
    }

    @ParameterizedTest
    @ValueSource(strings = ["https://it.wikipedia.org/wiki/Robert_Downey_Jr.", "https://www.imdb.com/Name?nm0000375"])
    fun downeyJr(url: String) {
        val src = fromUrl(url)
        val wikidata = "Q165219"
        doMock(src.second, wikidata)

        val result = run(actor(src))

        assertEquals(1, result.actors?.size)
        assertNull(result.dubbers)
        assertNull(result.movies)

        assertEquals(SourceIds.of(src, WIKIDATA to wikidata), result.actor?.ids)
        doVerify(src)
    }

    @Test
    fun downeyJr_doubleId() {
        val wiki = WIKI to "Robert_Downey_Jr."
        val imdb = IMDB to "nm0000375"
        val wikidata = "Q165219"
        doMock {
            whenever(it.findIdsByItWiki(setOf(wiki.second))).thenReturn(mapOf(wiki.second to wikidata))
            whenever(it.findIdsByImdb(setOf(imdb.second))).thenReturn(mapOf(imdb.second to wikidata))
        }

        val result = run(actor(wiki, imdb))

        assertEquals(1, result.actors?.size)
        assertNull(result.dubbers)
        assertNull(result.movies)

        assertEquals(SourceIds.of(wiki, imdb, WIKIDATA to wikidata), result.actor?.ids)
        doVerify {
            verify(it).findIdsByItWiki(setOf(wiki.second))
            verifyNoMoreInteractions(it)
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["https://it.wikipedia.org/wiki/Angelo_Maggi", "https://www.imdb.com/Name?nm0535947"])
    fun maggi(url: String) {
        val src = fromUrl(url)
        val wikidata = "Q3617056"
        doMock(src.second, wikidata)

        val result = run(dubber(src))

        assertEquals(1, result.dubbers?.size)
        assertNull(result.actors)
        assertNull(result.movies)

        assertEquals(SourceIds.of(src, WIKIDATA to wikidata), result.dubber?.ids)
        doVerify(src)
    }

    @ParameterizedTest
    @ValueSource(strings = ["https://it.wikipedia.org/wiki/Deadpool_2", "https://www.imdb.com/Name?tt5463162"])
    fun deadpool2(url: String) {
        val src = fromUrl(url)
        val wikidata = "Q25431158"
        doMock(src.second, wikidata)

        val result = run(movie(src))

        assertEquals(1, result.movies?.size)
        assertNull(result.actors)
        assertNull(result.dubbers)

        assertEquals(SourceIds.of(src, WIKIDATA to wikidata), result.movie?.ids)
        doVerify(src)
    }

    @Test
    fun mixed_single_itWiki() {
        val actorSrc = WIKI to "Robert_Downey_Jr."
        val dubberSrc = WIKI to "Angelo_Maggi"
        val movieSrc = WIKI to "Deadpool_2"
        val actorWD = "Q165219"
        val dubberWD = "Q3617056"
        val movieWD = "Q25431158"
        doMock {
            val res = mapOf(
                actorSrc.second to actorWD,
                dubberSrc.second to dubberWD,
                movieSrc.second to movieWD,
            )
            whenever(it.findIdsByItWiki(any())).thenReturn(res)
        }

        val result = run(actor(actorSrc), dubber(dubberSrc), movie(movieSrc))

        assertEquals(1, result.actors?.size)
        assertEquals(1, result.dubbers?.size)
        assertEquals(1, result.movies?.size)

        assertEquals(SourceIds.of(actorSrc, WIKIDATA to actorWD), result.actor?.ids)
        assertEquals(SourceIds.of(dubberSrc, WIKIDATA to dubberWD), result.dubber?.ids)
        assertEquals(SourceIds.of(movieSrc, WIKIDATA to movieWD), result.movie?.ids)
    }

    @Test
    fun mixed_single_imdb() {
        val actorSrc = IMDB to "nm0000375"
        val dubberSrc = IMDB to "nm0535947"
        val movieSrc = IMDB to "tt5463162"
        val actorWD = "Q165219"
        val dubberWD = "Q3617056"
        val movieWD = "Q25431158"
        doMock {
            whenever(it.findIdsByImdb(any())).thenReturn(mapOf(
                actorSrc.second to actorWD,
                dubberSrc.second to dubberWD,
                movieSrc.second to movieWD,
            ))
        }

        val result = run(actor(actorSrc), dubber(dubberSrc), movie(movieSrc))

        assertEquals(1, result.actors?.size)
        assertEquals(1, result.dubbers?.size)
        assertEquals(1, result.movies?.size)

        assertEquals(SourceIds.of(actorSrc, WIKIDATA to actorWD), result.actor?.ids)
        assertEquals(SourceIds.of(dubberSrc, WIKIDATA to dubberWD), result.dubber?.ids)
        assertEquals(SourceIds.of(movieSrc, WIKIDATA to movieWD), result.movie?.ids)
    }

    @Test
    fun all_mixed() {
        val downeyJrSrc = IMDB to "nm0000375"
        val maggiSrc = IMDB to "nm0535947"
        val deadpoolSrc = IMDB to "tt5463162"
        val randomSrc = IMDB to "xy52123651"
        val reynoldsSrc = WIKI to "Ryan_Reynolds"
        val patriarcaSrc = WIKI to "Gabriele_Patriarca_(doppiatore)"
        val doctorSrc = WIKI to "The_Good_Doctor_(serie_televisiva)"
        val downeyJrWD = "Q165219"
        val maggiWD = "Q3617056"
        val deadpoolWD = "Q25431158"
        val reynoldsWD = "Q192682"
        val patriarcaWD = "Q3756660"
        val doctorWD = "Q29908604"
        doMock {
            whenever(it.findIdsByImdb(any())).thenReturn(mapOf(
                downeyJrSrc.second to downeyJrWD,
                maggiSrc.second to maggiWD,
                deadpoolSrc.second to deadpoolWD,
            ))
            whenever(it.findIdsByItWiki(any())).thenReturn(mapOf(
                reynoldsSrc.second to reynoldsWD,
                patriarcaSrc.second to patriarcaWD,
                doctorSrc.second to doctorWD,
            ))
        }

        val result = run(
            actor(downeyJrSrc),
            dubber(maggiSrc),
            movie(deadpoolSrc),
            actor(randomSrc),
            actor(reynoldsSrc),
            dubber(patriarcaSrc),
            movie(doctorSrc)
        )

        assertEquals(2, result.actors?.size)
        assertEquals(2, result.dubbers?.size)
        assertEquals(2, result.movies?.size)

        listOf(
            SourceIds.of(downeyJrSrc, WIKIDATA to downeyJrWD) to result.actors,
            SourceIds.of(maggiSrc, WIKIDATA to maggiWD) to result.dubbers,
            SourceIds.of(deadpoolSrc, WIKIDATA to deadpoolWD) to result.movies,
            SourceIds.of(reynoldsSrc, WIKIDATA to reynoldsWD) to result.actors,
            SourceIds.of(patriarcaSrc, WIKIDATA to patriarcaWD) to result.dubbers,
            SourceIds.of(doctorSrc, WIKIDATA to doctorWD) to result.movies,
        ).forEach {
            val expected = it.first
            assertEquals(expected, it.second?.firstOrNull { actual -> actual.ids[WIKIDATA] == expected[WIKIDATA]}?.ids)
        }

        assertEquals(true, result.actors?.none { randomSrc.second == it.ids[randomSrc.first]?.id }, "no random")
    }

}

internal class FindWikidataIdTest: FindWikidataIdBaseTest() {
    private lateinit var wikidata: Wikidata

    @BeforeEach
    fun setup() {
        wikidata = mock()
        val ctx = TestContext.mocked {
            it.wikidata = wikidata
        }
        task = FindWikidataId(ctx)
    }

    override fun doMock(action: (Wikidata) -> Unit) {
        action(wikidata)
    }

    override fun doVerify(action: (Wikidata) -> Unit) {
        action(wikidata)
    }
}

@Disabled
internal class FindWikidataIdITTest: FindWikidataIdBaseTest() {
    val wikidata: Wikidata = WikidataImpl()

    @BeforeEach
    fun setup() {
        val ctx = TestContext.mocked {
            it.wikidata = wikidata
        }
        task = FindWikidataId(ctx)
    }

    override fun doMock(action: (Wikidata) -> Unit) {}
    override fun doVerify(action: (Wikidata) -> Unit) {}
}

private fun fromUrl(url: String) = SourceId.fromUrl(url).let { it.source to it.id }

private fun actor(vararg ids: Pair<Source, String>) = entity(*ids) { ActorRefImpl(ids = it)}
private fun dubber(vararg ids: Pair<Source, String>) = entity(*ids) { DubberRefImpl(ids = it)}
private fun movie(vararg ids: Pair<Source, String>) = entity(*ids) { movieRefOf(ids = it) }
private fun <T: EntityRef> entity(vararg ids: Pair<Source, String?>, ctor: (SourceIds) -> T) = ctor(SourceIds.of(*ids))