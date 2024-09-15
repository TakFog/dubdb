package takutility.dubdb.db

import takutility.dubdb.entities.Entity
import takutility.dubdb.entities.Movie

open class MemRepository<E: Entity>: EntityRepository<E> {

    override fun save(entity: Entity): E {
        TODO("Not yet implemented")
    }

    override fun findById(dubdbId: String): E? {
        TODO("Not yet implemented")
    }
}

class MemMovieRepository: MemRepository<Movie>(), MovieRepository {

}