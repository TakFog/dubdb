package takutility.dubdb

import com.mongodb.client.MongoClients
import kotlinx.coroutines.Runnable
import mu.KotlinLogging
import takutility.dubdb.db.*
import takutility.dubdb.service.TraktImpl
import takutility.dubdb.service.WikidataImpl
import takutility.dubdb.service.wikiapi.WikiApiImpl
import takutility.dubdb.wiki.WikiHtmlPageLoader
import takutility.dubdb.wiki.WikiPageLoader
import kotlin.io.path.Path

var flush: Runnable? = null
private val logger = KotlinLogging.logger {}

fun memRepositoryFromConfig(config: Config): RepositorySet {
    val movie = MemMovieRepository()
    val actor = MemActorRepository()
    val dubber = MemDubberRepository()
    val dubbedEntity = MemDubbedEntityRepository()

    val root = Path(config.memdb.folder)
    val dbToFile = mapOf(
        movie to root.resolve("movie.json"),
        actor to root.resolve("actor.json"),
        dubber to root.resolve("dubber.json"),
        dubbedEntity to root.resolve("dubbedEntity.json"),
    )

    dbToFile.forEach { (db, file) ->

        db.loadFromFile(file)
    }

    val saveDbs = Runnable {
        logger.info { "Saving mem DBs" }
        dbToFile.forEach { (db, file) ->
            db.saveToFile(file)
            logger.debug { "$file saved" }
        }
        logger.info { "Mem DB saved" }
    }
    flush = saveDbs

    Runtime.getRuntime().addShutdownHook(Thread(saveDbs));
    return RepositorySet(movie, actor, dubber, dubbedEntity)
}

fun mongoRepositoryFromConfig(config: Config): RepositorySet {
    val url = config.mongodb.url.let {
        if (it.startsWith("mongodb://") || it.startsWith("mongodb+srv://")) it
        else "mongodb://$it"
    }
    val client = MongoClients.create(url)
    val database = client.getDatabase(config.mongodb.db)

    val closeDb = Runnable {
        logger.info { "Closing Mongo client" }
        client.close()
    }
    Runtime.getRuntime().addShutdownHook(Thread(closeDb))

    return mongoRepositorySet(database)
}

fun contextFromConfig(config: Config = loadConfig()): DubDbContext {
    val db = when (config.dbType) {
        DbType.MEM -> memRepositoryFromConfig(config)
        DbType.MONGO -> mongoRepositoryFromConfig(config)
    }

    val trakt = TraktImpl(config)
    val wikiApi = WikiApiImpl()
    val wikidata = WikidataImpl()
    WikiHtmlPageLoader.fromConfig(config)

    return DubDbContextBase(
        movieDb = db.movie,
        actorDb = db.actor,
        dubberDb = db.dubber,
        dubEntityDb = db.dubEntity,
        trakt = trakt,
        wikiApi = wikiApi,
        wikidata = wikidata,
        wikiHtmlLoader = WikiHtmlPageLoader.get(),
        wikiPageLoader = WikiPageLoader.fromConfig(wikiApi, config),
        config = config,
    )
}
