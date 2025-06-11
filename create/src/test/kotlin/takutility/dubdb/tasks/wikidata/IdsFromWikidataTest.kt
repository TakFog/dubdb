package takutility.dubdb.tasks.wikidata

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import takutility.dubdb.TestContext
import takutility.dubdb.entities.*
import takutility.dubdb.entities.Source.*
import takutility.dubdb.service.Wikidata
import takutility.dubdb.service.WikidataImpl

internal abstract class IdsFromWikidataBaseTest {
    lateinit var task: IdsFromWikidata

    private fun run(vararg entities: EntityRef) = task.run(listOf(*entities))

    protected abstract fun doMock(action: (Wikidata) -> Unit)
    private fun doMock(vararg values: Pair<String, Array<Pair<Source, String>>>) = doMock {wd ->
        val result = values.associate { it.first to SourceIds.of(*it.second) }
        whenever(wd.findIds(result.keys.toList())).thenReturn(result)
    }

    @Test
    fun empty() {
        val result = task.run(listOf())
        assertTrue(result.isEmpty())
    }

    @Test
    fun notFound() {
        val result = run(actor(WIKIDATA to "Q713324"))
        assertTrue(result.isEmpty())
    }

    @Test
    fun downeyJr() {
        val wikidata = WIKIDATA to "Q165219"
        val downeyIds = arrayOf(
            WIKI to "Robert_Downey_Jr.",
            WIKI_EN to "Robert_Downey_Jr.",
            IMDB to "nm0000375",
        )
        doMock(wikidata.second to downeyIds)

        val result = run(actor(wikidata))

        assertEquals(1, result.actors?.size)
        assertNull(result.dubbers)
        assertNull(result.movies)

        assertEquals(SourceIds.of(wikidata, *downeyIds), result.actor?.ids)
    }

    @Test
    fun downeyJr_doubleId() {
        val wikidata = "Q165219"
        val input = arrayOf(WIKIDATA to wikidata, TRAKT to "15987")
        val output = arrayOf(
            WIKI to "Robert_Downey_Jr.",
            WIKI_EN to "Robert_Downey_Jr.",
            IMDB to "nm0000375",
        )
        doMock(wikidata to output)

        val result = run(actor(*input))

        assertEquals(1, result.actors?.size)
        assertNull(result.dubbers)
        assertNull(result.movies)

        assertEquals(SourceIds.of(*input, *output), result.actor?.ids)
    }

    @Test
    fun downeyJr_repeated() {
        val wikidata = "Q165219"
        val imdb = IMDB to "nm0000375"
        val input = arrayOf(WIKIDATA to wikidata, TRAKT to "15987", imdb)
        val output = arrayOf(
            WIKI to "Robert_Downey_Jr.",
            WIKI_EN to "Robert_Downey_Jr.",
            imdb,
        )
        doMock(wikidata to output)

        val result = run(actor(*input))

        assertEquals(1, result.actors?.size)
        assertNull(result.dubbers)
        assertNull(result.movies)

        assertEquals(SourceIds.of(*input, *output), result.actor?.ids)
    }

    @Test
    fun maggi() {
        val wikidata = WIKIDATA to "Q3617056"
        val output = arrayOf(
            WIKI to "Angelo_Maggi",
            WIKI_EN to "Angelo_Maggi",
            IMDB to "nm0535947",
            MONDO_DOPPIATORI to "doppiaggio/voci/vociamag"
        )
        doMock(wikidata.second to output)

        val result = run(dubber(wikidata))

        assertNull(result.actors)
        assertEquals(1, result.dubbers?.size)
        assertNull(result.movies)

        assertEquals(SourceIds.of(wikidata, *output), result.dubber?.ids)
    }

    @Test
    fun patriarca() {
        val wikidata = WIKIDATA to "Q3756660"
        val output = arrayOf(
            WIKI to "Gabriele_Patriarca_(doppiatore)",
            IMDB to "nm0665775",
            MONDO_DOPPIATORI to "doppiaggio/voci/vocigpat"
        )
        doMock(wikidata.second to output)

        val result = run(dubber(wikidata))

        assertNull(result.actors)
        assertEquals(1, result.dubbers?.size)
        assertNull(result.movies)

        assertEquals(SourceIds.of(wikidata, *output), result.dubber?.ids)
    }

