package takutility.dubdb.db

import takutility.dubdb.entities.*

interface EntityRepository<E: Entity> {

    fun save(entity: E): E
    fun save(entities: Iterable<E>): List<E> = entities.map(this::save)

    fun findById(dubdbId: String): E?
    fun findBySource(id: SourceId): List<E>
    fun findBySources(ids: SourceIds): List<E>
    fun findBySources(entity: Entity) = findBySources(entity.ids)
}

interface MovieRepository: EntityRepository<Movie>
interface ActorRepository: EntityRepository<Actor>
interface DubberRepository: EntityRepository<Dubber> {
    /**
     * Finds the most recently updated dubbers in the database.
     *
     * @param limit The maximum number of recent entries to return.
     * @param unparsed If true, includes entries that have not been parsed.
     * @param updated If true, includes entries that have been updated.
     * @return A list of the most recent dubbers that match the criteria.
     */
    fun findMostRecent(limit: Int, unparsed: Boolean = true, updated: Boolean = false): List<Dubber>
}
interface DubbedEntityRepository: EntityRepository<DubbedEntity> {
    fun findMostCommonMovies(limit: Int): List<MovieRef>
    fun findMostCommonDubbers(limit: Int): List<DubberRef>
    fun findMostCommonActors(limit: Int): List<ActorRef>

    fun findByRef(ref: DubberRef): List<DubbedEntity>

    fun updateRefIds(refs: List<DubberRef>)

    fun countDubbers(dubbers: List<DubberRef>): Map<DubberRef, Int>
    fun countDubber(dubber: DubberRef) = countDubbers(listOf(dubber))[dubber] ?: 0

}
