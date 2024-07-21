package takutility.dubdb.tasks.wiki

import takutility.dubdb.entities.DubberRef
import takutility.dubdb.tasks.TaskResult
import takutility.dubdb.wiki.WikiPageLoader

class FindDubberPhoto(loader: WikiPageLoader): WikiPageTask(loader) {

    fun run(actor: DubberRef): TaskResult {
        return TaskResult.empty //TODO
    }

}