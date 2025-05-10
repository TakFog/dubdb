package takutility.dubdb.ops.actor

import takutility.dubdb.DubDbContext
import takutility.dubdb.entities.Actor
import takutility.dubdb.entities.SourceIds
import takutility.dubdb.ops.ExtractPerson
import takutility.dubdb.tasks.trakt.UpdateActor

class ExtractActor(context: DubDbContext): ExtractPerson<Actor>(context) {
    override val db = context.actorDb

    override fun updateRefIds(persons: List<Actor>) = context.dubEntityDb.updateRefIds(persons)

    override fun newPerson(title: String, ids: SourceIds) = Actor(title, ids = ids)

    override fun moreIds(person: Actor) {
        context[UpdateActor::class].run(person)
    }

}