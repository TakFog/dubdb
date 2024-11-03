package takutility.dubdb

import takutility.dubdb.db.ActorRepository
import takutility.dubdb.db.DubbedEntityRepository
import takutility.dubdb.db.DubberRepository
import takutility.dubdb.db.MovieRepository
import takutility.dubdb.service.Trakt
import takutility.dubdb.service.WikiApi

interface DubDbContext {
    val movieDb: MovieRepository
    val actorDb: ActorRepository
    val dubberDb: DubberRepository
    val dubEntityDb: DubbedEntityRepository
    val trakt: Trakt
    val wikiApi: WikiApi
}
