package takutility.dubdb.tasks.wiki

import takutility.dubdb.DubDbContext
import takutility.dubdb.entities.EntityRef
import takutility.dubdb.entities.Source
import takutility.dubdb.entities.SourceId
import takutility.dubdb.entities.SourceIds
import takutility.dubdb.tasks.TaskResult
import java.net.URLDecoder

private val ogImageUrl = Regex("""^https://upload.wikimedia.org/wikipedia/commons/thumb/[^/]+/[^/]+/([^/]+)/""")

class FindPhoto(context: DubDbContext): WikiPageTask(context) {

    fun run(entity: EntityRef): TaskResult {
        return find(entity)?.let { TaskResult.with(it) } ?: TaskResult.empty
    }

    private fun find(entity: EntityRef): SourceIds? {
        val wiki = entity.wiki ?: return null
        val doc = load(wiki.id) ?: return null
        return doc.select("""meta[property="og:image"]""").first()
            ?.let { ogImageUrl.find(it.attr("content"))?.groupValues?.get(1) }
            ?.let { SourceIds.of(wiki, SourceId(Source.WIKIMEDIA, URLDecoder.decode(it, Charsets.UTF_8))) }
            ?: SourceIds.of(wiki)
    }
}