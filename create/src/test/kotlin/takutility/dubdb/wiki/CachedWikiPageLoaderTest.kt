package takutility.dubdb.wiki

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.Mockito.mock
import org.mockito.kotlin.*
import org.mockito.stubbing.OngoingStubbing
import takutility.dubdb.service.wikiapi.Parse
import takutility.dubdb.service.wikiapi.ParseResponse
import takutility.dubdb.service.wikiapi.WikiApi
import java.io.File
import java.nio.file.Path
import java.time.temporal.ChronoUnit

class CachedWikiPageLoaderTest {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var mockApi: WikiApi
    private lateinit var cachedloader: CachedWikiPageLoader
    
    @BeforeEach
    fun setUp() {
        mockApi = mock()
        cachedloader = CachedWikiPageLoader(mockApi, tempDir.toFile())
    }

    @Test
    fun `page is cached after first call`() {
        val title = "Test Page"
        val parse = Parse(pageid = 123L, title = title, revid = 456L)

        // Mock the delegate to return info
        whenever(mockApi.parse(eq(title), isNull(), parseProp(), isNull())).thenReturn(parse.toResp())
        
        // First call should hit the delegate
        val result1 = cachedloader.page(title)
        assertEquals(title, result1.title)
        assertEquals(parse.pageid, result1.id)
        assertEquals(parse.revid, result1.revisionId)
        assertTrue(result1.exists())
        assertNotNull(result1.lastRead)

        // Second call should use cache
        val result2 = cachedloader.page(title)
        assertEquals(title, result1.title)
        assertEquals(parse.pageid, result1.id)
        assertEquals(parse.revid, result1.revisionId)
        assertTrue(result2.exists())
        assertEquals(result1.lastRead?.truncatedTo(ChronoUnit.SECONDS),
            result2.lastRead?.truncatedTo(ChronoUnit.SECONDS))

        verify(mockApi, times(1)).parse(eq(title), isNull(), parseProp(), isNull())

        // Verify cache file was created
        val cacheFiles = tempDir.toFile().listFiles()
        assertNotNull(cacheFiles)
        assertEquals(1, cacheFiles!!.size)
        assertEquals("Test_Page.json", cacheFiles[0].name)
    }

    @Test
    fun `page uses hashed filename for invalid characters`() {
        val title = "Test/Page?" // invalid characters / and ?
        val parse = Parse(pageid = 123L, title = title, revid = 456L)

        whenever(mockApi.parse(eq(title), isNull(), parseProp(), isNull())).thenReturn(parse.toResp())
        
        val result1 = cachedloader.page(title)
        
        // Trigger saveCache by reading a property
        assertTrue(result1.exists())

        // Verify cache file was created with hashed name
        val cacheFiles = tempDir.toFile().listFiles()
        assertNotNull(cacheFiles)
        assertEquals(1, cacheFiles!!.size)
        
        val file = cacheFiles[0]
        assertTrue(file.name.startsWith("Test_Page__")) // Contains underscores for invalid chars
        assertTrue(file.name.endsWith(".json"))
        assertNotEquals("Test_Page_.json", file.name)
        
        // Second call should read from cache
        val result2 = cachedloader.page(title)
        assertTrue(result2.exists())
        assertEquals(123L, result2.id)
        
        // Parse should only be called once
        verify(mockApi, times(1)).parse(eq(title), isNull(), parseProp(), isNull())
    }

    @Test
    fun `page loads from existing valid cache without api call`() {
        val title = "Cached Page"
        // Setup cache file manually
        val fileContent = """
            {
              "title": "Cached Page",
              "id": 999,
              "revisionId": 888,
              "exists": true,
              "lastRead": "2024-01-01T12:00:00Z"
            }
        """.trimIndent()
        File(tempDir.toFile(), "Cached_Page.json").writeText(fileContent)
        
        val result = cachedloader.page(title)
        
        // Should have values from cache
        assertEquals("Cached Page", result.title)
        assertTrue(result.exists())
        assertEquals(999L, result.id)
        assertEquals(888L, result.revisionId)
        
        // Should not have called api
        verify(mockApi, never()).parse(any(), anyOrNull(), anyOrNull(), anyOrNull())
    }

