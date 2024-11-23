package takutility.dubdb

import takutility.dubdb.db.ActorRepository
import takutility.dubdb.db.DubbedEntityRepository
import takutility.dubdb.db.DubberRepository
import takutility.dubdb.db.MovieRepository
import takutility.dubdb.service.Trakt
import takutility.dubdb.service.WikiApi
import takutility.dubdb.wiki.WikiPageLoader
import kotlin.reflect.KClass

interface DubDbContext {
    val movieDb: MovieRepository
    val actorDb: ActorRepository
    val dubberDb: DubberRepository
    val dubEntityDb: DubbedEntityRepository
    val trakt: Trakt
    val wikiApi: WikiApi
    val wikiPageLoader: WikiPageLoader

    operator fun <T :Any> get(clazz: KClass<T>): T
}

inline fun <reified T : kotlin.Any> DubDbContext.m(): T = get(T::class)

open class DubDbContextBase(
    movieDb: MovieRepository,
    actorDb: ActorRepository,
    dubberDb: DubberRepository,
    dubEntityDb: DubbedEntityRepository,
    trakt: Trakt,
    wikiApi: WikiApi,
    wikiPageLoader: WikiPageLoader,
): DubDbContext {
    private val objects = mutableMapOf<KClass<*>, Any>()

    init {
        objects[MovieRepository::class] = movieDb
        objects[ActorRepository::class] = actorDb
        objects[DubberRepository::class] = dubberDb
        objects[DubbedEntityRepository::class] = dubEntityDb
        objects[Trakt::class] = trakt
        objects[WikiApi::class] = wikiApi
        objects[WikiPageLoader::class] = wikiPageLoader
    }

    override val movieDb: MovieRepository
        get() = get(MovieRepository::class)
    override val actorDb: ActorRepository
        get() = get(ActorRepository::class)
    override val dubberDb: DubberRepository
        get() = get(DubberRepository::class)
    override val dubEntityDb: DubbedEntityRepository
        get() = get(DubbedEntityRepository::class)
    override val trakt: Trakt
        get() = get(Trakt::class)
    override val wikiApi: WikiApi
        get() = get(WikiApi::class)
    override val wikiPageLoader: WikiPageLoader
        get() = get(WikiPageLoader::class)

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> get(clazz: KClass<T>): T {
        return objects.getOrPut(clazz) { createInstance(clazz) } as T
    }

    protected operator fun contains(clazz: KClass<*>) = objects.contains(clazz)

    protected open operator fun <T : Any> set(clazz: KClass<T>, value: T) {
        objects[clazz] = value
    }

    private fun <T : Any> createInstance(clazz: KClass<T>): T {
        return clazz.constructors
            .find { constructor ->
                constructor.parameters.size == 1 && constructor.parameters[0].type.classifier == DubDbContext::class
            }
            ?.call(this)
            ?: throw IllegalArgumentException("No suitable constructor found for class: \${clazz.simpleName}")
    }
}