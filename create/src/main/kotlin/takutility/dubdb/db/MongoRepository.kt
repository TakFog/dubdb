package takutility.dubdb.db

import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.*
import org.bson.Document
import org.bson.types.ObjectId
import takutility.dubdb.db.codec.codecRegistry
import takutility.dubdb.entities.*
import takutility.dubdb.util.Counter
import takutility.dubdb.util.countInstances
import takutility.dubdb.util.isBefore

private fun filterById(id: SourceId, field: String = "ids") =
    if (id.source == Source.DUBDB && field == "ids")
        Filters.eq("_id", ObjectId(id.id))
    else
        Filters.eq("$field.${id.source.name}", id.id)

private fun <E: EntityRef> findBySources(collection: MongoCollection<E>, ids: SourceIds, field: String = "ids"): List<E> {
    val uniqFilters = sequenceOf(Source.TRAKT, Source.IMDB, Source.WIKIDATA)
        .mapNotNull { s -> ids[s]?.let{ filterById(it, field) } }
        .toList()
    if (uniqFilters.isNotEmpty()) {
        val uniqRes = collection.find(Filters.or(uniqFilters)).toList()
        if (uniqRes.isNotEmpty()) return uniqRes
    }

    val subFilters = sequenceOf(Source.WIKI, Source.WIKI_EN, Source.WIKI_MISSING)
        .mapNotNull { s -> ids[s]?.let{ filterById(it, field) } }
        .toList()
    if (subFilters.isEmpty()) return emptyList()

    return collection.find(Filters.or(subFilters)).asSequence()
        .filter { it.ids.isCompatible(ids) }
        .toList()
}

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

    override fun save(entities: Iterable<E>): List<E> {
        val entityList = entities.toList()
        if (entityList.isEmpty()) return emptyList()

        val inserts = mutableListOf<E>()
        val bulkWrites = mutableListOf<WriteModel<E>>()

        for (entity in entityList) {
            val id = entity.id
            if (id == null || !ObjectId.isValid(id)) {
                entity.id = ObjectId().toHexString()
                inserts.add(entity)
            } else {
                // Prepara l'operazione di replace (upsert) per il BulkWrite
                val filter = Filters.eq("_id", ObjectId(id))
                val replaceModel = ReplaceOneModel(filter, entity, ReplaceOptions().upsert(true))
                bulkWrites.add(replaceModel)
            }
        }

        if (inserts.isNotEmpty()) {
            collection.insertMany(inserts)
        }

        if (bulkWrites.isNotEmpty()) {
            collection.bulkWrite(bulkWrites, BulkWriteOptions().ordered(false))
        }

        return entityList
    }


    override fun findById(dubdbId: String): E? {
        if (!ObjectId.isValid(dubdbId)) return null
        return collection.find(Filters.eq("_id", ObjectId(dubdbId))).firstOrNull()
    }

    override fun findBySource(id: SourceId): List<E> {
        if (id.source == Source.DUBDB) return findById(id.id)
            ?.let { listOf(it) } ?: emptyList()

        return collection.find(filterById(id)).toList()
    }

    override fun findBySources(ids: SourceIds): List<E> = findBySources(collection, ids)
}

class MongoMovieRepository(collection: MongoCollection<Movie>) :
    MongoRepository<Movie>(collection), MovieRepository {
    constructor(database: MongoDatabase, collectionName: String = "movies") :
        this(database.getCollection(collectionName, Movie::class.java).withCodecRegistry(codecRegistry))
}

class MongoActorRepository(collection: MongoCollection<Actor>) :
    MongoRepository<Actor>(collection), ActorRepository {
    constructor(database: MongoDatabase, collectionName: String = "actors") :
        this(database.getCollection(collectionName, Actor::class.java).withCodecRegistry(codecRegistry))
}

class MongoDubberRepository(collection: MongoCollection<Dubber>) :
    MongoRepository<Dubber>(collection), DubberRepository {
    constructor(database: MongoDatabase, collectionName: String = "dubbers") :
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
    constructor(database: MongoDatabase, collectionName: String = "dubbedentities") :
        this(database.getCollection(collectionName, DubbedEntity::class.java).withCodecRegistry(codecRegistry))

    private inline fun <reified T : EntityRef> findMostCommon(fieldName: String, limit: Int): List<T> {
        val filter = Filters.and(
            Filters.exists(fieldName),
            Filters.ne(fieldName, null),
            Filters.or(
                Filters.eq("$fieldName.parsed", false),
                Filters.exists("$fieldName.parsed", false)
            )
        )

        val pipeline = listOf(
            Aggregates.match(filter),
            Aggregates.group(
                // Raggruppa per la struttura dell'entità annidata
                Document("name", "$$fieldName.name").append("ids", "$$fieldName.ids"),
                Accumulators.sum("count", 1),
                // Mantiene il documento originale del riferimento per la deserializzazione
                Accumulators.first("entity", "$$fieldName")
            ),
            Aggregates.sort(Sorts.descending("count")),
            Aggregates.limit(limit),
            Aggregates.replaceRoot("\$entity")
        )

        return collection
            .withCodecRegistry(codecRegistry)
            .aggregate(pipeline, T::class.java)
            .toList()
    }

    override fun findMostCommonMovies(limit: Int): List<MovieRef> = findMostCommon("movie", limit)
    override fun findMostCommonDubbers(limit: Int): List<DubberRef> = findMostCommon("dubber", limit)
    override fun findMostCommonActors(limit: Int): List<ActorRef> = findMostCommon("actor", limit)

    private fun findByRef(ref: EntityRef, field: String): List<DubbedEntity>
        = findBySources(collection, ref.ids, "$field.ids")

    override fun findByRef(ref: DubberRef): List<DubbedEntity> {
        return findByRef(ref, "dubber").filter { it.dubber?.matches(ref) ?: false }.toList()
    }

    override fun findByRef(ref: MovieRef): List<DubbedEntity> {
        return findByRef(ref, "movie").filter { it.movie.matches(ref) }.toList()
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