    @Test
    fun deadpool2() {
        val wikidata = WIKIDATA to "Q25431158"
        val output = arrayOf(
            WIKI to "Deadpool_2",
            WIKI_EN to "Deadpool_2",
            IMDB to "tt5463162",
            MONDO_DOPPIATORI to "doppiaggio/film1/deadpool2"
        )
        doMock(wikidata.second to output)

        val result = run(movie(wikidata))

        assertNull(result.actors)
        assertNull(result.dubbers)
        assertEquals(1, result.movies?.size)

        assertEquals(SourceIds.of(wikidata, *output), result.movie?.ids)
    }

    @Test
    fun goodDoctor() {
        val wikidata = WIKIDATA to "Q29908604"
        val output = arrayOf(
            WIKI to "The_Good_Doctor_(serie_televisiva)",
            WIKI_EN to "The_Good_Doctor_(American_TV_series)",
            IMDB to "tt6470478",
            MONDO_DOPPIATORI to "doppiaggio/telefilm/thegooddoctor"
        )
        doMock(wikidata.second to output)

        val result = run(movie(wikidata))

        assertNull(result.actors)
        assertNull(result.dubbers)
        assertEquals(1, result.movies?.size)

        assertEquals(SourceIds.of(wikidata, *output), result.movie?.ids)
    }

    @Test
    fun mixed_single() {
        val downeyWD = WIKIDATA to "Q165219"
        val patriarcaWD = WIKIDATA to "Q3756660"
        val doctorWD = WIKIDATA to "Q29908604"
        val downeyOut = arrayOf(
            WIKI to "Robert_Downey_Jr.",
            WIKI_EN to "Robert_Downey_Jr.",
            IMDB to "nm0000375",
        )
        val patriarcaOut = arrayOf(
            WIKI to "Gabriele_Patriarca_(doppiatore)",
            IMDB to "nm0665775",
            MONDO_DOPPIATORI to "doppiaggio/voci/vocigpat"
        )
        val doctorOut = arrayOf(
            WIKI to "The_Good_Doctor_(serie_televisiva)",
            WIKI_EN to "The_Good_Doctor_(American_TV_series)",
            IMDB to "tt6470478",
            MONDO_DOPPIATORI to "doppiaggio/telefilm/thegooddoctor"
        )
        doMock {
            val res = mapOf(
                downeyWD.second to SourceIds.of(*downeyOut),
                patriarcaWD.second to SourceIds.of(*patriarcaOut),
                doctorWD.second to SourceIds.of(*doctorOut),
            )
            whenever(it.findIds(res.keys.toList())).thenReturn(res)
        }

        val result = run(actor(downeyWD), dubber(patriarcaWD), movie(doctorWD))

        assertEquals(1, result.actors?.size)
        assertEquals(1, result.dubbers?.size)
        assertEquals(1, result.movies?.size)

        assertEquals(SourceIds.of(downeyWD, *downeyOut), result.actor?.ids)
        assertEquals(SourceIds.of(patriarcaWD, *patriarcaOut), result.dubber?.ids)
        assertEquals(SourceIds.of(doctorWD, *doctorOut), result.movie?.ids)
    }

