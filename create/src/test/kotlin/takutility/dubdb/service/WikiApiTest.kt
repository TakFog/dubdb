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

        assertEquals(10, result.size)
        for (i in result.indices) {
            val dub = result[i]
            assertNotNull(dub.wikiId, "wikiId $i")
            assertNotNull(dub.lastUpdate, "update $i")
            if (i > 0) {
                val prev = result[i-1].lastUpdate
                assertFalse(dub.lastUpdate!! > result[i - 1].lastUpdate, "$i: ${dub.lastUpdate} > $prev")
            }
        }
    }
}