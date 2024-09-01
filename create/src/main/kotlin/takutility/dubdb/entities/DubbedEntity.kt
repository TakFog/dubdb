package takutility.dubdb.entities

class DubbedEntity(
    name: String,
    var movie: MovieRef,
    var chara: Chara? = null,
    var dubber: DubberRef? = null,
    var actor: ActorRef? = null,
    ids: SourceIds = SourceIds(),
    parsed: Boolean = false,
    sources: MutableList<RawData> = mutableListOf(),
    //DubAttributes
): Entity(name, ids, parsed, sources) {
    override fun get(): DubbedEntity = this
    override fun toRef(): EntityRef = EntityRefImpl(name, ids.toMutable())

}