    @Test
    fun allMixed() {
        val downeyWD = WIKIDATA to "Q165219"
        val maggiWD = WIKIDATA to "Q3617056"
        val patriarcaWD = WIKIDATA to "Q3756660"
        val deadpoolWD = WIKIDATA to "Q25431158"
        val doctorWD = WIKIDATA to "Q29908604"
        val randomWD = WIKIDATA to "Q713324"
        val downeyOut = arrayOf(
            WIKI to "Robert_Downey_Jr.",
            WIKI_EN to "Robert_Downey_Jr.",
            IMDB to "nm0000375",
        )
        val maggiOut = arrayOf(
            WIKI to "Angelo_Maggi",
            WIKI_EN to "Angelo_Maggi",
            IMDB to "nm0535947",
            MONDO_DOPPIATORI to "doppiaggio/voci/vociamag"
        )
        val patriarcaOut = arrayOf(
            WIKI to "Gabriele_Patriarca_(doppiatore)",
            IMDB to "nm0665775",
            MONDO_DOPPIATORI to "doppiaggio/voci/vocigpat"
        )
        val deadpoolOut = arrayOf(
            WIKI to "Deadpool_2",
            WIKI_EN to "Deadpool_2",
            IMDB to "tt5463162",
            MONDO_DOPPIATORI to "doppiaggio/film1/deadpool2"
        )
        val doctorOut = arrayOf(
            WIKI to "The_Good_Doctor_(serie_televisiva)",
            WIKI_EN to "The_Good_Doctor_(American_TV_series)",
            IMDB to "tt6470478",
            MONDO_DOPPIATORI to "doppiaggio/telefilm/thegooddoctor"
        )
        doMock {
            val res = mapOf(
                downeyWD.second to SourceIds.of(*downeyOut),
                maggiWD.second to SourceIds.of(*maggiOut),
                patriarcaWD.second to SourceIds.of(*patriarcaOut),
                deadpoolWD.second to SourceIds.of(*deadpoolOut),
                doctorWD.second to SourceIds.of(*doctorOut),
                randomWD.second to SourceIds(),
            )
            whenever(it.findIds(any())).thenReturn(res)
        }

        val result = run(
            actor(downeyWD),
            dubber(maggiWD),
            movie(deadpoolWD),
            actor(randomWD),
            dubber(patriarcaWD),
            movie(doctorWD)
        )

        assertEquals(1, result.actors?.size)
        assertEquals(2, result.dubbers?.size)
        assertEquals(2, result.movies?.size)

        listOf(
            SourceIds.of(downeyWD, *downeyOut) to result.actors,
            SourceIds.of(maggiWD, *maggiOut) to result.dubbers,
            SourceIds.of(deadpoolWD, *deadpoolOut) to result.movies,
            SourceIds.of(patriarcaWD, *patriarcaOut) to result.dubbers,
            SourceIds.of(doctorWD, *doctorOut) to result.movies,
        ).forEach {
            val expected = it.first
            assertEquals(expected, it.second?.firstOrNull { actual -> actual.ids[WIKIDATA] == expected[WIKIDATA]}?.ids)
        }

        assertEquals(true, result.actors?.none { randomWD.second == it.ids[WIKIDATA]?.id }, "no random")
    }

}

internal class IdsFromWikidataTest: IdsFromWikidataBaseTest() {
    private lateinit var wikidata: Wikidata

    @BeforeEach
    fun setup() {
        wikidata = mock()
        val ctx = TestContext.mocked {
            it.wikidata = wikidata
        }
        task = IdsFromWikidata(ctx)
    }

    override fun doMock(action: (Wikidata) -> Unit) {
        action(wikidata)
    }
}

@Disabled
internal class IdsFromWikidataITTest: IdsFromWikidataBaseTest() {
    val wikidata: Wikidata = WikidataImpl()

    @BeforeEach
    fun setup() {
        val ctx = TestContext.mocked {
            it.wikidata = wikidata
        }
        task = IdsFromWikidata(ctx)
    }

    override fun doMock(action: (Wikidata) -> Unit) {}
}


private fun actor(vararg ids: Pair<Source, String>) = entity(*ids) { ActorRefImpl(ids = it) }
private fun dubber(vararg ids: Pair<Source, String>) = entity(*ids) { DubberRefImpl(ids = it) }
private fun movie(vararg ids: Pair<Source, String>) = entity(*ids) { movieRefOf(ids = it) }
private fun <T: EntityRef> entity(vararg ids: Pair<Source, String?>, ctor: (SourceIds) -> T) = ctor(SourceIds.of(*ids))