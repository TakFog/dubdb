package takutility.dubdb.entities

interface MovieRef: EntityRef<Movie>

class Movie(
    name: String,
    val movieType: MovieType,
    val year: Int,
    ids: SourceIds = SourceIds(),
    parsed: Boolean = false,
    sources: MutableList<DataSource> = mutableListOf()
): MovieRef, Entity<Movie>(name, ids, parsed, sources) {
    override fun get(): Movie = this
}

enum class MovieType { MOVIE, SERIES }
