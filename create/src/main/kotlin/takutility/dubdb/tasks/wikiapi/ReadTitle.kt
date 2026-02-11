package takutility.dubdb.tasks.wikiapi

import takutility.dubdb.DubDbContext
import takutility.dubdb.entities.EntityRef
import takutility.dubdb.service.wikiapi.WikiApi
import takutility.dubdb.tasks.TaskResult
import takutility.dubdb.tasks.wiki.WikiPageTask

val cleanTitle = Regex(""" \(.+\)$""")

class ReadTitle(context: DubDbContext): WikiPageTask(context) {

    fun run(entity: EntityRef): TaskResult = entity.wiki?.id
        ?.let { context[WikiApi::class].info(it) }
        ?.title
        ?.let { TaskResult(string = cleanTitle.replace(it, "")) }
        ?: TaskResult.empty

}