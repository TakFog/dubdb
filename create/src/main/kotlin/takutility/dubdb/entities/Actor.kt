package takutility.dubdb.entities

import java.time.Instant

interface ActorRef: TypedEntityRef<ActorRef>, EntityRefOf<Actor> {
    override fun toRef(): ActorRef = ActorRefImpl(name, ids.toMutable(), parsed)
    override fun asRef(): ActorRef = this
}

class ActorRefImpl(name: String? = null, ids: SourceIds = SourceIds(), parsed: Boolean? = null)
    : BaseEntityRefImpl<Actor>(name, ids, parsed), ActorRef

class Actor(
    name: String,
    ids: SourceIds = SourceIds(),
    parseTs: Instant? = null,
    sources: MutableList<RawData> = mutableListOf()
): ActorRef, Entity(name, ids, parseTs, sources), EntityOf<Actor, ActorRef> {
    override fun get(): Actor = this
    override fun asRef(): ActorRef = this
}