package takutility.dubdb.ops.dubber

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import takutility.dubdb.TestContext
import takutility.dubdb.db.MemDubberRepository
import takutility.dubdb.entities.Dubber
import takutility.dubdb.entities.Source
import takutility.dubdb.entities.SourceIds
import takutility.dubdb.tasks.wikiapi.DubbersFromCategory
import java.time.LocalDate
import java.time.ZoneOffset

internal class LatestDubbersTest {
    lateinit var ctx: TestContext
    lateinit var dubberDb: MemDubberRepository
    lateinit var fromCategory: DubbersFromCategory
    lateinit var op: LatestDubbers

    @BeforeEach
    fun setUp() {
        fromCategory = mock()
        dubberDb = MemDubberRepository()
        ctx = TestContext.mocked(true) {
            it.dubberDb = dubberDb
            it.set(fromCategory)
        }
        op = LatestDubbers(ctx)
    }

    @Test
    fun runEmptyDb() {
        val size = 5
        val fromCat = mockCatDubbers(size*10, LocalDate.of(2025, 1, 6))

        whenever(fromCategory.run(any())).thenReturn(fromCat)
        whenever(ctx.dubEntityDb.countDubbers(any())).thenReturn(emptyMap())

        val result = op.run(size)

        assertEquals(fromCat.size, dubberDb.db.size)
        assertEquals(size, result.size)
        assertEquals(fromCat.subList(0, size).map { it.wikiId }, result.map { it.wikiId })
    }

    @Test
    fun runPartialNew() {
        val size = 5
        val fromCat = mockCatDubbers(size*10, LocalDate.of(2025, 1, 6))
        for (i in fromCat.indices step 3) {
            dubberDb.save(fromCat[i].let { Dubber(it.name, ids = it.ids) })
        }

        whenever(fromCategory.run(any())).thenReturn(fromCat)
        whenever(ctx.dubEntityDb.countDubbers(any())).thenReturn(emptyMap())

        val result = op.run(size)

        assertEquals(fromCat.size, dubberDb.db.size)
        assertEquals(size, result.size)
        assertEquals(fromCat.subList(0, size).map { it.wikiId }, result.map { it.wikiId })
    }

    @Test
    fun skipParsed() {
        val size = 5
        val fromCat = mockCatDubbers(size*10, LocalDate.of(2025, 1, 6))
        for (i in fromCat.indices step 3) {
            dubberDb.save(fromCat[i].let { Dubber(it.name, ids = it.ids,
                parseTs = it.lastUpdate!!.plusMonths(5).atStartOfDay().toInstant(ZoneOffset.UTC)) })
        }

        whenever(fromCategory.run(any())).thenReturn(fromCat)
        whenever(ctx.dubEntityDb.countDubbers(any())).thenReturn(emptyMap())

        val result = op.run(size)

        assertEquals(fromCat.size, dubberDb.db.size)
        assertEquals(size, result.size)
        assertEquals(listOf(1, 2, 4, 5, 7).map { fromCat[it].wikiId }, result.map { it.wikiId })
    }

    @Test
    fun includeUpdated() {
        val size = 5
        val fromCat = mockCatDubbers(size+2, LocalDate.of(2025, 1, 6))
        listOf(0, 3).forEach {i ->
            dubberDb.save(fromCat[i].let {
                Dubber(it.name, ids = it.ids,
                    parseTs = it.lastUpdate!!.plusMonths(5).atStartOfDay().toInstant(ZoneOffset.UTC)
                )
            })
        }
        listOf(1, 4).forEach {i ->
            dubberDb.save(fromCat[i].let {
                Dubber(it.name, ids = it.ids,
                    parseTs = it.lastUpdate!!.minusMonths(5).atStartOfDay().toInstant(ZoneOffset.UTC)
                )
            })
        }

        whenever(fromCategory.run(any())).thenReturn(fromCat)
        whenever(ctx.dubEntityDb.countDubbers(any())).thenReturn(emptyMap())

        val result = op.run(size)

        assertEquals(fromCat.size, dubberDb.db.size)
        assertEquals(size, result.size)
        assertEquals(listOf(1, 2, 4, 5, 6).map { fromCat[it].wikiId }, result.map { it.wikiId })
    }

    private fun mockDubbers(size: Int) = IntRange(1, size).map {
        Dubber("name $it", ids = SourceIds.of(Source.WIKI to "name_$it"))
    }.toList()

    private fun mockCatDubbers(size: Int, mostRecent: LocalDate): List<Dubber> {
        val dubbers = mockDubbers(size)
        dubbers.forEachIndexed { index, dubber -> dubber.lastUpdate = mostRecent.minusDays(index.toLong()) }
        return dubbers
    }

}