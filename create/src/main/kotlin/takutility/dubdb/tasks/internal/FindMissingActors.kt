package takutility.dubdb.tasks.internal

import takutility.dubdb.DubDbContext
import takutility.dubdb.entities.Actor
import takutility.dubdb.entities.ActorRef
import takutility.dubdb.entities.DubbedEntity
import takutility.dubdb.tasks.TaskResult

class FindMissingActors(val context: DubDbContext) {

    fun run(entities: Collection<DubbedEntity>): TaskResult {
        if (entities.isEmpty()) return TaskResult.empty

        val unparsed = mutableMapOf<String, Actor>()
        val missing = mutableListOf<ActorRef>()
        val selectedEntities = mutableListOf<DubbedEntity>()

        entities.forEach { e ->
            val actor = e.actor ?: return@forEach
            val found = context.actorDb.findBySources(actor.ids)
            if (found.isEmpty()) {
                missing.add(actor)
                selectedEntities.add(e)
            } else {
                val unparsedActors = found.filterNot { it.isParsed }
                if (unparsedActors.isNotEmpty()) {
                    unparsedActors.forEach { unparsed[it.id!!] = it }
                    selectedEntities.add(e)
                }
            }
        }

        if (selectedEntities.isEmpty()) return TaskResult.empty

        return TaskResult(actors = unparsed.values + mergeMissing(missing), dubbedEntities = selectedEntities)
    }

    private fun mergeMissing(actors: List<ActorRef>): List<Actor> {
        val merged = mutableListOf<Actor>()

        actors.forEach { a ->
            merged.find { it.matches(a) }
                ?.let { it.ids += a.ids }
                ?: merged.add(Actor(a.name ?: "", ids = a.ids.toMutable()))
        }

        return merged
    }
}