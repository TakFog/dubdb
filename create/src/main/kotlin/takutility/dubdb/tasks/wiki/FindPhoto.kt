package takutility.dubdb.tasks.wiki

import takutility.dubdb.DubDbContext
import takutility.dubdb.entities.EntityRef
import takutility.dubdb.entities.Source
import takutility.dubdb.entities.SourceId
import takutility.dubdb.entities.SourceIds
import takutility.dubdb.tasks.TaskResult
import takutility.dubdb.wiki.WikiPage
import java.net.URLDecoder

private val ogImageUrl = Regex("""^https://upload.wikimedia.org/wikipedia/commons/thumb/[^/]+/[^/]+/([^/]+)/""")

class FindPhoto(context: DubDbContext): WikiPageTask(context) {

    fun run(entity: EntityRef): TaskResult = entity.wiki
        ?.let(this::loadPage)
        ?.let { run(entity, it) }
        ?: TaskResult.empty

    fun run(entity: EntityRef, page: WikiPage): TaskResult {
        return find(entity, page)?.let { TaskResult.with(it) } ?: TaskResult.empty
    }

    private fun find(entity: EntityRef, page: WikiPage): SourceIds? {
        if (!page.exists()) return null
        val wiki = entity.wiki ?: return null
        return page.doc!!.select("""meta[property="og:image"]""").first()
            ?.let { ogImageUrl.find(it.attr("content"))?.groupValues?.get(1) }
            ?.let { SourceIds.of(wiki, SourceId(Source.WIKIMEDIA, URLDecoder.decode(it, Charsets.UTF_8))) }
            ?: SourceIds.of(wiki)
    }
}