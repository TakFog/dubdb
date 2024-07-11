package takutility.dubdb.entities

class DubbedEntity(
    name: String,
    val movie: MovieRef,
    val chara: Chara?,
    val dubber: DubberRef?,
    val actor: ActorRef?,
    ids: SourceIds = SourceIds(),
    parsed: Boolean = false,
    sources: MutableList<DataSource> = mutableListOf(),
    //DubAttributes
) {
}