package takutility.dubdb.tasks.wikiapi

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import takutility.dubdb.TestContext
import takutility.dubdb.entities.EntityRefImpl
import takutility.dubdb.entities.Source
import takutility.dubdb.entities.SourceIds
import takutility.dubdb.service.wikiapi.Info
import takutility.dubdb.service.wikiapi.WikiApi

internal class ReadTitleTest {
    lateinit var task: ReadTitle
    lateinit var api: WikiApi

    @BeforeEach
    fun before() {
        api= mock()
        val context = TestContext.mocked { it.wikiApi = api }
        task = ReadTitle(context)
    }

    fun run(name: String) = task.run(EntityRefImpl(ids = SourceIds.of(Source.WIKI to name))).string

    fun info(title: String) = Info(title = title, pageid = 1, lastrevid = 541125)

    @Test
    fun gabrielePatriarca() {
        val title = "Gabriele_Patriarca_(doppiatore)"
        whenever(api.info(title)).thenReturn(info("Gabriele Patriarca (doppiatore)"))
        val res = run(title)
        assertEquals("Gabriele Patriarca", res)
    }

    @Test
    fun goodDoctor() {
        val title = "The_Good_Doctor_(serie_televisiva)"
        whenever(api.info(title)).thenReturn(info("The Good Doctor (serie televisiva)"))
        val res = run(title)
        assertEquals("The Good Doctor", res)
    }

    @Test
    fun giorni500() {
        val title = "(500)_giorni_insieme"
        whenever(api.info(title)).thenReturn(info("(500) giorni insieme"))
        val res = run(title)
        assertEquals("(500) giorni insieme", res)
    }

    @Test
    fun missingPage() {
        val title = "Nonexisting_page"
        whenever(api.info(title)).thenReturn(null)
        val res = run(title)
        assertNull(res)
    }

}