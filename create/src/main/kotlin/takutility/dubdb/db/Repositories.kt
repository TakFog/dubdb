package takutility.dubdb.db

import takutility.dubdb.entities.*

interface EntityRepository<E: Entity> {

    fun save(entity: E): E
    fun save(entities: Iterable<E>): List<E> = entities.map(this::save)

    fun findById(dubdbId: String): E?

}

interface MovieRepository: EntityRepository<Movie>
interface ActorRepository: EntityRepository<Actor>
interface DubberRepository: EntityRepository<Dubber> {
    fun findMostRecent(limit: Int): List<Dubber>
}
interface DubbedEntityRepository: EntityRepository<DubbedEntity> {
    fun findMostCommonMovies(limit: Int): List<MovieRef>
    fun findMostCommonDubbers(limit: Int): List<DubberRef>
    fun findMostCommonActors(limit: Int): List<ActorRef>

    fun countDubbers(dubbers: List<DubberRef>): Map<DubberRef, Int>
    fun countDubber(dubber: DubberRef) = countDubbers(listOf(dubber))[dubber]

}
