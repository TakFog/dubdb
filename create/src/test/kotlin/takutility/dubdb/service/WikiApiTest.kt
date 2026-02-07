package takutility.dubdb.service

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

@Disabled
internal class WikiApiTest {

    @Test
    fun dubbersFromCat() {
        val api = WikiApiImpl()
        val result = api.dubbersFromCat(10)

        assertNotNull(result)
        val value = result.queryValue()
        assertEquals(10, value.size)
        for (i in value.indices) {
            if (i == 0) continue
            val dub = value[i]
            val prev = value[i-1].timestamp
            assertFalse(dub.timestamp > prev, "$i: ${dub.timestamp} > $prev")
        }
    }

    @Test
    fun info() {
        val api = WikiApiImpl()
        val result = api.info("Gabriele_Patriarca_(doppiatore)")
        assertEquals("Gabriele Patriarca (doppiatore)", result.title)
        assertEquals(1842818, result.id)
        assertNotNull(result.lastEdit)
        assertNull(result.content)
    }

    @Test
    fun info_notExists() {
        val api = WikiApiImpl()
        val result = api.info("Title of a page that doesn't exist")
        assertNull(result.id)
        assertNull(result.lastEdit)
        assertNull(result.content)
    }
}