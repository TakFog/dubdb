package takutility.dubdb.entities

class DubbedEntity(
    name: String,
    val movie: MovieRef,
    val chara: Chara? = null,
    val dubber: DubberRef? = null,
    val actor: ActorRef? = null,
    ids: SourceIds = SourceIds(),
    parsed: Boolean = false,
    sources: MutableList<RawData> = mutableListOf(),
    //DubAttributes
): Entity(name, ids, parsed, sources) {
    override fun get(): DubbedEntity = this
    override fun toRef(): EntityRef = EntityRefImpl(name, ids.toMutable())

}