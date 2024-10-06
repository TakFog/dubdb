package takutility.dubdb.entities

import java.time.LocalDate

interface DubberRef: EntityRef {
    override fun get(): Dubber?
    override fun toRef(): DubberRef = DubberRefImpl(name, ids.toMutable())
}

class DubberRefImpl(name: String? = null, ids: SourceIds = SourceIds())
    : BaseEntityRefImpl<Dubber>(name, ids), DubberRef

class Dubber(
    name: String,
    ids: SourceIds = SourceIds(),
    var lastUpdate: LocalDate? = null,
    parsed: Boolean = false,
    sources: MutableList<RawData> = mutableListOf()
): DubberRef, Entity(name, ids, parsed, sources) {
    override fun get(): Dubber = this
}