package takutility.dubdb.entities

interface MovieRef: EntityRef {
    override fun get(): Movie?
}

class MovieRefImpl(name: String? = null, ids: SourceIds = SourceIds())
    : BaseEntityRefImpl<Movie>(name, ids), MovieRef

fun movieRefOf(name: String? = null, ids: SourceIds = SourceIds()): MovieRef = MovieRefImpl(name, ids)

class Movie(
    name: String,
    var type: MovieType? = null,
    var year: Int? = null,
    ids: SourceIds = SourceIds(),
    parsed: Boolean = false,
    sources: MutableList<RawData> = mutableListOf()
): MovieRef, Entity(name, ids, parsed, sources) {
    override fun get(): Movie = this
}

enum class MovieType { MOVIE, SERIES }
