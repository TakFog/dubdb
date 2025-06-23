package takutility.dubdb.db

import takutility.dubdb.entities.*
import takutility.dubdb.util.Counter

interface EntityRepository<E: Entity> {

    fun save(entity: E): E
    fun save(entities: Iterable<E>): List<E> = entities.map(this::save)

    fun findById(dubdbId: String): E?
    fun findBySource(id: SourceId): List<E>
    fun findBySources(ids: SourceIds): List<E>
    fun findBySources(entity: EntityRef) = findBySources(entity.ids)
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
    fun findByRef(ref: MovieRef): List<DubbedEntity>

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("updateDubberRefIds")
    fun updateRefIds(refs: List<DubberRef>)

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("updateActorRefIds")
    fun updateRefIds(refs: List<ActorRef>)
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("updateMovieRefIds")
    fun updateRefIds(refs: List<MovieRef>)

    fun countEntitiesBySource(source: Source, ids: Iterable<String>): Counter<String>

    fun countDubbers(dubbers: List<DubberRef>): Counter<DubberRef>
    fun countDubber(dubber: DubberRef) = countDubbers(listOf(dubber))[dubber]

    fun countActors(actors: List<ActorRef>): Counter<ActorRef>
    fun countActor(actor: ActorRef) = countActors(listOf(actor))[actor]

}

data class RepositorySet(
    val movie: MovieRepository,
    val actor: ActorRepository,
    val dubber: DubberRepository,
    val dubEntity: DubbedEntityRepository,
) {
    fun toSet() = setOf<EntityRepository<*>>(movie, actor, dubber, dubEntity)
}