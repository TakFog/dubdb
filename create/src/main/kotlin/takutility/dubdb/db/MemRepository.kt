package takutility.dubdb.db

import org.bson.codecs.DecoderContext
import org.bson.codecs.EncoderContext
import org.bson.json.JsonReader
import org.bson.json.JsonWriter
import takutility.dubdb.db.codec.codecRegistry
import takutility.dubdb.entities.*
import java.io.Writer
import java.util.*

open class MemRepository<E: Entity>(private val type: Class<E>): EntityRepository<E> {
    val db = mutableMapOf<String, E>()

    fun loadFromJson(jsons: List<String>) {
        val codec = codecRegistry.get(type)
        jsons.forEach {
            val value = codec.decode(JsonReader(it), DecoderContext.builder().build())
            save(value)
        }
    }

    fun saveToJson(writer: Writer) {
        val codec = codecRegistry.get(type)
        val jsonWriter = JsonWriter(writer)
        db.values.forEach {
            codec.encode(jsonWriter, it, EncoderContext.builder().build())
            writer.write("\n")
        }
    }

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

class MemMovieRepository: MemRepository<Movie>(Movie::class.java), MovieRepository
class MemActorRepository: MemRepository<Actor>(Actor::class.java), ActorRepository
class MemDubberRepository: MemRepository<Dubber>(Dubber::class.java), DubberRepository {
    override fun findMostRecent(limit: Int): List<Dubber> {
        return db.values.asSequence()
            .filter { it.lastUpdate != null }
            .sortedByDescending { it.lastUpdate }
            .take(limit)
            .toList()
    }
}
class MemDubbedEntityRepository: MemRepository<DubbedEntity>(DubbedEntity::class.java), DubbedEntityRepository {

    private fun <T: EntityRef> findMostCommon(limit: Int, transform: (DubbedEntity) -> T?): List<T> {
        return db.values.asSequence()
            .mapNotNull(transform)
            .filter { it.id == null }
            .map { EntityIds(it) }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(limit)
            .map { it.key.entity }
    }

    override fun findMostCommonMovies(limit: Int) = findMostCommon(limit) { it.movie }
    override fun findMostCommonDubbers(limit: Int) = findMostCommon(limit) { it.dubber }
    override fun findMostCommonActors(limit: Int) = findMostCommon(limit) { it.actor }

    override fun countDubbers(dubbers: List<DubberRef>): Map<DubberRef, Int> {
        TODO("Not yet implemented")
    }
}