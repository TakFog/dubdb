package takutility.dubdb

import mu.KotlinLogging
import takutility.dubdb.db.*
import takutility.dubdb.ops.dubber.Dubbers
import takutility.dubdb.ops.movie.Movies
import takutility.dubdb.service.TraktImpl
import takutility.dubdb.service.WikiApiImpl
import takutility.dubdb.service.WikidataImpl
import takutility.dubdb.wiki.WikiPageLoader
import kotlin.io.path.Path

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

    Runtime.getRuntime().addShutdownHook(object : Thread() {
        override fun run() {
            logger.info { "Saving mem DBs" }
            dbToFile.forEach { (db, file) ->
                db.saveToFile(file)
                logger.debug { "$file saved" }
            }
            logger.info { "Mem DB saved" }
        }
    })
    return RepositorySet(movie, actor, dubber, dubbedEntity)
}

fun contextFromConfig(config: Config = loadConfig()): DubDbContext {
    val db = memRepositoryFromConfig(config)

    val trakt = TraktImpl(config)
    val wikiApi = WikiApiImpl()
    val wikidata = WikidataImpl()
    WikiPageLoader.fromConfig(config)

    return DubDbContextBase(
        movieDb = db.movie,
        actorDb = db.actor,
        dubberDb = db.dubber,
        dubEntityDb = db.dubEntity,
        trakt = trakt,
        wikiApi = wikiApi,
        wikidata = wikidata,
        wikiPageLoader = WikiPageLoader.get(),
        config = config,
    )
}



private val logger = KotlinLogging.logger {}

fun main() {
    logger.info { "Starting DubDB data extraction" }

    val config = loadConfig()
    val context = contextFromConfig(config)

    var i = 0
    while (true) {
        i += 1
        logger.info { "Iteration $i" }
        context[Movies::class].run(20)
        context[Dubbers::class].run(20)
    }
}