package takutility.dubdb.tasks.wiki

import takutility.dubdb.entities.ActorRef
import takutility.dubdb.tasks.TaskResult
import takutility.dubdb.wiki.WikiPageLoader

class FindActorPhoto(loader: WikiPageLoader): WikiPageTask(loader) {

    fun run(actor: ActorRef): TaskResult {
        return TaskResult.empty //TODO
    }

}