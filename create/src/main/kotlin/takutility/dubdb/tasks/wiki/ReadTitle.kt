package takutility.dubdb.tasks.wiki

import takutility.dubdb.DubDbContext
import takutility.dubdb.entities.EntityRef
import takutility.dubdb.tasks.TaskResult
import takutility.dubdb.wiki.WikiPage

val cleanTitle = Regex(""" \(.+\)$""")

class ReadTitle(context: DubDbContext): WikiPageTask(context) {

    fun run(entity: EntityRef): TaskResult = entity.wiki
        ?.let(this::loadPage)
        ?.let { run(it) }
        ?: TaskResult.empty

    fun run(page: WikiPage): TaskResult {
        return page.doc?.select("h1")?.get(0)?.text()
            ?.let { TaskResult(string = cleanTitle.replace(it, "")) } ?: return TaskResult.empty
    }

}