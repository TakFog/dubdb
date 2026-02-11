package takutility.dubdb.wiki

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import takutility.dubdb.service.wikiapi.Parse
import takutility.dubdb.service.wikiapi.ParseResponse
import takutility.dubdb.service.wikiapi.WikiApi
import takutility.dubdb.service.wikiapi.WikiApiResponse
import java.io.File

class WikiApiPageTest {
    private val revid = 148840443L

    private val api: WikiApi = mock()
    private val pageTitle = "Angelo Maggi"
    private val mapper = jacksonObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    private fun loadJson(filename: String): ParseResponse {
        val resource = javaClass.getResource(filename)
            ?: throw IllegalArgumentException("File not found: $filename")
        val json = File(resource.toURI()).readText()
        
        // The JSON files in test resources have a root "parse" object, 
        // but ParseResponse expects WikiApiResponse<Map<String, Parse>> which means {"query": {"someKey": Parse}}
        // We need to extract the "parse" object and wrap it to match the expected return type of api.parse()
        
        val node = mapper.readTree(json)
        val parseNode = node.get("parse")
        val parseObject = mapper.treeToValue(parseNode, Parse::class.java)
        
        // Wrap in the structure expected by WikiApi code: WikiApiResponse(query = mapOf("val" to parseObject))
        // The key in the map doesn't matter as queryValue() takes the first value
        return WikiApiResponse(mapOf("page" to parseObject))
    }

    @BeforeEach
    fun setUp() {
        val metaResponse = loadJson("angelo_maggi_meta.json")
        val mainResponse = loadJson("angelo_maggi_main.json")
        val doppiaggioResponse = loadJson("angelo_maggi_doppiaggio.json")

        // Mock API calls
        // 1. Initial parse for metadata
        whenever(api.parse(title = pageTitle, prop = "sections|properties|langlinks")).doReturn(metaResponse)

        // 2. Main section content (index 0)
        // revid is used in loadContent, which comes from the meta response
        val revid = 148840443L
        whenever(api.parse(revid = revid, section = 0, prop = "wikitext")).doReturn(mainResponse)

        // 3. Doppiaggio section content (index 5)
        whenever(api.parse(revid = revid, section = 5, prop = "wikitext")).doReturn(doppiaggioResponse)
    }

    @Test
    fun `null response`() {
        whenever(api.parse(any(), any(), any(), any())).thenReturn(null)

        val page = WikiApiPage(api, "any title")

        assertFalse(page.exists(), "Page exists")
    }

    @Test
    fun `load metadata`() {
        val page = WikiApiPage(api, pageTitle)

        // Verify existence and basic properties
        assertTrue(page.exists())
        assertEquals(1334683L, page.id)
        assertEquals(revid, page.revisionId)
        assertEquals("Maggi Mariotti ,Angelo", page.sort)
        assertEquals("Angelo_Maggi_20240113.jpg", page.image)
        assertEquals("Q3617056", page.wikidata)
        
        // Verify language links
        assertEquals("https://en.wikipedia.org/wiki/Angelo_Maggi", page.langLink?.get("en"))
    }

    @Test
    fun `load sections`() {
        val page = WikiApiPage(api, pageTitle)

        // Verify sections structure
        // "Doppiaggio" is a top-level section
        val doppiaggioSection = page.sections?.get("Doppiaggio")
        assertNotNull(doppiaggioSection)
        assertEquals("Doppiaggio", doppiaggioSection?.title)
        assertEquals(6870, doppiaggioSection?.offset)

        // Verify subsections of Doppiaggio
        // In the JSON, "Film" (index 6) is a subsection of "Doppiaggio" (index 5)
        // because "Doppiaggio" is level 2, "Film" is level 3.
        assertNotNull(doppiaggioSection?.subsections)
        assertTrue(doppiaggioSection!!.subsections.containsKey("Film"))
        val filmSection = doppiaggioSection.subsections["Film"]
        assertEquals("Film", filmSection?.title)
        assertEquals(6887, filmSection?.offset)

        assertEquals(listOf("Film", "Film d'animazione", "Serie televisive", "Serie animate", "Televisione",
            "Videogiochi", "Podcast"), doppiaggioSection.subsections.keys.toList())
    }

    @Test
    fun `main section content`() {
        val page = WikiApiPage(api, pageTitle)

        assertNotNull(page.mainSection)
        val mainContent = page.mainSection!!.content
        assertNotNull(mainContent)
        assertTrue(mainContent!!.startsWith("{{Bio"))
        assertTrue(mainContent.contains("Maggi Mariotti"))
        assertTrue(mainContent.contains("doppiatore"))
        assertTrue(mainContent.endsWith("}}"))
    }
    @Test
    fun `section content`() {
        val page = WikiApiPage(api, pageTitle)

        val doppiaggioContent = page.sections?.get("Doppiaggio")?.content
        assertNotNull(doppiaggioContent)
        assertTrue(doppiaggioContent!!.startsWith("== Doppiaggio =="))
        assertTrue(doppiaggioContent.contains("=== Film ==="))
        assertTrue(doppiaggioContent.contains("[[Tom Hanks]]"))
        val ending = "=== Podcast ===\n* [[Fedez]] in ''[[Muschio selvaggio]]'' (ep. 77)"
        assertEquals(ending, doppiaggioContent.substring(doppiaggioContent.length - ending.length))

    }

    @Test
    fun `subsection content from parent section`() {
        val page = WikiApiPage(api, pageTitle)

        val doppiaggioSection = page.sections?.get("Doppiaggio")
        val doppiaggioContent = doppiaggioSection?.content
        assertNotNull(doppiaggioContent)

        val filmContent = doppiaggioSection!!.subsections["Film"]?.content
        assertNotNull(filmContent)
        assertFalse(filmContent!!.contains("== Doppiaggio =="))
        assertTrue(filmContent.startsWith("=== Film ==="))
        val filmEnding = "* [[Guy Marchand]] in ''[[Toglimi un dubbio]]''\n\n"
        assertEquals(filmEnding, filmContent.substring(filmContent.length - filmEnding.length), "film ending")

        val podcastContent = doppiaggioSection.subsections["Podcast"]?.content
        assertEquals("=== Podcast ===\n* [[Fedez]] in ''[[Muschio selvaggio]]'' (ep. 77)", podcastContent)
    }
}
