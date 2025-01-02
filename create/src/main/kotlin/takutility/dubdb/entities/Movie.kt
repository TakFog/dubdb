package takutility.dubdb.entities

import java.time.Instant

interface MovieRef: EntityRef {
    val type: MovieType?

    override fun get(): Movie?
    override fun toRef(): MovieRef = MovieRefImpl(name, type, ids.toMutable())
}

class MovieRefImpl(name: String? = null, override val type: MovieType? = null, ids: SourceIds = SourceIds())
    : BaseEntityRefImpl<Movie>(name, ids), MovieRef {
    override fun toString(): String = name ?: ids.toString()
}

fun movieRefOf(name: String? = null, type: MovieType? = null, ids: SourceIds = SourceIds()): MovieRef
    = MovieRefImpl(name, type, ids)

class Movie(
    name: String,
    override var type: MovieType? = null,
    var year: Int? = null,
    ids: SourceIds = SourceIds(),
    parseTs: Instant? = null,
    sources: MutableList<RawData> = mutableListOf()
): MovieRef, Entity(name, ids, parseTs, sources) {
    override fun get(): Movie = this
}

enum class MovieType { MOVIE, SERIES }
