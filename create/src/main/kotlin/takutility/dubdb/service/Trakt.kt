package takutility.dubdb.service

import com.uwetrottmann.trakt5.TraktV2
import com.uwetrottmann.trakt5.entities.SearchResult
import com.uwetrottmann.trakt5.enums.IdType
import retrofit2.Response
import takutility.dubdb.Config
import takutility.dubdb.entities.EntityRef
import takutility.dubdb.entities.Source.IMDB
import takutility.dubdb.entities.Source.TRAKT

typealias IntPredicate = (Int) -> Boolean

interface Trakt {

    fun searchImdb(imdbId: String): TraktResults?
    fun searchTrakt(traktId: String): TraktResults?

    fun searchTrakt(traktId: Int): TraktResults? = searchTrakt(traktId.toString())
    fun search(entity: EntityRef): TraktResults? = if (TRAKT in entity.ids)
            entity.ids[TRAKT]?.id?.let(this::searchTrakt)
        else
            entity.ids[IMDB]?.id?.let(this::searchImdb)

}

class TraktResults(val results: List<SearchResult>) {

    fun iterate(movie: IntPredicate = ::ignoreId, show: IntPredicate = ::ignoreId,
                   person: IntPredicate = ::ignoreId): Int? {
        for (result in results) {
            if (result.movie?.ids?.trakt?.let(movie) == true)
                return result.movie.ids.trakt
            if (result.show?.ids?.trakt?.let(show) == true)
                return result.show.ids.trakt
            if (result.person?.ids?.trakt?.let(person) == true)
                return result.person.ids.trakt
        }
        return null
    }

    fun getYear(traktId: Int?): Int? {
        if (traktId == null) return null
        for (result in results) {
            if (traktId == result.movie?.ids?.trakt) return result.movie?.year
            if (traktId == result.show?.ids?.trakt) return result.show?.year
        }
        return null
    }
}

class TraktImpl(private val trakt: TraktV2) : Trakt {

    constructor(apiKey: String): this(TraktV2(apiKey))
    constructor(config: Config): this(config.trakt.client_id)
    constructor(): this(Config.inst)

    override fun searchImdb(imdbId: String): TraktResults? {
        return idLookup(IdType.IMDB, imdbId)
    }

    override fun searchTrakt(traktId: String): TraktResults? {
        return idLookup(IdType.TRAKT, traktId)
    }

    private fun idLookup(type: IdType, id: String): TraktResults? {
        return try {
            trakt.search().idLookup(type, id, null, null, null, null).execute().ifSuccessful()
                ?.body()?.let { TraktResults(it) }
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

}

fun useId(id: Int) = true
fun ignoreId(id: Int) = false

fun <T> Response<T>.ifSuccessful() = if (isSuccessful) this else null