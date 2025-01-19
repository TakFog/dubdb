package takutility.dubdb.entities

import java.time.Instant

interface ActorRef: EntityRef {
    override fun get(): Actor?
    override fun toRef(): ActorRef = ActorRefImpl(name, ids.toMutable())
}

class ActorRefImpl(name: String? = null, ids: SourceIds = SourceIds(), parsed: Boolean? = null)
    : BaseEntityRefImpl<Actor>(name, ids, parsed), ActorRef

class Actor(
    name: String,
    ids: SourceIds = SourceIds(),
    parseTs: Instant? = null,
    sources: MutableList<RawData> = mutableListOf()
): ActorRef, Entity(name, ids, parseTs, sources) {
    override fun get(): Actor = this
}