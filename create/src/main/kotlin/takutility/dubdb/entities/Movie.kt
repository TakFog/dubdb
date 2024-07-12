package takutility.dubdb.entities

typealias MovieRef = EntityRef

fun movieRefOf(name: String? = null, ids: SourceIds = SourceIds()): MovieRef = BaseEntityRefImpl<Movie>(name, ids)

class Movie(
    name: String,
    val movieType: MovieType,
    val year: Int,
    ids: SourceIds = SourceIds(),
    parsed: Boolean = false,
    sources: MutableList<RawData> = mutableListOf()
): MovieRef, Entity(name, ids, parsed, sources) {
    override fun get(): Movie = this
}

enum class MovieType { MOVIE, SERIES }
