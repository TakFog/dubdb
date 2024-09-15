package takutility.dubdb.db

import takutility.dubdb.entities.Entity

interface EntityRepository<E: Entity> {

    fun save(entity: Entity): E

    fun findById(dubdbId: String): E?

}