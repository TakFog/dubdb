package takutility.dubdb.db

import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.client.model.Sorts
import org.bson.conversions.Bson
import org.bson.types.ObjectId
import takutility.dubdb.db.codec.codecRegistry
import takutility.dubdb.entities.*
import takutility.dubdb.util.Counter
import takutility.dubdb.util.countInstances
import takutility.dubdb.util.isBefore

open class MongoRepository<E : Entity>(
    protected val collection: MongoCollection<E>
) : EntityRepository<E> {

    constructor(database: MongoDatabase, collectionName: String, clazz: Class<E>) : this(
        database.getCollection(collectionName, clazz).withCodecRegistry(codecRegistry)
    )

    override fun save(entity: E): E {
        var id = entity.id
        if (id == null || !ObjectId.isValid(id)) {
            val objectId = ObjectId()
            id = objectId.toHexString()
            entity.id = id
            collection.insertOne(entity)
        } else {
            collection.replaceOne(Filters.eq("_id", ObjectId(id)), entity, ReplaceOptions().upsert(true))
        }
        return entity
    }

    override fun findById(dubdbId: String): E? {
        if (!ObjectId.isValid(dubdbId)) return null
        return collection.find(Filters.eq("_id", ObjectId(dubdbId))).firstOrNull()
    }

    override fun findBySource(id: SourceId): List<E> {
        val filter = if (id.source == Source.DUBDB) {
            if (!ObjectId.isValid(id.id)) return emptyList()
            Filters.eq("_id", ObjectId(id.id))
        } else {
            Filters.eq("ids.${id.source.name}", id.id)
        }
        return collection.find(filter).into(mutableListOf())
    }

    override fun findBySources(ids: SourceIds): List<E> {
        sequenceOf(Source.TRAKT, Source.IMDB, Source.WIKIDATA)
            .filter { it in ids }
            .map { findBySource(ids[it]!!) }
            .filter { it.isNotEmpty() }
            .firstOrNull()?.let { return it }

        val subIds = SourceIds.of(ids
            .filter { it.source in listOf(Source.WIKI, Source.WIKI_EN, Source.WIKI_MISSING) })
        if (subIds.isEmpty()) return emptyList()

        val wikiFilters = subIds.map { Filters.eq("ids.${it.source.name}", it.id) }
        val filter = Filters.or(wikiFilters)
        return collection.find(filter).asSequence()
            .filter { it.ids.isCompatible(subIds) }
            .toList()
    }
}

class MongoMovieRepository(collection: MongoCollection<Movie>) :
    MongoRepository<Movie>(collection), MovieRepository {
    constructor(database: MongoDatabase, collectionName: String = "movie") :
        this(database.getCollection(collectionName, Movie::class.java).withCodecRegistry(codecRegistry))
}

class MongoActorRepository(collection: MongoCollection<Actor>) :
    MongoRepository<Actor>(collection), ActorRepository {
    constructor(database: MongoDatabase, collectionName: String = "actor") :
        this(database.getCollection(collectionName, Actor::class.java).withCodecRegistry(codecRegistry))
}

class MongoDubberRepository(collection: MongoCollection<Dubber>) :
    MongoRepository<Dubber>(collection), DubberRepository {
    constructor(database: MongoDatabase, collectionName: String = "dubber") :
        this(database.getCollection(collectionName, Dubber::class.java).withCodecRegistry(codecRegistry))

    override fun findMostRecent(limit: Int, unparsed: Boolean, updated: Boolean): List<Dubber> {
        return collection.find(Filters.ne("lastUpdate", null))
            .sort(Sorts.descending("lastUpdate"))
            .asSequence()
            .filter { (unparsed && !it.isParsed) || (updated && it.parseTs.isBefore(it.lastUpdate)) }
            .take(limit)
            .toList()
    }
}

class MongoDubbedEntityRepository(collection: MongoCollection<DubbedEntity>) :
    MongoRepository<DubbedEntity>(collection), DubbedEntityRepository {
    constructor(database: MongoDatabase, collectionName: String = "dubbed") :
        this(database.getCollection(collectionName, DubbedEntity::class.java).withCodecRegistry(codecRegistry))

    private fun <T : EntityRef> findMostCommon(limit: Int, transform: (DubbedEntity) -> T?): List<T> {
        return collection.find().asSequence()
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

    override fun findByRef(ref: DubberRef): List<DubbedEntity> {
        val filters = mutableListOf<Bson>()
        ref.id?.let { if (ObjectId.isValid(it)) filters.add(Filters.eq("dubber.ids.DUBDB", it)) }
        ref.ids.forEach {
            if (it.source != Source.DUBDB) {
                filters.add(Filters.eq("dubber.ids.${it.source.name}", it.id))
            }
        }
        val cursor = if (filters.isNotEmpty()) collection.find(Filters.or(filters)) else collection.find()
        return cursor.asSequence().filter { it.dubber?.matches(ref) ?: false }.toList()
    }

    override fun findByRef(ref: MovieRef): List<DubbedEntity> {
        val filters = mutableListOf<Bson>()
        ref.id?.let { if (ObjectId.isValid(it)) filters.add(Filters.eq("movie.ids.DUBDB", it)) }
        ref.ids.forEach {
            if (it.source != Source.DUBDB) {
                filters.add(Filters.eq("movie.ids.${it.source.name}", it.id))
            }
        }
        val cursor = if (filters.isNotEmpty()) collection.find(Filters.or(filters)) else collection.find()
        return cursor.asSequence().filter { it.movie.matches(ref) }.toList()
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("updateDubberRefIds")
    override fun updateRefIds(refs: List<DubberRef>) {
        updateRefIds(refs, DubbedEntity::dubber)
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("updateActorRefIds")
    override fun updateRefIds(refs: List<ActorRef>) {
        updateRefIds(refs, DubbedEntity::actor)
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("updateMovieRefIds")
    override fun updateRefIds(refs: List<MovieRef>) {
        updateRefIds(refs, DubbedEntity::movie)
    }

    private fun <T : EntityRef> updateRefIds(refs: List<T>, extractor: (DubbedEntity) -> T?) {
        collection.find().forEach { de ->
            val subEntity = extractor.invoke(de) ?: return@forEach
            var updated = false
            refs.forEach {
                if (subEntity.matches(it)) {
                    subEntity.ids += it.ids
                    it.parsed?.apply { subEntity.parsed = this }
                    updated = true
                }
            }
            if (updated) {
                save(de)
            }
        }
    }

    override fun countEntitiesBySource(source: Source, ids: Iterable<String>): Counter<String> {
        val idSet = ids.toSet()
        return collection.find().asSequence()
            .mapNotNull { it.ids[source]?.id }
            .filter { idSet.contains(it) }
            .countInstances(ids)
    }

    override fun countDubbers(dubbers: List<DubberRef>) = countEntities(dubbers) { it.dubber }
    override fun countActors(actors: List<ActorRef>) = countEntities(actors) { it.actor }

    private fun <E : EntityRef> countEntities(entities: List<E>, getter: (DubbedEntity) -> E?): Counter<E> {
        return collection.find().asSequence()
            .mapNotNull(getter)
            .filter { !it.ids.isEmpty() }
            .flatMap { d -> entities.filter { d.matches(it) } }
            .countInstances(entities)
    }
}

fun mongoRepositorySet(database: MongoDatabase) = RepositorySet(
    movie = MongoMovieRepository(database),
    actor = MongoActorRepository(database),
    dubber = MongoDubberRepository(database),
    dubEntity = MongoDubbedEntityRepository(database),
)
