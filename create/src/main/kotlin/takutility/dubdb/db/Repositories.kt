package takutility.dubdb.db

import takutility.dubdb.entities.*

interface EntityRepository<E: Entity> {

    fun save(entity: E): E

    fun findById(dubdbId: String): E?

}

interface MovieRepository: EntityRepository<Movie>
interface ActorRepository: EntityRepository<Actor>
interface DubberRepository: EntityRepository<Dubber>
interface DubbedEntityRepository: EntityRepository<DubbedEntity>