    @Test
    fun `page ignores corrupted cache and falls back to api call`() {
        val title = "Corrupted Page"
        // Setup corrupted cache file manually
        val fileContent = """
            { "title": "Corrupted Page", "id": "mismatched type" 
        """.trimIndent()
        File(tempDir.toFile(), "Corrupted_Page.json").writeText(fileContent)
        
        val parse = Parse(pageid = 111L, title = title, revid = 222L)
        whenever(mockApi.parse(eq(title), isNull(), parseProp(), isNull())).thenReturn(parse.toResp())
        
        val result = cachedloader.page(title)
        
        // Check properties load by triggering api
        assertTrue(result.exists())
        assertEquals(111L, result.id)
        assertEquals(222L, result.revisionId)
        
        // Verify api was called despite cache existing
        verify(mockApi, times(1)).parse(eq(title), isNull(), parseProp(), isNull())
    }

    @Test
    fun `non existing pages are cached`() {
        val title = "Missing Page"
        val errorObj = mapOf(
            "code" to "missingtitle",
            "info" to "The page you specified doesn't exist.",
            "docref" to "See https://it.wikipedia.org/w/api.php for API usage."
        )

        whenever(mockApi.parse(eq(title), isNull(), parseProp(), isNull())).thenReturn(ParseResponse(parse = null, error = errorObj))
        
        val result1 = cachedloader.page(title)
        
        assertFalse(result1.exists())
        assertNull(result1.id)
        assertNotNull(result1.lastRead)
        
        // Ensure cache is written
        val cacheFiles = tempDir.toFile().listFiles()
        assertNotNull(cacheFiles)
        assertEquals(1, cacheFiles!!.size)
        assertEquals("Missing_Page.json", cacheFiles[0].name) // Missing_Page does not contain invalid chars so no hash
        
        // Second call should use cache
        val result2 = cachedloader.page(title)
        assertFalse(result2.exists())
        assertNull(result2.id)
        assertEquals(result1.lastRead, result2.lastRead)
        
        verify(mockApi, once()).parse(eq(title), isNull(), parseProp(), isNull())
    }

    @Test
    fun `page with spaces or underscores resolve to the same cache file`() {
        val titleWithSpace = "Test Page?"
        val titleWithUnderscore = "Test_Page?"
        
        val parse = Parse(pageid = 123L, title = titleWithSpace, revid = 456L)

        // Only setup the first one
        whenever(mockApi.parse(eq(titleWithSpace), isNull(), parseProp(), isNull())).thenReturn(parse.toResp())
        
        val resultCached = cachedloader.page(titleWithSpace)
        assertTrue(resultCached.exists())

        val result1 = cachedloader.page(titleWithSpace)
        assertTrue(result1.exists())
        assertEquals(resultCached.id, result1.id)
        assertEquals(resultCached.lastRead, result1.lastRead)

        // Now try to load the page with underscore, it should load from the SAME cache file
        // since spaces and underscores resolve to the same hash base.
        val result2 = cachedloader.page(titleWithUnderscore)
        assertTrue(result2.exists())
        assertEquals(resultCached.id, result2.id)
        assertEquals(resultCached.lastRead, result2.lastRead)

        // API should only have been called ONCE for the first title, never for the second
        verify(mockApi, once()).parse(eq(titleWithSpace), isNull(), parseProp(), isNull())
        verify(mockApi, never()).parse(eq(titleWithUnderscore), isNull(), parseProp(), isNull())
        
        // Ensure only one file exists
        val cacheFiles = tempDir.toFile().listFiles()
        assertNotNull(cacheFiles)
        assertEquals(1, cacheFiles!!.size)
    }

    private fun whenParse(title: String? = null, revid: Long? = null, prop: String? = null, section: Int? = null): OngoingStubbing<ParseResponse?> {
        return whenever(mockApi.parse(
            title ?: isNull(), revid ?: isNull(),
            prop ?: isNull(), section ?: isNull()
        ))
    }
    private fun parseProp() = eq("tocdata|properties|langlinks|revid")

    private fun Parse.toResp() = ParseResponse(this)

}

private fun once() = times(1)