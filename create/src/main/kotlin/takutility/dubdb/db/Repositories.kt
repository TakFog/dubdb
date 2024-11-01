package takutility.dubdb.db

import takutility.dubdb.entities.*

interface EntityRepository<E: Entity> {

    fun save(entity: E): E

    fun findById(dubdbId: String): E?

}

interface MovieRepository: EntityRepository<Movie>
interface ActorRepository: EntityRepository<Actor>
interface DubberRepository: EntityRepository<Dubber> {
    fun findMostRecent(limit: Int): List<Dubber>
}
interface DubbedEntityRepository: EntityRepository<DubbedEntity> {
    fun findMostCommonMovies(limit: Int): List<MovieRef>
}
