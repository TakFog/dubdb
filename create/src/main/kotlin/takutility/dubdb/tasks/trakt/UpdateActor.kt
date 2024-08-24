package takutility.dubdb.tasks.trakt

import takutility.dubdb.entities.Actor
import takutility.dubdb.entities.Source
import takutility.dubdb.service.Trakt
import takutility.dubdb.service.useId

class UpdateActor(private val trakt: Trakt) {

    fun run(actor: Actor) {
        if (Source.TRAKT in actor.ids)
            return // already processed
        if (Source.IMDB !in actor.ids)
            return // can't process without id

        trakt.searchImdb(actor.ids[Source.IMDB]!!.id)
            ?.iterate(person = ::useId)
            ?.let { actor.traktId = it }
    }
}
