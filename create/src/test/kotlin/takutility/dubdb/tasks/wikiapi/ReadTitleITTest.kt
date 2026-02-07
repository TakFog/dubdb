package takutility.dubdb.tasks.wikiapi

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import takutility.dubdb.TestContext
import takutility.dubdb.entities.EntityRefImpl
import takutility.dubdb.entities.Source
import takutility.dubdb.entities.SourceIds
import takutility.dubdb.service.WikiApiImpl

@Disabled("Wiki API test")
internal class ReadTitleITTest {
    lateinit var task: ReadTitle

    @BeforeEach
    fun before() {
        val context = TestContext.mocked { it.wikiApi = WikiApiImpl() }
        task = ReadTitle(context)
    }

    fun run(name: String) = task.run(EntityRefImpl(ids = SourceIds.of(Source.WIKI to name))).string

    @Test
    fun avengers() {
        val res = run("Avengers:_Age_of_Ultron")
        assertEquals("Avengers: Age of Ultron", res)
    }

    @Test
    fun angeloMaggi() {
        val res = run("Angelo_Maggi")
        assertEquals("Angelo Maggi", res)
    }

    @Test
    fun robertDowneyJr() {
        val res = run("Robert_Downey_Jr.")
        assertEquals("Robert Downey Jr.", res)
    }

    @Test
    fun gabrielePatriarca() {
        val res = run("Gabriele_Patriarca_(doppiatore)")
        assertEquals("Gabriele Patriarca", res)
    }

    @Test
    fun joker2_encoded() {
        val res = run("Joker:_Folie_%C3%A0_Deux")
        assertNull(res)
    }

    @Test
    fun joker2() {
        val res = run("Joker:_Folie_à_Deux")
        assertEquals("Joker: Folie à Deux", res)
    }

    @Test
    fun goodDoctor() {
        val res = run("The_Good_Doctor_(serie_televisiva)")
        assertEquals("The Good Doctor", res)
    }

    @Test
    fun giorni500() {
        val res = run("(500)_giorni_insieme")
        assertEquals("(500) giorni insieme", res)
    }

    @Test
    fun missingPage() {
        val res = run("Title of a page that doesn't exist")
        assertNull(res)
    }

}