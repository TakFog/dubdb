package takutility.dubdb.entities

interface DubberRef: EntityRef<Dubber>

class Dubber(
    name: String,
    ids: SourceIds = SourceIds(),
    parsed: Boolean = false,
    sources: MutableList<DataSource> = mutableListOf()
): DubberRef, Entity<Dubber>(name, ids, parsed, sources) {
    override fun get(): Dubber = this
}