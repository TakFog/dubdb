package takutility.dubdb.ops.movie

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import takutility.dubdb.DubDbContext
import takutility.dubdb.TestContext
import takutility.dubdb.db.MemDubbedEntityRepository
import takutility.dubdb.db.MemMovieRepository
import takutility.dubdb.entities.*

internal class ExtractMovieTest {
    lateinit var movieDb: MemMovieRepository
    lateinit var dubEntityDb: MemDubbedEntityRepository
    lateinit var ctx: DubDbContext
    lateinit var op: ExtractMovie

    @BeforeEach
    fun setUp() {
        dubEntityDb = MemDubbedEntityRepository()
        movieDb = MemMovieRepository()
        ctx = TestContext.mocked {
            it.movieDb = movieDb
            it.dubEntityDb = dubEntityDb
        }
        op = ExtractMovie(ctx)
    }

    @Test
    fun ultron_saveMovie() {
        val movie = op.run(page("Avengers:_Age_of_Ultron"))

        assertNotNull(movie.id)
        val id = movie.id!!
        val dbMovie = ctx.movieDb.findById(id)
        assertEquals(movie, dbMovie)
    }

    @Test
    fun ultron_updateMovie() {
        val name = "Ultron Test"
        val title = "Avengers:_Age_of_Ultron"

        val old = movieDb.save(Movie(name, ids = SourceIds.of(Source.WIKI to title, Source.UNK to "avengers-ultron")))
        val oldIds = old.ids.toImmutable()

        val movie = op.run(page(title))

        oldIds.forEach { assertEquals(it, movie.ids[it.source], "old ${it.source}") }
        assertEquals(name, movie.name)
        assertNotNull(movie.id)
        val id = movie.id!!
        val dbMovie = ctx.movieDb.findById(id)
        assertEquals(movie, dbMovie)
    }

    @Test
    fun ultron_ids() {
        val movie = op.run(page("Avengers:_Age_of_Ultron"))

        val ids = SourceIds.of(
            Source.WIKI to "Avengers:_Age_of_Ultron",
            Source.MONDO_DOPPIATORI to "doppiaggio/film1/avengers-ageofultron.htm",
            Source.IMDB to "tt2395427",
            Source.WIKIDATA to "Q14171368",
            Source.WIKI_EN to "Avengers:_Age_of_Ultron",
            Source.TRAKT to "71938"
        )
        ids.forEach { assertEquals(it, movie.ids[it.source]) }
    }

    @Test
    fun valerian_ids() {
        val movie = op.run(page("Valerian_e_la_città_dei_mille_pianeti"))

        val ids = SourceIds.of(
            Source.WIKI to "Valerian_e_la_città_dei_mille_pianeti",
            Source.MONDO_DOPPIATORI to "doppiaggio/film1/valerianelacittadeimillepianeti.htm",
            Source.IMDB to "tt2239822",
            Source.WIKIDATA to "Q20926273",
            Source.WIKI_EN to "Valerian_and_the_City_of_a_Thousand_Planets",
            Source.TRAKT to "220423"
        )
        ids.forEach { assertEquals(it, movie.ids[it.source]) }
    }

    @Test
    fun ultron_entities_sources() {
        val movie = op.run(page("Avengers:_Age_of_Ultron"))

        val entities = dubEntityDb.db.values
        entities.forEach {
            assertEquals(movie.id, it.movie.id, "$it invalid movie")
        }
        entities.find { it.name == "Thor" }?.sources?.apply {
            forEach { assertEquals(movie.wiki, it.sourceId) }
            assertEquals(3, size)
            assertEquals(setOf(DataSource.MOVIE_DUB, DataSource.MOVIE_ORIG, DataSource.TRAKT), map { it.dataSource }.toSet())
            assertEquals(setOf(movie.wiki), filter { it.dataSource != DataSource.TRAKT }.map { it.sourceId }.toSet())
        }
    }

    @Test
    fun ultron_entities_single() {
        val movie = op.run(page("Avengers:_Age_of_Ultron"))

        val entities = dubEntityDb.db.values

        assertEntity(entities, "Thor", "Chris Hemsworth", "Massimiliano Manfredi")
        assertEntity(entities, "Maria Hill", "Cobie Smulders", "Federica De Bortoli")
        assertEntity(entities, "Heimdall", "Idris Elba", "Alberto Angrisano")
        //from trakt
        assertActor(entities, "Cooper Barton", "Ben Sakamoto")
        assertActor(entities, "Klaue's Mercenary", "Bentley Kalu")
    }

    @Test
    fun ultron_entities_multi() {
        val movie = op.run(page("Avengers:_Age_of_Ultron"))

        val entities = dubEntityDb.db.values

        assertEntity(entities, "Tony Stark", "Robert Downey Jr.", "Angelo Maggi")
        assertEntity(entities, "Iron Man", "Robert Downey Jr.", "Angelo Maggi")
        assertEntity(entities, "Clint Barton", "Jeremy Renner", "Christian Iansante")
        assertEntity(entities, "Occhio di Falco", "Jeremy Renner", "Christian Iansante")
        assertEntity(entities, "Bruce Banner", "Mark Ruffalo", "Riccardo Rossi")
        assertDubber(entities, "Hulk", "Riccardo Rossi")
        assertActor(entities, "Hulk", "Mark Ruffalo")
        assertActor(entities, "Hulk", "Lou Ferrigno")
        assertEntity(entities, "Visione", "Paul Bettany", "Nino D'Agata")
        assertEntity(entities, "J.A.R.V.I.S.", "Paul Bettany", "Nino D'Agata")
    }

    @Test
    fun valerian_entities() {
        val movie = op.run(page("Valerian_e_la_città_dei_mille_pianeti"))

        val entities = dubEntityDb.db.values

        assertEntity(entities, "Maggiore Valerian", "Dane DeHaan", "Davide Perino")
        assertEntity(entities, "Sergente Laureline", "Cara Delevingne", "Valentina Favazza")
        assertEntity(entities, "Comandante Arün Filitt", "Clive Owen", "Fabio Boccanera")
        assertEntity(entities, "Bubble", "Rihanna", "Domitilla D'Amico")
        assertActor(entities, "Igon Siruss", "John Goodman")
    }

    fun page(title: String) = ctx.wikiPageLoader.page(title)

}

fun assertActor(entities: Collection<DubbedEntity>, name: String, actor: String) = assertEntity(entities, name, actor, null)
fun assertDubber(entities: Collection<DubbedEntity>, name: String, dubber: String) = assertEntity(entities, name, null, dubber)

fun assertEntity(entities: Collection<DubbedEntity>, name: String, actor: String?, dubber: String?) {
    actor?.let { a ->
        val entity = entities.find { name == it.name && it.actor?.name == a }
        assertNotNull(entity, "$name - actor $a - not found")
    }
    dubber?.let { d ->
        val entity = entities.find { name == it.name && it.dubber?.name == d }
        assertNotNull(entity, "$name - dubber $d - not found")
    }
    if (dubber != null && actor != null) {
        val entity = entities.find { name == it.name && it.actor?.name == actor && it.dubber?.name == dubber }
        assertNotNull(entity, "$name - actor $actor - dubber $dubber - not found")
    }
}