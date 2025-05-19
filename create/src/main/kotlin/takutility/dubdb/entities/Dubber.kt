package takutility.dubdb.entities

import java.time.Instant
import java.time.LocalDate

interface DubberRef: EntityRefOf<Dubber>, TypedEntityRef<DubberRef> {
    override fun toRef(): DubberRef = DubberRefImpl(name, ids.toMutable(), parsed)
    override fun asRef(): DubberRef = this
}

class DubberRefImpl(name: String? = null, ids: SourceIds = SourceIds(), parsed: Boolean? = null)
    : BaseEntityRefImpl<Dubber>(name, ids, parsed), DubberRef

class Dubber(
    name: String,
    ids: SourceIds = SourceIds(),
    var lastUpdate: LocalDate? = null,
    parseTs: Instant? = null,
    sources: MutableList<RawData> = mutableListOf()
): DubberRef, Entity(name, ids, parseTs, sources), EntityOf<Dubber, DubberRef> {
    override fun get(): Dubber = this
    override fun asRef() = this
}