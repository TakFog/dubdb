package takutility.dubdb.tasks.internal

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import takutility.dubdb.TestContext
import takutility.dubdb.assertEqualsUnordered
import takutility.dubdb.db.MemActorRepository
import takutility.dubdb.entities.*
import takutility.dubdb.tasks.TaskResult
import java.time.Instant

internal class FindMissingActorsTest {
    lateinit var task: FindMissingActors
    lateinit var actorDb: MemActorRepository

    @BeforeEach
    fun before() {
        actorDb = MemActorRepository()
        val context = TestContext.mocked {
            it.actorDb = actorDb
        }
        task = FindMissingActors(context)
    }

    fun run(vararg entities: DubbedEntity): TaskResult = task.run(listOf(*entities))

    @Test
    fun empty() {
        val result = task.run(listOf())
        assertTrue(result.isEmpty())
    }

    @Test
    fun missing() {
        val actor = Actor("name", ids = SourceIds.of(Source.WIKI to "wikiname"))
        val entity = DubbedEntity("entity", movie = movieRefOf(), actor = actor.toRef())

        val result = run(entity)

        assertEquals(listOf(entity), result.dubbedEntities)
        assertEquals(listOf(actor), result.actors)
    }

    @Test
    fun unparsed() {
        var actor = Actor("name", ids = SourceIds.of(Source.WIKI to "wikiname"))
        val entity = DubbedEntity("entity", movie = movieRefOf(), actor = actor.toRef())
        actor = actorDb.save(actor)

        val result = run(entity)

        assertEquals(listOf(entity), result.dubbedEntities)
        assertEquals(listOf(actor), result.actors)
    }

    @Test
    fun parsed() {
        val actor = Actor("name", ids = SourceIds.of(Source.WIKI to "wikiname"), parseTs = Instant.now())
        val entity = DubbedEntity("entity", movie = movieRefOf(), actor = actor.toRef())
        actorDb.save(actor)

        val result = run(entity)

        assertTrue(result.isEmpty())
    }

    @Test
    fun multimatch() {
        val wiki = SourceId(Source.WIKI, "wikiname")
        val trakt = SourceId(Source.TRAKT, "12345")
        val actor = actorDb.save(Actor("name", ids = SourceIds.of(wiki, trakt)))
        val entity1 = DubbedEntity("entity", movie = movieRefOf(), actor = ActorRefImpl(ids = SourceIds.of(wiki)))
        val entity2 = DubbedEntity("entity", movie = movieRefOf(), actor = ActorRefImpl(ids = SourceIds.of(trakt)))

        val result = run(entity1, entity2)

        assertEqualsUnordered(listOf(entity1, entity2), result.dubbedEntities)
        assertEquals(listOf(actor), result.actors)
    }

    @Test
    fun multiSaved() {
        val wiki = SourceId(Source.WIKI, "wikiname")
        val trakt1 = SourceId(Source.TRAKT, "12345")
        val trakt2 = SourceId(Source.TRAKT, "6789")
        val actor1 = actorDb.save(Actor("name", ids = SourceIds.of(wiki, trakt1)))
        val actor2 = actorDb.save(Actor("name", ids = SourceIds.of(wiki, trakt2)))
        val entity = DubbedEntity("entity", movie = movieRefOf(), actor = ActorRefImpl(ids = SourceIds.of(wiki)))

        val result = run(entity)

        assertEquals(listOf(entity), result.dubbedEntities)
        assertEqualsUnordered(listOf(actor1, actor2), result.actors)
    }

    @Test
    fun compatible() {
        val wiki = SourceId(Source.WIKI, "wikiname")
        val wikiEn = SourceId(Source.WIKI_EN, "en_wikiname")
        val trakt = SourceId(Source.TRAKT, "12345")
        val entity1 = DubbedEntity("entity", movie = movieRefOf(), actor = ActorRefImpl(ids = SourceIds.of(wiki, wikiEn)))
        val entity2 = DubbedEntity("entity", movie = movieRefOf(), actor = ActorRefImpl(ids = SourceIds.of(trakt, wikiEn)))

        val result = run(entity1, entity2)

        assertEqualsUnordered(listOf(entity1, entity2), result.dubbedEntities)
        assertEquals(1, result.actors?.size)
        assertEquals(SourceIds.of(wiki, trakt, wikiEn), result.actors?.get(0)?.ids)
    }

    @Test
    fun allTypes() {
        var unparsed = Actor("name unp", ids = SourceIds.of(Source.WIKI to "unparsed"))
        val parsed = Actor("name p", ids = SourceIds.of(Source.WIKI to "parsed"), parseTs = Instant.now())
        val missing = Actor("name m", ids = SourceIds.of(Source.WIKI to "missing"))
        val entityUp = DubbedEntity("entity up", movie = movieRefOf(), actor = unparsed.toRef())
        val entityP = DubbedEntity("entity p", movie = movieRefOf(), actor = parsed.toRef())
        val entityM = DubbedEntity("entity m", movie = movieRefOf(), actor = missing.toRef())
        unparsed = actorDb.save(unparsed)
        actorDb.save(parsed)

        val result = run(entityP, entityUp, entityM)

        assertEqualsUnordered(listOf(entityUp, entityM), result.dubbedEntities)
        assertEqualsUnordered(listOf(unparsed, missing), result.actors)
    }

}