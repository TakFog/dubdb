package takutility.dubdb.entities

enum class DataSource {
    MOVIE_ORIG,
    MOVIE_ORIG_DUB,
    MOVIE_DUB,
    DUBBER,
    TRAKT,
}

data class RawData(
    val sourceId: SourceId,
    val dataSource: DataSource,
    val raw: String)