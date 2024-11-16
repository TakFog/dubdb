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

open class DubDbContextBase(
    override val movieDb: MovieRepository,
    override val actorDb: ActorRepository,
    override val dubberDb: DubberRepository,
    override val dubEntityDb: DubbedEntityRepository,
    override val trakt: Trakt,
    override val wikiApi: WikiApi,
    override val wikiPageLoader: WikiPageLoader,
): DubDbContext {
    private val objects = HashMap<KClass<*>, Any>()

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> get(clazz: KClass<T>): T {
        var inst = objects[clazz] as T?
        if (inst == null) {
            inst = createInstance(clazz)
            objects[clazz] = inst
        }
        return inst
    }

    protected operator fun <T : Any> set(clazz: KClass<T>, value: T) {
        objects[clazz] = value
    }

    private fun <T : Any> createInstance(clazz: KClass<T>): T = clazz.constructors
            .find { c ->  c.parameters.size == 1 && c.parameters[0].type.classifier == DubDbContext::class}
            ?.call(this)
            ?: throw IllegalArgumentException("No suitable constructor found")
}