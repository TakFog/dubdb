package takutility.dubdb.db

import okhttp3.internal.toImmutableMap
import org.bson.codecs.DecoderContext
import org.bson.codecs.EncoderContext
import org.bson.json.JsonReader
import org.bson.json.JsonWriter
import takutility.dubdb.db.codec.codecRegistry
import takutility.dubdb.entities.*
import takutility.dubdb.util.isBefore
import java.io.Writer
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import kotlin.io.path.deleteIfExists
import kotlin.io.path.moveTo
import kotlin.io.path.name

open class MemRepository<E: Entity>(private val type: Class<E>): EntityRepository<E> {
    val db = mutableMapOf<String, E>()

    fun saveToFile(file: Path) {
        if (db.isEmpty()) {
            file.deleteIfExists()
            return
        }
        Files.createDirectories(file.parent)
        val temp = file.resolveSibling(file.name + ".tmp")
        Files.newBufferedWriter(temp).use(this::saveToJson)
        temp.moveTo(file, true)
    }

    fun loadFromFile(file: Path) {
        if (Files.exists(file))
            loadFromJson(Files.readAllLines(file))
    }

    fun loadFromJson(jsons: List<String>) {
        val codec = codecRegistry.get(type)
        jsons.forEach {
            val value = codec.decode(JsonReader(it), DecoderContext.builder().build())
            save(value)
        }
    }

    fun saveToJson(writer: Writer) {
        val codec = codecRegistry.get(type)
        db.values.forEach {
            codec.encode(JsonWriter(writer), it, EncoderContext.builder().build())
            writer.write("\n")
        }
    }

    override fun save(entity: E): E {
        var id = entity.id
        if (id == null) {
            id = UUID.randomUUID().toString().replace("-","").let { it.substring(it.length - 24) }
            entity.id = id
        }
        db[id] = entity
        return entity
    }

    override fun findById(dubdbId: String): E? = db[dubdbId]

    override fun findBySource(id: SourceId): List<E> = db.values.filter { it.ids.containsId(id) }

    override fun findBySources(ids: SourceIds): List<E> {
        sequenceOf(Source.TRAKT, Source.IMDB, Source.WIKIDATA)
            .filter { it in ids }
            .map { findBySource(ids[it]!!) }
            .filter { it.isNotEmpty() }
            .firstOrNull()?.let { return it }

        val subIds = SourceIds.of(ids
            .filter { it.source in listOf(Source.WIKI, Source.WIKI_EN, Source.WIKI_MISSING) })
        return db.values.filter { it.ids.isCompatible(subIds) }
    }
}

class MemMovieRepository: MemRepository<Movie>(Movie::class.java), MovieRepository
class MemActorRepository: MemRepository<Actor>(Actor::class.java), ActorRepository
class MemDubberRepository: MemRepository<Dubber>(Dubber::class.java), DubberRepository {
    override fun findMostRecent(limit: Int, unparsed: Boolean, updated: Boolean): List<Dubber> {
        return db.values.asSequence()
            .filter { it.lastUpdate != null && ((unparsed && !it.isParsed) || (updated && it.parseTs.isBefore(it.lastUpdate))) }
            .sortedByDescending { it.lastUpdate }
            .take(limit)
            .toList()
    }
}
class MemDubbedEntityRepository: MemRepository<DubbedEntity>(DubbedEntity::class.java), DubbedEntityRepository {

    private fun <T: EntityRef> findMostCommon(limit: Int, transform: (DubbedEntity) -> T?): List<T> {
        return db.values.asSequence()
            .mapNotNull(transform)
            .filter { !it.isParsed }
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

    override fun findByRef(ref: DubberRef) = db.values.filter { it.dubber?.matches(ref) ?: false }

    override fun updateRefIds(refs: List<DubberRef>) {
        db.values.forEach { de ->
            val dubber = de.dubber ?: return@forEach
            refs.forEach {
                if (dubber.matches(it)) {
                    dubber.ids += it.ids
                    it.parsed?.apply { dubber.parsed = this }
                }
            }
        }
    }

    override fun countDubbers(dubbers: List<DubberRef>): Map<DubberRef, Int> {
        val map: MutableMap<DubberRef, Int> = dubbers.associateWithTo(mutableMapOf()) { 0 }

        db.values.asSequence()
            .mapNotNull { it.dubber }
            .flatMap { d -> dubbers.filter { d.matches(it) } }
            .forEach { map[it] = map.getOrDefault(it, 0) + 1 }

        return map.toImmutableMap()
    }
}