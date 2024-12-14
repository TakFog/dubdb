package takutility.dubdb.ops.dubber

import takutility.dubdb.DubDbContext
import takutility.dubdb.entities.DubberRef
import takutility.dubdb.m
import takutility.dubdb.tasks.wikiapi.DubbersFromCategory

class Dubbers(val context: DubDbContext) {

    fun run(num: Int) {
        var dubbers = context.dubEntityDb.findMostCommonDubbers(num)
        if (notEnoughDubbers(num, dubbers)) {
            context.m<DubbersFromCategory>().run(num)
            dubbers = context.dubberDb.findMostRecent(num)
            val pop = context.dubEntityDb.countDubbers(dubbers)
            dubbers = dubbers.sortedByDescending { pop.getOrDefault(it, 0) }
        }

        dubbers.asSequence()
            .mapNotNull { it.wikiId }
            .map { context.wikiPageLoader.page(it) }
            .filter { it.exists() }
            .forEach { context.m<ExtractDubber>().run(it) }
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