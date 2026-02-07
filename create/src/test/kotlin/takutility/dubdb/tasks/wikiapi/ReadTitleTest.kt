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
import takutility.dubdb.service.WikiApi
import takutility.dubdb.wiki.WikiPage

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

    fun page(title: String) = WikiPage(title, id = 1)

    @Test
    fun gabrielePatriarca() {
        val title = "Gabriele_Patriarca_(doppiatore)"
        whenever(api.info(title)).thenReturn(page("Gabriele Patriarca (doppiatore)"))
        val res = run(title)
        assertEquals("Gabriele Patriarca", res)
    }

    @Test
    fun goodDoctor() {
        val title = "The_Good_Doctor_(serie_televisiva)"
        whenever(api.info(title)).thenReturn(page("The Good Doctor (serie televisiva)"))
        val res = run(title)
        assertEquals("The Good Doctor", res)
    }

    @Test
    fun giorni500() {
        val title = "(500)_giorni_insieme"
        whenever(api.info(title)).thenReturn(page("(500) giorni insieme"))
        val res = run(title)
        assertEquals("(500) giorni insieme", res)
    }

    @Test
    fun missingPage() {
        val title = "Nonexisting_page"
        whenever(api.info(title)).thenReturn(WikiPage(title))
        val res = run(title)
        assertNull(res)
    }

}