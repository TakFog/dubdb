package takutility.dubdb.db

import takutility.dubdb.entities.*
import java.util.*

open class MemRepository<E: Entity>: EntityRepository<E> {
    val db = mutableMapOf<String, E>()

    override fun save(entity: E): E {
        var id = entity.id
        if (id == null) {
            id = UUID.randomUUID().toString()
            entity.id = id
        }
        db[id] = entity
        return entity
    }

    override fun findById(dubdbId: String): E? = db[dubdbId]
}

class MemMovieRepository: MemRepository<Movie>(), MovieRepository
class MemActorRepository: MemRepository<Actor>(), ActorRepository
class MemDubberRepository: MemRepository<Dubber>(), DubberRepository
class MemDubbedEntityRepository: MemRepository<DubbedEntity>(), DubbedEntityRepository