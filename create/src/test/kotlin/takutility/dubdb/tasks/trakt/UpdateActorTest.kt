package takutility.dubdb.tasks.trakt

import com.uwetrottmann.trakt5.entities.Person
import com.uwetrottmann.trakt5.entities.PersonIds
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.verifyNoInteractions
import takutility.dubdb.entities.Actor
import takutility.dubdb.entities.Source.*
import takutility.dubdb.entities.SourceIds
import takutility.dubdb.service.SearchResults
import takutility.dubdb.service.Trakt

internal abstract class UpdateActorBaseTest {
    lateinit var task: UpdateActor

    @Test
    open fun withTrakt() {
        val actor = Actor(name = "With Trakt",
            ids = SourceIds.of(
                WIKIDATA to "Q165219",
                WIKI_EN to "Robert_Downey_Jr.",
                TRAKT to "15987"
            )
        )

        assertEquals(15987, actor.traktId)
        task.run(actor)

        assertEquals(15987, actor.traktId)
        assertEquals(3, actor.ids.size)
    }

    @Test
    open fun noImdb() {
        val actor = Actor(name = "No Imdb",
            ids = SourceIds.of(
                WIKIDATA to "Q165219",
                WIKI_EN to "Robert_Downey_Jr.",
            )
        )

        assertNull(actor.traktId)
        task.run(actor)

        assertNull(actor.traktId)
        assertEquals(2, actor.ids.size)
    }

    @Test
    fun robertDowneyJr() {
        val actor = Actor(name = "Robert Downey Jr.",
            ids = SourceIds.of(
                IMDB to "nm0000375",
                WIKIDATA to "Q165219",
                WIKI_EN to "Robert_Downey_Jr.",
            )
        )

        assertNull(actor.traktId)
        task.run(actor)

        assertEquals(15987, actor.traktId)
        assertEquals(4, actor.ids.size)
    }

}

internal class UpdateActorTest: UpdateActorBaseTest() {
    lateinit var trakt: Trakt

    @BeforeEach
    fun setup() {
        trakt = mockTrakt {
            on { searchImdb("nm0000375") } doReturn downeyJr
        }
        task = UpdateActor(trakt)
    }

    override fun withTrakt() {
        super.withTrakt()
        verifyNoInteractions(trakt)
    }

    override fun noImdb() {
        super.noImdb()
        verifyNoInteractions(trakt)
    }
}

@Disabled
internal class UpdateActorIntegrationTest: UpdateActorBaseTest() {

    @BeforeEach
    fun setup() {
        task = UpdateActor(traktImpl)
    }
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