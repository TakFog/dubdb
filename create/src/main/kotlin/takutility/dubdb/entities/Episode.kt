package takutility.dubdb.entities

class Episode private constructor(val season: Int, val episodeFrom: Int, val episodeTo: Int) {

    companion object {
        fun fullSeason(season: Int) = Episode(season, 1, -1)
        fun single(season: Int, episode: Int) = Episode(season, episode, episode)
        fun range(season: Int, episodeFrom: Int, episodeTo: Int) = Episode
    }

    fun isFullSeason(): Boolean = episodeFrom == 1 && episodeTo < 0

    operator fun contains(other: Episode): Boolean {
        if (season != other.season) return false
        if (isFullSeason()) return true
        if (other.isFullSeason()) return false
        return episodeFrom <= other.episodeFrom && episodeTo >= other.episodeTo
    }
}
