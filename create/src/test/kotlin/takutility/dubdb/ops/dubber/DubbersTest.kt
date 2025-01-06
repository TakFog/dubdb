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
        whenever(ctx.dubEntityDb.countDubber(any())).thenReturn(500)
        whenever(ctx.dubEntityDb.countDubber(mostCommon.last())).thenReturn(100)

        op.run(size)

        verify(ctx.dubEntityDb).findMostCommonDubbers(size)
        verify(ctx.m<LatestDubbers>(), never()).run(size)

        val extractDubber = ctx.m<ExtractDubber>()
        verify(extractDubber, times(size)).run(any())
        val inOrder = inOrder(extractDubber)
        mostCommon.forEach { inOrder.verify(extractDubber).run(pages[it.wikiId]!!) }
    }

    @Test
    fun tooFewDubbers() {
        val size = 5
        val mostRecent = mockDubbers(size)
        val pages = dubbers2pages(mostRecent)

        whenever(ctx.dubEntityDb.findMostCommonDubbers(any())).thenReturn(IntRange(0, size - 2).map { mock<DubberRef>() }.toList())
        whenever(ctx.m<LatestDubbers>().run(any())).thenReturn(mostRecent)

        op.run(size)

        verify(ctx.dubEntityDb).findMostCommonDubbers(size)
        verify(ctx.dubEntityDb, never()).countDubber(any())
        verify(ctx.m<LatestDubbers>()).run(size)

        val extractDubber = ctx.m<ExtractDubber>()
        verify(extractDubber, times(size)).run(any())
        val inOrder = inOrder(extractDubber)
        mostRecent.forEach { inOrder.verify(extractDubber).run(pages[it.wikiId]!!) }
    }

    @Test
    fun tooUnpopularDubbers() {
        val size = 5
        val dubbers = mockDubbers(size * 2)
        val mostCommon = dubbers.subList(0, size)
        val mostRecent = dubbers.subList(size, dubbers.size)
        val pages = dubbers2pages(mostRecent)

        whenever(ctx.dubEntityDb.findMostCommonDubbers(any())).thenReturn(mostCommon)
        whenever(ctx.m<LatestDubbers>().run(any())).thenReturn(mostRecent)
        whenever(ctx.dubEntityDb.countDubber(any())).thenReturn(500)
        whenever(ctx.dubEntityDb.countDubber(mostCommon.last())).thenReturn(9)

        op.run(size)

        verify(ctx.dubEntityDb).findMostCommonDubbers(size)
        verify(ctx.m<LatestDubbers>()).run(size)

        val extractDubber = ctx.m<ExtractDubber>()
        verify(extractDubber, times(size)).run(any())
        val inOrder = inOrder(extractDubber)
        mostRecent.forEach { inOrder.verify(extractDubber).run(pages[it.wikiId]!!) }
    }

    @Test
    fun sameDubberPopularity() {
        val size = 5
        val dubbers = mockDubbers(size * 2)
        val mostCommon = dubbers.subList(0, size)
        val mostRecent = dubbers.subList(size, dubbers.size)
        val pages = dubbers2pages(mostRecent)

        whenever(ctx.dubEntityDb.findMostCommonDubbers(any())).thenReturn(mostCommon)
        whenever(ctx.m<LatestDubbers>().run(any())).thenReturn(mostRecent)
        whenever(ctx.dubEntityDb.countDubber(any())).thenReturn(500)

        op.run(size)

        verify(ctx.dubEntityDb).findMostCommonDubbers(size)
        verify(ctx.m<LatestDubbers>()).run(size)

        val extractDubber = ctx.m<ExtractDubber>()
        verify(extractDubber, times(size)).run(any())
        val inOrder = inOrder(extractDubber)
        mostRecent.forEach { inOrder.verify(extractDubber).run(pages[it.wikiId]!!) }
    }

    @Test
    fun noDubbers() {
        val size = 5
        val mostRecent = mockDubbers(size - 2)
        val pages = dubbers2pages(mostRecent)

        whenever(ctx.dubEntityDb.findMostCommonDubbers(any())).thenReturn(listOf())
        whenever(ctx.m<LatestDubbers>().run(any())).thenReturn(mostRecent)

        op.run(size)

        verify(ctx.dubEntityDb).findMostCommonDubbers(size)
        verify(ctx.m<LatestDubbers>()).run(size)
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
