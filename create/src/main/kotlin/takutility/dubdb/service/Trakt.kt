package takutility.dubdb.service

import com.uwetrottmann.trakt5.TraktV2
import com.uwetrottmann.trakt5.entities.*
import com.uwetrottmann.trakt5.enums.IdType
import org.threeten.bp.LocalDate
import org.threeten.bp.OffsetDateTime
import retrofit2.Response
import takutility.dubdb.Config
import takutility.dubdb.entities.EntityRef
import takutility.dubdb.entities.Source.IMDB
import takutility.dubdb.entities.Source.TRAKT

typealias IntPredicate = (Int) -> Boolean

interface Trakt {

    fun searchImdb(imdbId: String): SearchResults?
    fun searchTrakt(traktId: String): SearchResults?

    fun searchTrakt(traktId: Int): SearchResults? = searchTrakt(traktId.toString())
    fun search(entity: EntityRef): SearchResults? = if (TRAKT in entity.ids)
            entity.traktId?.let(this::searchTrakt)
        else
            entity.ids[IMDB]?.id?.let(this::searchImdb)

    fun personCredits(traktId: Int): CreditResults
}

class SearchResults(private val results: List<SearchResult>) {

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

class CreditResults(val movies: List<CastMember>, val shows: List<CastMember>): Iterable<CastMember> {
    override fun iterator(): Iterator<CastMember> = sequence {
        yieldAll(movies)
        yieldAll(shows)
    }.iterator()

}

class TraktImpl(private val trakt: TraktV2) : Trakt {

    constructor(apiKey: String): this(TraktV2(apiKey))
    constructor(config: Config): this(config.trakt.client_id)
    constructor(): this(Config.inst)

    override fun searchImdb(imdbId: String): SearchResults? {
        return idLookup(IdType.IMDB, imdbId)
    }

    override fun searchTrakt(traktId: String): SearchResults? {
        return idLookup(IdType.TRAKT, traktId)
    }

    override fun personCredits(traktId: Int): CreditResults {
        TODO("Not yet implemented")
    }

    private fun idLookup(type: IdType, id: String): SearchResults? {
        return try {
            trakt.search().idLookup(type, id, null, null, null, null).execute().ifSuccessful()
                ?.body()?.let { SearchResults(it) }
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

}

class MovieOrShow(
    val base: BaseEntity,
    val year: Int? = null,
    val ids: BaseIds? = null,
    val slug: String? = null,
    val certification: String? = null,
    val tagline: String? = null,
    val released: LocalDate? = null,
    val runtime: Int? = null,
    val trailer: String? = null,
    val homepage: String? = null,
    val language: String? = null,
    val genres: List<String>? = null,
) {
    val movie get() = base as Movie
    val show get() = base as Show

    val title: String? by base::title
    val overview: String? by base::overview
    val rating: Double? by base::rating
    val votes: Int? by base::votes
    val updated_at: OffsetDateTime? by base::updated_at
    val available_translations: List<String>? by base::available_translations

    fun isMovie() = base is Movie
    fun isShow() = base is Show
}

fun useId(id: Int) = true
fun ignoreId(id: Int) = false

fun <T> Response<T>.ifSuccessful() = if (isSuccessful) this else null

fun Movie.toEntity() = MovieOrShow(
    base = this,
    year = year,
    ids = ids,
    slug = ids.slug,
    certification = certification,
    runtime = runtime,
    trailer = trailer,
    homepage = homepage,
    language = language,
    genres = genres,
)


fun Show.toEntity() = MovieOrShow(
    base = this,
    year = year,
    ids = ids,
    slug = ids.slug,
    certification = certification,
    runtime = runtime,
    trailer = trailer,
    homepage = homepage,
    language = language,
    genres = genres,
)

fun CastMember.movieOrShow() = movie?.toEntity() ?: show?.toEntity()