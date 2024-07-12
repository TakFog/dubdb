package takutility.dubdb.entities

interface DubberRef: EntityRef {
    override fun get(): Dubber
}

class Dubber(
    name: String,
    ids: SourceIds = SourceIds(),
    parsed: Boolean = false,
    sources: MutableList<RawData> = mutableListOf()
): DubberRef, Entity(name, ids, parsed, sources) {
    override fun get(): Dubber = this
}