package takutility.dubdb.entities

interface ActorRef: EntityRef {
    override fun get(): Actor

}

class Actor(
    name: String,
    ids: SourceIds = SourceIds(),
    parsed: Boolean = false,
    sources: MutableList<RawData> = mutableListOf()
): ActorRef, Entity(name, ids, parsed, sources) {
    override fun get(): Actor = this
}