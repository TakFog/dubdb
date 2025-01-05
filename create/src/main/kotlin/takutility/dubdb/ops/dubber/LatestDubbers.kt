package takutility.dubdb.ops.dubber

import takutility.dubdb.DubDbContext
import takutility.dubdb.entities.Dubber
import takutility.dubdb.m
import takutility.dubdb.tasks.wikiapi.DubbersFromCategory

/**
 * Return the most recently updated dubbers in Wikipedia ordered by popularity
 */
class LatestDubbers(val context: DubDbContext) {

    fun run(num: Int): List<Dubber> {
        val fromCat = context.m<DubbersFromCategory>().run(context.config.wiki.categoryLimit)

        val toSave = mutableListOf<Dubber>()
        val toUpdate = mutableListOf<Dubber>()
        fromCat.forEach { d ->
            val res = context.dubberDb.findBySource(d.wiki!!)
            if (res.isEmpty()) toSave.add(d)
            else res
                .filter { it.lastUpdate != d.lastUpdate }
                .forEach {
                    it.lastUpdate = d.lastUpdate
                    toUpdate.add(it)
                }
        }
        val saved = context.dubberDb.save(toSave)
        context.dubberDb.save(toUpdate)
        context.dubEntityDb.updateRefIds(saved)

        var recent = context.dubberDb.findMostRecent(num, unparsed = true, updated = false)
        if (recent.size < num) recent = context.dubberDb.findMostRecent(num, unparsed = true, updated = true)

        val pop = context.dubEntityDb.countDubbers(recent)
        return recent.sortedByDescending { pop.getOrDefault(it, 0) }
    }
}