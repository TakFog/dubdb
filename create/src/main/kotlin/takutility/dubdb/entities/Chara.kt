package takutility.dubdb.entities

data class Chara(
    val name: String,
    val ids: SourceIds,
    val movie: MovieRef,
    val episodes: List<Episode>?,
)