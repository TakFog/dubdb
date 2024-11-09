package takutility.dubdb

import org.mockito.Mockito.mock
import takutility.dubdb.db.ActorRepository
import takutility.dubdb.db.DubbedEntityRepository
import takutility.dubdb.db.DubberRepository
import takutility.dubdb.db.MovieRepository
import takutility.dubdb.service.Trakt
import takutility.dubdb.service.WikiApi

class TestContext(
    override var movieDb: MovieRepository,
    override var actorDb: ActorRepository,
    override var dubberDb: DubberRepository,
    override var dubEntityDb: DubbedEntityRepository,
    override var trakt: Trakt,
    override var wikiApi: WikiApi,
) : DubDbContext {

    companion object {
        fun mocked(init: ((TestContext) -> Unit)? = null): TestContext {
            val ctx = TestContext(mock(), mock(), mock(), mock(), mock(), mock())
            init?.let { ctx.also(it) }
            return ctx
        }
    }
}