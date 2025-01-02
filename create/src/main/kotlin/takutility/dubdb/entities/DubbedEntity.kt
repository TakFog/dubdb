package takutility.dubdb.entities

import java.time.Instant

class DubbedEntity(
    name: String,
    var movie: MovieRef,
//    var chara: Chara? = null,
    var dubber: DubberRef? = null,
    var actor: ActorRef? = null,
    ids: SourceIds = SourceIds(),
    parseTs: Instant? = null,
    sources: MutableList<RawData> = mutableListOf(),
    //DubAttributes
): Entity(name, ids, parseTs, sources) {
    override fun get(): DubbedEntity = this
    override fun toRef(): EntityRef = EntityRefImpl(name, ids.toMutable())

    override fun toString(): String {
        return "$name (${movie.name})"
    }
}