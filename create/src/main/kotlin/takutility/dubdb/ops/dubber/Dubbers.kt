package takutility.dubdb.ops.dubber

import takutility.dubdb.DubDbContext
import takutility.dubdb.m
import takutility.dubdb.tasks.wikiapi.DubbersFromCategory

class Dubbers(val context: DubDbContext) {

    fun run(num: Int) {
        var dubbers = context.dubEntityDb.findMostCommonDubbers(num)
        if (dubbers.size < num) {
            context.m<DubbersFromCategory>().run(num)
            dubbers = context.dubberDb.findMostRecent(num)
        }

        dubbers.asSequence()
            .mapNotNull { it.wikiId }
            .map { context.wikiPageLoader.page(it) }
            .filter { it.exists() }
            .forEach { context.m<ExtractDubber>().run(it) }
    }
}