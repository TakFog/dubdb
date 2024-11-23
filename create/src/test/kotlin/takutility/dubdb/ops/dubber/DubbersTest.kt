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
        ctx = TestContext.mocked()
        op = Dubbers(ctx)
    }

    @Test
    fun run() {
        val size = 5
        val mostCommon = IntRange(1, size).map {
            Dubber("name $it", ids = SourceIds.of(Source.WIKI to "name_$it"))
        }.toList()
        val pages = dubbers2pages(mostCommon)

        whenever(ctx.dubEntityDb.findMostCommonDubbers(any())).thenReturn(mostCommon)

        op.run(5)

        verify(ctx.dubEntityDb).findMostCommonDubbers(size)
        verify(ctx.m<DubbersFromCategory>(), never()).run()
        verify(ctx.dubberDb, never()).findMostRecent(size)
        verify(ctx.m<ExtractDubber>(), times(size)).run(any())
        mostCommon.forEach { verify(ctx.m<ExtractDubber>().run(pages[it.wikiId]!!)) }
    }
    @Test
    fun tooFewDubbers() {
        val size = 5
        val mostRecent = IntRange(1, size).map {
            Dubber("name $it", ids = SourceIds.of(Source.WIKI to "name_$it"))
        }.toList()
        val pages = dubbers2pages(mostRecent)

        whenever(ctx.dubEntityDb.findMostCommonDubbers(any())).thenReturn(IntRange(0, size - 2).map { mock<DubberRef>() }.toList())
        whenever(ctx.dubberDb.findMostRecent(any())).thenReturn(mostRecent)

        op.run(5)

        verify(ctx.dubEntityDb).findMostCommonDubbers(size)
        verify(ctx.m<DubbersFromCategory>()).run()
        verify(ctx.dubberDb).findMostRecent(size)
        verify(ctx.m<ExtractDubber>(), times(size)).run(any())
        mostRecent.forEach { verify(ctx.m<ExtractDubber>().run(pages[it.wikiId]!!)) }
    }

    @Test
    fun noDubbers() {
        val size = 5
        val mostRecent = IntRange(1, size - 2).map {
            Dubber("name $it", ids = SourceIds.of(Source.WIKI to "name_$it"))
        }.toList()
        val pages = dubbers2pages(mostRecent)

        whenever(ctx.dubEntityDb.findMostCommonDubbers(any())).thenReturn(listOf())
        whenever(ctx.dubberDb.findMostRecent(any())).thenReturn(mostRecent)

        op.run(5)

        verify(ctx.dubEntityDb).findMostCommonDubbers(size)
        verify(ctx.m<DubbersFromCategory>()).run()
        verify(ctx.dubberDb).findMostRecent(size)
        verify(ctx.m<ExtractDubber>(), times(mostRecent.size)).run(any())
        mostRecent.forEach { verify(ctx.m<ExtractDubber>().run(pages[it.wikiId]!!)) }
    }

    private fun dubbers2pages(dubbers: List<Dubber>): Map<String?, WikiPage> {

        val pages = dubbers.map { it.wikiId }.associateWith { mock<WikiPage>() }
        ctx.wikiPageLoader = mock {
            on { page(any()) }.then { a -> pages.getOrDefault(a.getArgument(0), null) }
        }
        return pages
    }
}
