package takutility.dubdb.tasks.wiki

import takutility.dubdb.entities.DubberRef
import takutility.dubdb.tasks.TaskResult
import takutility.dubdb.wiki.WikiPageLoader

class ReadDubberSection(loader: WikiPageLoader): WikiPageTask(loader) {

    fun run(dubber: DubberRef): TaskResult {
        val doc = load(dubber.wikiId) ?: return TaskResult.empty
        return TaskResult.empty //TODO
    }
}