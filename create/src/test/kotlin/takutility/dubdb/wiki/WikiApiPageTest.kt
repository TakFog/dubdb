package takutility.dubdb.wiki

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.mockito.stubbing.OngoingStubbing
import takutility.dubdb.service.wikiapi.Parse
import takutility.dubdb.service.wikiapi.ParseResponse
import takutility.dubdb.service.wikiapi.WikiApi
import java.io.File

private const val SENTINEL = "SENTINEL~VALUE"

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
        return ParseResponse(parseObject)
    }

    @BeforeEach
    fun setUp() {
        val metaResponse = loadJson("angelo_maggi_meta.json")
        val mainResponse = loadJson("angelo_maggi_main.json")
        val doppiaggioResponse = loadJson("angelo_maggi_doppiaggio.json")

        // Mock API calls
        // 1. Initial parse for metadata
        api.whenParse(title = pageTitle, prop = "tocdata|properties|langlinks|revid").doReturn(metaResponse)

        // 2. Main section content (index 0)
        // revid is used in loadContent, which comes from the meta response
        val revid = 148840443L
       api.whenParse(revid = revid, section = 0, prop = "wikitext").doReturn(mainResponse)

        // 3. Doppiaggio section content (index 5)
        api.whenParse(revid = revid, section = 5, prop = "wikitext").doReturn(doppiaggioResponse)
    }

    @Test
    fun `null response`() {
        val title = "any title"

        val page = WikiApiPage(api, title)

        assertFalse(page.exists(), "Page exists")
        verify(api).parse(eq(title), isNull(), any(), isNull())
    }

    @Test
    fun `load metadata`() {
        val page = WikiApiPage(api, pageTitle)
        val exists = page.exists()

        verify(api).parse(eq(pageTitle), isNull(), any(), isNull())

        // Verify existence and basic properties
        assertTrue(exists, "exists")
        assertEquals(1334683L, page.id, "page id")
        assertEquals(revid, page.revisionId, "revision id")
        assertEquals("Maggi Mariotti ,Angelo", page.sort, "sort")
        assertEquals("Angelo_Maggi_20240113.jpg", page.image, "image")
        assertEquals("Q3617056", page.wikidata, "wikidata")
        
        // Verify language links
        assertEquals("https://en.wikipedia.org/wiki/Angelo_Maggi", page.langLink?.get("en"), "en link")
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
        assertEnd(ending, doppiaggioContent)

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
        assertEnd(filmEnding, filmContent, "film ending")

        val podcastContent = doppiaggioSection.subsections["Podcast"]?.content
        assertEquals("=== Podcast ===\n* [[Fedez]] in ''[[Muschio selvaggio]]'' (ep. 77)", podcastContent)
    }
}

fun assertEnd(expectedEnding: String, actual: String, message: String? = null) {
    assertEquals(expectedEnding, actual.substring(actual.length - expectedEnding.length), message)
}

fun WikiApi.whenParse(title: String? = null, revid: Long? = null, prop: String? = null, section: Int? = null): OngoingStubbing<ParseResponse?> {
    return whenever(this.parse(
        title = if (title != null) eq(title) else anyOrNull(),
        revid = if (revid != null) eq(revid) else anyOrNull(),
        prop = if (prop != null) eq(prop) else anyOrNull(),
        section = if (section != null) eq(section) else anyOrNull(),
    ))
}