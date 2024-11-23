package takutility.dubdb

import org.mockito.Mockito.mock
import takutility.dubdb.db.ActorRepository
import takutility.dubdb.db.DubbedEntityRepository
import takutility.dubdb.db.DubberRepository
import takutility.dubdb.db.MovieRepository
import takutility.dubdb.service.Trakt
import takutility.dubdb.service.WikiApi
import takutility.dubdb.wiki.CachedWikiPageLoader
import takutility.dubdb.wiki.WikiPageLoader
import kotlin.reflect.KClass

class TestContext(
    movieDb: MovieRepository,
    actorDb: ActorRepository,
    dubberDb: DubberRepository,
    dubEntityDb: DubbedEntityRepository,
    trakt: Trakt,
    wikiApi: WikiApi,
    wikiPageLoader: WikiPageLoader,
    val fullMock: Boolean = false
) : DubDbContextBase(movieDb, actorDb, dubberDb, dubEntityDb, trakt, wikiApi, wikiPageLoader) {
    companion object {
        fun mocked(fullMock: Boolean = false, init: ((TestContext) -> Unit)? = null): TestContext {
            val ctx = TestContext(
                movieDb = mock(),
                actorDb = mock(),
                dubberDb = mock(),
                dubEntityDb = mock(),
                trakt = mock(),
                wikiApi = mock(),
                wikiPageLoader = if (fullMock) mock() else CachedWikiPageLoader("src/test/resources/cache"),
                fullMock = fullMock
            )
            init?.let { ctx.also(it) }
            return ctx
        }
    }

    override var movieDb
        get() = super.movieDb
        set(value) = set(MovieRepository::class, value)
    override var actorDb
        get() = super.actorDb
        set(value) = set(ActorRepository::class, value)
    override var dubberDb
        get() = super.dubberDb
        set(value) = set(DubberRepository::class, value)
    override var dubEntityDb
        get() = super.dubEntityDb
        set(value) = set(DubbedEntityRepository::class, value)
    override var trakt
        get() = super.trakt
        set(value) = set(Trakt::class, value)
    override var wikiApi
        get() = super.wikiApi
        set(value) = set(WikiApi::class, value)
    override var wikiPageLoader
        get() = super.wikiPageLoader
        set(value) = set(WikiPageLoader::class, value)

    override fun <T : Any> get(clazz: KClass<T>): T {
        if (fullMock && clazz !in this)
            set(clazz, mock(clazz.java))
        return super.get(clazz)

    }
    public override fun <T : Any> set(clazz: KClass<T>, value: T) {
        super.set(clazz, value)
    }
}