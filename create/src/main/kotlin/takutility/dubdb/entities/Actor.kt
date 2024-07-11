package takutility.dubdb.entities

interface ActorRef: EntityRef<Actor>

class Actor(
    name: String,
    ids: SourceIds = SourceIds(),
    parsed: Boolean = false,
    sources: MutableList<DataSource> = mutableListOf()
): ActorRef, Entity<Actor>(name, ids, parsed, sources) {
    override fun get(): Actor = this
}