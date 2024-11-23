package takutility.dubdb.ops.dubber

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import takutility.dubdb.TestContext
import takutility.dubdb.entities.Dubber
import takutility.dubdb.entities.DubberRef
import takutility.dubdb.entities.Source
import takutility.dubdb.entities.SourceIds
import takutility.dubdb.m
import takutility.dubdb.tasks.wikiapi.DubbersFromCategory
import takutility.dubdb.wiki.WikiPage

internal class DubbersTest {
    lateinit var ctx: TestContext
    lateinit var op: Dubbers

    @BeforeEach
    fun setUp() {
        ctx = TestContext.mocked(true)
        op = Dubbers(ctx)
    }

    @Test
    fun run() {
        val size = 5
        val mostCommon = mockDubbers(size)
        val pages = dubbers2pages(mostCommon)

        whenever(ctx.dubEntityDb.findMostCommonDubbers(any())).thenReturn(mostCommon)

        op.run(size)

        verify(ctx.dubEntityDb).findMostCommonDubbers(size)
        verify(ctx.m<DubbersFromCategory>(), never()).run(size)
        verify(ctx.dubberDb, never()).findMostRecent(size)
        verify(ctx.m<ExtractDubber>(), times(size)).run(any())
        mostCommon.forEach { verify(ctx.m<ExtractDubber>()).run(pages[it.wikiId]!!) }
    }

    @Test
    fun tooFewDubbers() {
        val size = 5
        val mostRecent = mockDubbers(size)
        val pages = dubbers2pages(mostRecent)

        whenever(ctx.dubEntityDb.findMostCommonDubbers(any())).thenReturn(IntRange(0, size - 2).map { mock<DubberRef>() }.toList())
        whenever(ctx.dubberDb.findMostRecent(any())).thenReturn(mostRecent)

        op.run(size)

        verify(ctx.dubEntityDb).findMostCommonDubbers(size)
        verify(ctx.m<DubbersFromCategory>()).run(size)
        verify(ctx.dubberDb).findMostRecent(size)
        verify(ctx.m<ExtractDubber>(), times(size)).run(any())
        mostRecent.forEach { verify(ctx.m<ExtractDubber>()).run(pages[it.wikiId]!!) }
    }

    @Test
    fun noDubbers() {
        val size = 5
        val mostRecent = mockDubbers(size - 2)
        val pages = dubbers2pages(mostRecent)

        whenever(ctx.dubEntityDb.findMostCommonDubbers(any())).thenReturn(listOf())
        whenever(ctx.dubberDb.findMostRecent(any())).thenReturn(mostRecent)

        op.run(size)

        verify(ctx.dubEntityDb).findMostCommonDubbers(size)
        verify(ctx.m<DubbersFromCategory>()).run(size)
        verify(ctx.dubberDb).findMostRecent(size)
        verify(ctx.m<ExtractDubber>(), times(mostRecent.size)).run(any())
        mostRecent.forEach { verify(ctx.m<ExtractDubber>()).run(pages[it.wikiId]!!) }
    }

    private fun mockDubbers(size: Int) = IntRange(1, size).map {
        Dubber("name $it", ids = SourceIds.of(Source.WIKI to "name_$it"))
    }.toList()

    private fun dubbers2pages(dubbers: List<Dubber>): Map<String?, WikiPage> {

        val pages = dubbers.map { it.wikiId }.associateWith {
            mock<WikiPage> { on {exists()} doReturn true }
        }
        ctx.wikiPageLoader = mock {
            on { page(any()) }.then { a -> pages.getOrDefault(a.getArgument(0), null) }
        }
        return pages
    }
}
