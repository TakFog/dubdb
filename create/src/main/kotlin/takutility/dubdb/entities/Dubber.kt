package takutility.dubdb.entities

import java.time.Instant
import java.time.LocalDate

interface DubberRef: EntityRef {
    override fun get(): Dubber?
    override fun toRef(): DubberRef = DubberRefImpl(name, ids.toMutable())
}

class DubberRefImpl(name: String? = null, ids: SourceIds = SourceIds(), parsed: Boolean? = null)
    : BaseEntityRefImpl<Dubber>(name, ids, parsed), DubberRef

class Dubber(
    name: String,
    ids: SourceIds = SourceIds(),
    var lastUpdate: LocalDate? = null,
    parseTs: Instant? = null,
    sources: MutableList<RawData> = mutableListOf()
): DubberRef, Entity(name, ids, parseTs, sources) {
    override fun get(): Dubber = this
}