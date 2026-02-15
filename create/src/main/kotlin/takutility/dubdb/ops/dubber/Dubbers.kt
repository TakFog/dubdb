package takutility.dubdb.ops.dubber

import mu.KotlinLogging
import takutility.dubdb.DubDbContext
import takutility.dubdb.entities.DubberRef

private val logger = KotlinLogging.logger {}

class Dubbers(val context: DubDbContext) {

    fun run(num: Int) {
        var dubbers = context.dubEntityDb.findMostCommonDubbers(num)
        //TODO tests disabled, remove both or update
//        if (notEnoughDubbers(num, dubbers)) {
//            dubbers = context.m<LatestDubbers>().run(num)
//        }

        val filtered = dubbers.asSequence()
            .mapNotNull { it.wikiId }
            .map { context.wikiHtmlLoader.page(it) }
            .filter { it.exists() }
            .toList()

        var i = 0
        filtered.forEach {
            i++
            logger.info { "$i/${filtered.size} Extracting ${it.title}" }
            context[ExtractDubber::class].run(it)
        }
    }

    private fun notEnoughDubbers(num: Int, dubbers: List<DubberRef>): Boolean {
        if (dubbers.size < num)
            return true

        val lastPop = context.dubEntityDb.countDubber(dubbers.last())
        if (lastPop < 10)
            return true

        val halfPop = context.dubEntityDb.countDubber(dubbers[dubbers.size/2])
        if (halfPop == lastPop)
            return true

        return false
    }
}