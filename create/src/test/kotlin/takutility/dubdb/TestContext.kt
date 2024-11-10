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

class TestContext(
    override var movieDb: MovieRepository,
    override var actorDb: ActorRepository,
    override var dubberDb: DubberRepository,
    override var dubEntityDb: DubbedEntityRepository,
    override var trakt: Trakt,
    override var wikiApi: WikiApi,
    override var wikiPageLoader: WikiPageLoader
) : DubDbContextBase(movieDb, actorDb, dubberDb, dubEntityDb, trakt, wikiApi, wikiPageLoader) {

    companion object {
        fun mocked(init: ((TestContext) -> Unit)? = null): TestContext {
            val wikiPageLoader = CachedWikiPageLoader("src/test/resources/cache")
            val ctx = TestContext(mock(), mock(), mock(), mock(), mock(), mock(), wikiPageLoader)
            init?.let { ctx.also(it) }
            return ctx
        }
    }
}