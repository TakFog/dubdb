package takutility.dubdb.tasks.wikiapi

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import takutility.dubdb.service.CategoryMember
import takutility.dubdb.service.CategoryMemberResponse
import takutility.dubdb.service.WikiApi
import java.time.Instant
import java.time.LocalDate

internal class DubbersFromCategoryTest {

    @Test
    fun run() {
        val api: WikiApi = mock {
            on { dubbersFromCat(any()) }.doReturn(CategoryMemberResponse(mapOf("categorymembers" to listOf(
                CategoryMember(295471, "Neri Marcorè", Instant.parse("2024-10-01T23:20:46Z")),
                CategoryMember(2406906, "Piero Tiberi", Instant.parse("2024-09-27T12:44:48Z")),
                CategoryMember(1224733, "Riccardo Garrone (attore)", Instant.parse("2024-07-18T19:01:25Z")),
            ))))
        }

        val result = DubbersFromCategory(api, 3).run()

        verify(api).dubbersFromCat(3)
        assertEquals(3, result.size)
        result[0].apply {
            assertEquals("Neri Marcorè", name)
            assertEquals("Neri_Marcorè", wikiId)
            assertEquals(LocalDate.of(2024, 10, 1), lastUpdate)
        }
        result[1].apply {
            assertEquals("Piero Tiberi", name)
            assertEquals("Piero_Tiberi", wikiId)
            assertEquals(LocalDate.of(2024, 9, 27), lastUpdate)
        }
        result[2].apply {
            assertEquals("Riccardo Garrone", name)
            assertEquals("Riccardo_Garrone_(attore)", wikiId)
            assertEquals(LocalDate.of(2024, 7, 18), lastUpdate)
        }
    }
}