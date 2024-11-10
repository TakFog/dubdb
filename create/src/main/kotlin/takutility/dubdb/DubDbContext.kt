package takutility.dubdb

import takutility.dubdb.db.ActorRepository
import takutility.dubdb.db.DubbedEntityRepository
import takutility.dubdb.db.DubberRepository
import takutility.dubdb.db.MovieRepository
import takutility.dubdb.service.Trakt
import takutility.dubdb.service.WikiApi
import takutility.dubdb.tasks.wiki.ReadIds
import takutility.dubdb.wiki.WikiPageLoader

interface DubDbContext {
    val movieDb: MovieRepository
    val actorDb: ActorRepository
    val dubberDb: DubberRepository
    val dubEntityDb: DubbedEntityRepository
    val trakt: Trakt
    val wikiApi: WikiApi
    val wikiPageLoader: WikiPageLoader

    val readIds: ReadIds
}

open class DubDbContextBase(
    override val movieDb: MovieRepository,
    override val actorDb: ActorRepository,
    override val dubberDb: DubberRepository,
    override val dubEntityDb: DubbedEntityRepository,
    override val trakt: Trakt,
    override val wikiApi: WikiApi,
    override val wikiPageLoader: WikiPageLoader,
): DubDbContext {
    override val readIds: ReadIds by lazy { ReadIds(this) }
}