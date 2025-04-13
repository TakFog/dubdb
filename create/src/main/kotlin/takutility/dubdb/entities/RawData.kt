package takutility.dubdb.entities

enum class DataSource(val parent: ParentDataSource) {
    MOVIE_ORIG(ParentDataSource.MOVIE),
    MOVIE_ORIG_DUB(ParentDataSource.MOVIE),
    MOVIE_DUB(ParentDataSource.MOVIE),
    DUBBER(ParentDataSource.DUBBER),
    TRAKT(ParentDataSource.TRAKT),
    TRAKT_MOVIE(ParentDataSource.TRAKT),
    TRAKT_ACTOR(ParentDataSource.TRAKT),
}

enum class ParentDataSource {
    MOVIE,
    DUBBER,
    TRAKT,
}

data class RawData(
    val sourceId: SourceId,
    val dataSource: DataSource,
    val raw: String)