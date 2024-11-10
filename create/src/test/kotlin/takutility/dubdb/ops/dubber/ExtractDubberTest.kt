package takutility.dubdb.ops.dubber

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import takutility.dubdb.DubDbContext
import takutility.dubdb.TestContext
import takutility.dubdb.db.MemDubbedEntityRepository
import takutility.dubdb.db.MemDubberRepository
import takutility.dubdb.entities.DubbedEntity
import takutility.dubdb.entities.Source
import takutility.dubdb.entities.SourceIds

internal class ExtractDubberTest {
    lateinit var dubEntityDb: MemDubbedEntityRepository
    lateinit var ctx: DubDbContext
    lateinit var op: ExtractDubber

    @BeforeEach
    fun setUp() {
        dubEntityDb = MemDubbedEntityRepository()
        ctx = TestContext.mocked {
            it.dubberDb = MemDubberRepository()
            it.dubEntityDb = dubEntityDb
        }
        op = ExtractDubber(ctx)
    }

    @Test
    fun angeloMaggi_savedDubber() {
        val dubber = op.run(page("Angelo_Maggi"))

        assertNotNull(dubber.id)
        val id = dubber.id!!
        val dbDubber = ctx.dubberDb.findById(id)
        assertEquals(dubber, dbDubber)
    }

    @Test
    fun angeloMaggi_ids() {
        val dubber = op.run(page("Angelo_Maggi"))

        val ids = SourceIds.of(
            Source.WIKI to "Angelo_Maggi",
            Source.MONDO_DOPPIATORI to "doppiaggio/voci/vociamag.htm",
            Source.IMDB to "nm0535947",
            Source.WIKIDATA to "Q3617056",
            Source.WIKI_EN to "Angelo_Maggi",
        )
        ids.forEach { assertEquals(it, dubber.ids[it.source]) }
    }

    @Test
    fun angeloMaggi_photo() {
        val dubber = op.run(page("Angelo_Maggi"))

        assertEquals("Angelo_Maggi_20240113.jpg", dubber.ids[Source.WIKIMEDIA]?.id, "photo")
    }

    @Test
    fun angeloMaggi_entities() {
        val dubber = op.run(page("Angelo_Maggi"))


        val entities = dubEntityDb.db.values
        entities.forEach {
            assertEquals(dubber.id, it.dubber?.id, "$it invalid dubber")
            assertEquals(1, it.sources.size, "$it entity source")
            assertEquals(dubber.wiki, it.sources[0].sourceId, "$it entity source id")
        }
        assertEntity(
            entities, "Tom Hanks",
            "Lo schermo velato",
            "Cast Away",
            "Prova a prendermi",
            "The Terminal",
            "La guerra di Charlie Wilson",
            "Molto forte, incredibilmente vicino",
            "Cloud Atlas",
            "Captain Phillips - Attacco in mare aperto",
            "Saving Mr. Banks",
            "Ithaca - L'attesa di un ritorno",
            "Il ponte delle spie",
            "Sully",
            "The Circle",
            "The Post",
            "Un amico straordinario",
            "Greyhound - Il nemico invisibile",
            "Elvis",
            "30 Rock",
        )
        assertEntity(entities, "Ted Levine", "Shutter Island")
        assertEntity(entities, "Jack Coleman", "Castle")
        assertEntity(
            entities, "Ryo Nagare",
            "Il Grande Mazinga contro Getta Robot",
            "Il Grande Mazinga contro Getta Robot G",
            "UFO Robot Goldrake, il Grande Mazinga e Getta Robot G contro il Dragosauro",
        )
        assertEntity(entities, "Guile", "Street Fighter II V")
    }

    fun page(title: String) = ctx.wikiPageLoader.page(title)

}

fun assertEntity(entities: Collection<DubbedEntity>, name: String, vararg movies: String) {
    movies.forEach { movie ->
        val entity = entities.find { name == it.name && movie == it.movie.name }
        assertNotNull(entity, "$name - $movie - not found")
    }
}