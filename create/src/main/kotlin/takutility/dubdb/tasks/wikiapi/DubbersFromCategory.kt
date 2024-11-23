package takutility.dubdb.tasks.wikiapi

import takutility.dubdb.DubDbContext
import takutility.dubdb.entities.Dubber
import takutility.dubdb.entities.Source
import takutility.dubdb.entities.SourceIds
import takutility.dubdb.service.WikiApi
import java.time.LocalDate
import java.time.ZoneOffset

class DubbersFromCategory(context: DubDbContext) {
    private val api: WikiApi = context.wikiApi

    fun run(limit: Int): List<Dubber> {
        return api.dubbersFromCat(limit).queryValue()
            .map { Dubber(
                name = cleanWikiTitle(it.title),
                ids = SourceIds.of(Source.WIKI to it.title.replace(" ", "_")),
                lastUpdate = LocalDate.ofInstant(it.timestamp, ZoneOffset.UTC),
            ) }
            .toList()
    }
}

private fun cleanWikiTitle(title: String): String {
    val bracket = title.indexOf("(")
    if (bracket < 3) return title
    return title.substring(0, bracket).trim()
}
