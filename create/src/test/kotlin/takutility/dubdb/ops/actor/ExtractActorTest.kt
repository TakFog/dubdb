package takutility.dubdb.ops.actor

import com.uwetrottmann.trakt5.entities.Person
import com.uwetrottmann.trakt5.entities.PersonIds
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import takutility.dubdb.DubDbContext
import takutility.dubdb.TestContext
import takutility.dubdb.db.MemActorRepository
import takutility.dubdb.db.MemDubbedEntityRepository
import takutility.dubdb.entities.Actor
import takutility.dubdb.entities.Source
import takutility.dubdb.entities.SourceIds
import takutility.dubdb.service.SearchResults
import takutility.dubdb.tasks.trakt.mockTrakt
import takutility.dubdb.tasks.trakt.newResult

internal class ExtractActorTest {
    lateinit var actorDb: MemActorRepository
    lateinit var dubEntityDb: MemDubbedEntityRepository
    lateinit var ctx: DubDbContext
    lateinit var op: ExtractActor

    @BeforeEach
    fun setUp() {
        dubEntityDb = MemDubbedEntityRepository()
        actorDb = MemActorRepository()
        val trakt = mockTrakt {
            on { searchImdb("nm0000375") } doReturn downeyJr
        }
        ctx = TestContext.mocked {
            it.actorDb = actorDb
            it.dubEntityDb = dubEntityDb
            it.trakt = trakt
        }
        op = ExtractActor(ctx)
    }

    @Test
    fun robertDowneyJr_savedActor() {
        val actor = op.run(page("Robert_Downey_Jr."))

        assertNotNull(actor.id)
        val id = actor.id!!
        val dbActor = ctx.actorDb.findById(id)
        assertEquals(actor, dbActor)
    }

    @Test
    fun robertDowneyJr_updateActor() {
        val name = "Robert Downey Jr. Test"
        val title = "Robert_Downey_Jr."

        val old = actorDb.save(Actor(name, ids = SourceIds.of(Source.WIKI to title, Source.MONDO_DOPPIATORI to "robert-downey-jr")))
        val oldIds = old.ids.toImmutable()

        val actor = op.run(page(title))

        oldIds.forEach { assertEquals(it, actor.ids[it.source], "old ${it.source}") }
        assertEquals(name, actor.name)
        assertNotNull(actor.id)
        val id = actor.id!!
        val dbActor = ctx.actorDb.findById(id)
        assertEquals(actor, dbActor)
    }

    @Test
    fun robertDowneyJr_ids() {
        val actor = op.run(page("Robert_Downey_Jr."))

        val ids = SourceIds.of(
            Source.WIKI to "Robert_Downey_Jr.",
            Source.TRAKT to "15987",
            Source.IMDB to "nm0000375",
            Source.WIKIDATA to "Q165219",
            Source.WIKI_EN to "Robert_Downey_Jr.",
        )
        ids.forEach { assertEquals(it, actor.ids[it.source]) }
    }

    @Test
    fun robertDowneyJr_photo() {
        val actor = op.run(page("Robert_Downey_Jr."))

        assertEquals("Robert_Downey_Jr_2014_Comic_Con_(cropped).jpg", actor.ids[Source.WIKIMEDIA]?.id, "photo")
    }

    fun page(title: String) = ctx.wikiPageLoader.page(title)

}

private val downeyJr = SearchResults(listOf(newResult {
    type = "person"
    person = Person().apply {
        name = "Robert Downey Jr."
        ids = PersonIds().apply {
            slug = "robert-downey-jr"
            trakt = 15987
            imdb = "nm0000375"
            tmdb = 3223
        }
    }
}))