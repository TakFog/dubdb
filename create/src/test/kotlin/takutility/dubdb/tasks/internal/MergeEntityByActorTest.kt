package takutility.dubdb.tasks.internal

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import takutility.dubdb.TestContext
import takutility.dubdb.assertEqualsUnordered
import takutility.dubdb.entities.*
import takutility.dubdb.entities.DataSource.*
import takutility.dubdb.entities.Source.*
import takutility.dubdb.fromJson
import takutility.dubdb.tasks.TaskResult

internal class MergeEntityByActorTest {
    val movieRef = movieRefOf(ids = SourceIds.of(WIKI to "movie", TRAKT to "123456"))

    lateinit var task: MergeEntityByActor

    @BeforeEach
    fun before() {
        val context = TestContext.mocked()
        task = MergeEntityByActor()
    }

    fun run(vararg entities: DubbedEntity): TaskResult = task.run(listOf(*entities))

    @Test
    fun completeParsedActor() {
        val matarazzo = fromJson<ActorRef>("""{"name": "Gaten Matarazzo", "ids": {"WIKI": "Gaten_Matarazzo", "WIKIDATA": "Q26704332", "WIKI_EN": "Gaten_Matarazzo", "IMDB": "nm7140802", "WIKIMEDIA": "Gaten_Matarazzo.jpg", "TRAKT": "686045", "DUBDB": "2c244701a72e71d6becb3efd"}, "parsed": true}""")
        val fabiano = fromJson<DubberRef>("""{"name": "Mattia Fabiano", "ids": {"DUBDB": "486049ce8d6f9a9517aaeed0", "WIKI": "Mattia_Fabiano", "WIKIDATA": "Q112873540", "MONDO_DOPPIATORI": "doppiaggio/voci/vocimfab.htm", "IMDB": "nm7445018"}, "parsed": true}""")
        val id = "a0e845ff9af5face8795a255"

        val movieOrig = DubbedEntity(
            movie = movieRef,
            actor = matarazzo,
            name = "Dustin Henderson",
            sources = mutableListOf(RawData(SourceId(WIKI, "Stranger_Things"), MOVIE_ORIG, "<a href=\"/wiki/Gaten_Matarazzo\" title=\"Gaten Matarazzo\">Gaten Matarazzo</a>: Dustin Henderson")),
        )
        val trakt = DubbedEntity(
            movie = movieRef,
            actor = matarazzo,
            name = "Dustin Henderson",
            sources = mutableListOf(RawData(SourceId(TRAKT, "104439"), TRAKT_MOVIE, "Dustin Henderson")),
        )
        val fromDb = DubbedEntity(
            movie = movieRef,
            dubber = fabiano,
            name = "Gaten Matarazzo",
            ids = SourceIds.of(DUBDB to id, WIKI to "Gaten_Matarazzo"),
            sources = mutableListOf(RawData(SourceId(WIKI, "Mattia_Fabiano"), DUBBER, raw="""<a href="/wiki/Gaten_Matarazzo" title="Gaten Matarazzo">Gaten Matarazzo</a> in <i><a href="/wiki/Stranger_Things" title="Stranger Things">Stranger Things</a></i> e <i><a href="/wiki/Prank_Encounters_-_Scherzi_da_brivido" title="Prank Encounters - Scherzi da brivido">Prank Encounters - Scherzi da brivido</a></i>""")),
        )

        val result = run(movieOrig, trakt, fromDb)

        assertNotNull(result.dubbedEntities)
        assertEquals(1, result.dubbedEntities!!.size)

        result.dubbedEntity?.apply {
            assertEquals(movieRef, movie)
            assertEquals(fabiano, dubber)
            assertEquals(matarazzo, actor)
            assertEquals("Dustin Henderson", name)
            assertEquals(SourceIds.of(DUBDB to id), ids)
            assertEqualsUnordered(listOf(movieOrig, trakt, fromDb).map { it.sources[0] }, sources)
        }
    }

    @Test
    fun completeUnparsedActor() {
        val montgomeryName = "Dacre Montgomery"
        val montgomeryWiki = SourceId(WIKI, "Dacre_Montgomery")
        val montgomeryTrakt = SourceId(TRAKT, "560040")
        val montgomeryImdb = SourceId(IMDB, "nm4223882")
        val cannella = fromJson<DubberRef>("""{"name": "Mirko Cannella", "ids": {"DUBDB": "514d4513adab642b511a6c11", "WIKI": "Mirko_Cannella", "WIKIDATA": "Q61017132", "MONDO_DOPPIATORI": "doppiaggio/voci/vocimcann.htm"}, "parsed": true}""")
        val id = SourceId(DUBDB, "4800497c835a8d8a4a5a6321")

        val movieOrig = DubbedEntity(
            movie = movieRef,
            actor = ActorRefImpl(montgomeryName, SourceIds.of(montgomeryWiki)),
            name = "Billy Hargrove",
            sources = mutableListOf(RawData(SourceId(WIKI, "Stranger_Things"), MOVIE_ORIG, "<a href=\"/wiki/Dacre_Montgomery\" title=\"Dacre Montgomery\">Dacre Montgomery</a>: Billy Hargrove")),
        )
        val trakt = DubbedEntity(
            movie = movieRef,
            actor = ActorRefImpl(montgomeryName, SourceIds.of(montgomeryTrakt, montgomeryImdb)),
            name = "Billy Hargrove",
            sources = mutableListOf(RawData(SourceId(TRAKT, "104439"), TRAKT_MOVIE, "Billy Hargrove")),
        )
        val fromDb = DubbedEntity(
            movie = movieRef,
            dubber = cannella,
            name = montgomeryName,
            ids = SourceIds.of(id, montgomeryWiki),
            sources = mutableListOf(RawData(SourceId(WIKI, "Mattia_Fabiano"), DUBBER, raw="""<a href="/wiki/Gaten_Matarazzo" title="Gaten Matarazzo">Gaten Matarazzo</a> in <i><a href="/wiki/Stranger_Things" title="Stranger Things">Stranger Things</a></i> e <i><a href="/wiki/Prank_Encounters_-_Scherzi_da_brivido" title="Prank Encounters - Scherzi da brivido">Prank Encounters - Scherzi da brivido</a></i>""")),
        )

        val result = run(movieOrig, trakt, fromDb)

        assertNotNull(result.dubbedEntities)
        assertEquals(1, result.dubbedEntities!!.size)

        result.dubbedEntity?.apply {
            assertEquals(movieRef, movie)
            assertEquals(cannella, dubber)
            assertNotNull(actor)
            assertEquals(montgomeryName, actor?.name)
            assertEquals(SourceIds.of(montgomeryWiki, montgomeryTrakt, montgomeryImdb), actor?.ids)
            assertEquals("Billy Hargrove", name)
            assertEquals(SourceIds.of(id), ids)
            assertEqualsUnordered(listOf(movieOrig, trakt, fromDb).map { it.sources[0] }, sources)
        }
    }

    @Test
    fun differentName() {
        val daviesName = "Morgan Davies"
        val daviesWiki = SourceId(WIKI, "Morgan_Davies")
        val daviesTrakt = SourceId(TRAKT, "146170")
        val daviesImdb = SourceId(IMDB, "nm3115934")
        val suarez = fromJson<DubberRef>("""{"name": "Riccardo Suarez", "ids": {"DUBDB": "e17646619314840e113493b1", "WIKI": "Riccardo_Suarez", "WIKIDATA": "Q112316265", "MONDO_DOPPIATORI": "doppiaggio/voci/vocirsua.htm", "IMDB": "nm6694322"}, "parsed": true}""")
        val id = SourceId(DUBDB, "9808462a9d4b69a45609b2be")

        val movieOrig = DubbedEntity(
            movie = movieRef,
            actor = ActorRefImpl(daviesName, SourceIds.of(daviesWiki)),
            name = "Kobi",
            sources = mutableListOf(RawData(SourceId(WIKI, "One_Piece_(serie_televisiva)"), MOVIE_ORIG, "<a href=\"/wiki/Morgan_Davies\" title=\"Morgan Davies\">Morgan Davies</a>: Kobi")),
        )
        val trakt = DubbedEntity(
            movie = movieRef,
            actor = ActorRefImpl(daviesName, SourceIds.of(daviesTrakt, daviesImdb)),
            name = "Koby",
            sources = mutableListOf(RawData(SourceId(TRAKT, "184618"), TRAKT_MOVIE, "Kody")),
        )
        val fromDb = DubbedEntity(
            movie = movieRef,
            dubber = suarez,
            name = daviesName,
            ids = SourceIds.of(id, daviesWiki),
            sources = mutableListOf(RawData(SourceId(WIKI, "Riccardo_Suarez"), DUBBER, raw="""<a href="/wiki/Morgan_Davies" title="Morgan Davies">Morgan Davies</a> in <i><a href="/wiki/One_Piece_(serie_televisiva)" title="One Piece (serie televisiva)">One Piece</a></i>""")),
        )

        val result = run(movieOrig, trakt, fromDb)

        assertNotNull(result.dubbedEntities)
        assertEquals(1, result.dubbedEntities!!.size)

        result.dubbedEntity?.apply {
            assertEquals(movieRef, movie)
            assertEquals(suarez, dubber)
            assertNotNull(actor)
            assertEquals(daviesName, actor?.name)
            assertEquals(SourceIds.of(daviesWiki, daviesTrakt, daviesImdb), actor?.ids)
            assertEquals("Kobi", name)
            assertEquals(SourceIds.of(id), ids)
            assertEqualsUnordered(listOf(movieOrig, trakt, fromDb).map { it.sources[0] }, sources)
        }
    }

    @Test
    fun withAlias() {
        val brown = fromJson<ActorRef>("""{"name": "Millie Bobby Brown", "ids": {"WIKI": "Millie_Bobby_Brown", "WIKIDATA": "Q25936414", "WIKI_EN": "Millie_Bobby_Brown", "IMDB": "nm5611121", "WIKIMEDIA": "Millie_Bobby_Brown_-_MBB_-_Portrait_1_-_SFM5_-_July_10,_2022_at_Stranger_Fan_Meet_5_People_Convention.jpg", "TRAKT": "450913", "DUBDB": "d95e4d4a95e8be4ace049dff"}, "parsed": true}""")
        val fabiano = fromJson<DubberRef>("""{"name": "Chiara Fabiano", "ids": {"DUBDB": "eaeb402883553b1e589842a8", "WIKI": "Chiara_Fabiano", "WIKIDATA": "Q117085918", "MONDO_DOPPIATORI": "doppiaggio/voci/vocicfab.htm", "IMDB": "nm5933163"}, "parsed": true}""")
        val id = "65f34ac28bcc0d0e92a58cd6"

        val movieOrig = DubbedEntity(
            movie = movieRef,
            actor = brown,
            name = "Jane Ives",
            sources = mutableListOf(RawData(SourceId(WIKI, "Stranger_Things"), MOVIE_ORIG, "<a href=\"/wiki/Millie_Bobby_Brown\" title=\"Millie Bobby Brown\">Millie Bobby Brown</a>: Undici / Jane Ives")),
        )
        val movieOrigAlias = DubbedEntity(
            movie = movieRef,
            actor = brown,
            name = "Undici",
            sources = mutableListOf(RawData(SourceId(WIKI, "Stranger_Things"), MOVIE_ORIG, "<a href=\"/wiki/Millie_Bobby_Brown\" title=\"Millie Bobby Brown\">Millie Bobby Brown</a>: Undici / Jane Ives")),
        )
        val trakt = DubbedEntity(
            movie = movieRef,
            actor = brown,
            name = "Jane 'Eleven' Hopper",
            sources = mutableListOf(RawData(SourceId(TRAKT, "104439"), TRAKT_MOVIE, "Jane 'Eleven' Hopper")),
        )
        val fromDb = DubbedEntity(
            movie = movieRef,
            dubber = fabiano,
            name = "Millie Bobby Brown",
            ids = SourceIds.of(DUBDB to id, WIKI to "Millie_Bobby_Brown"),
            sources = mutableListOf(RawData(SourceId(WIKI, "Chiara_Fabiano"), DUBBER, raw="""<a href="/wiki/Millie_Bobby_Brown" title="Millie Bobby Brown">Millie Bobby Brown</a> in <i><a href="/wiki/Stranger_Things" title="Stranger Things">Stranger Things</a></i>""")),
        )

        val result = run(movieOrig, movieOrigAlias, trakt, fromDb)

        assertNull(result.dubbedEntities)
    }

    @Test
    fun movieAndTrakt() {
        val zafraName = "Marita Zafra"
        val zafraWiki = SourceId(WIKI, "Marita_Zafra")
        val zafraTrakt = SourceId(TRAKT, "2317891")
        val zafraImdb = SourceId(WIKI, "nm4747874")


        val movieOrig = DubbedEntity(
            movie = movieRef,
            actor = ActorRefImpl(zafraName, SourceIds.of(zafraWiki)),
            name = "Casilda Escolano Ibáñez",
            sources = mutableListOf(RawData(SourceId(WIKI, "Una_vita_(soap_opera)"), MOVIE_ORIG, "<a href=\"/wiki/Marita_Zafra\" title=\"Marita Zafra\">Marita Zafra</a>: Casilda Escolano Ibáñez")),
        )
        val trakt = DubbedEntity(
            movie = movieRef,
            actor = ActorRefImpl(zafraName, SourceIds.of(zafraTrakt, zafraImdb)),
            name = "Casilda Escolano",
            sources = mutableListOf(RawData(SourceId(TRAKT, "98675"), TRAKT_MOVIE, "Casilda Escolano")),
        )

        val result = run(movieOrig, trakt)

        assertNotNull(result.dubbedEntities)
        assertEquals(1, result.dubbedEntities!!.size)

        result.dubbedEntity?.apply {
            assertEquals(movieRef, movie)
            assertNull(dubber)
            assertNotNull(actor)
            assertEquals(zafraName, actor?.name)
            assertEquals(SourceIds.of(zafraWiki, zafraTrakt, zafraImdb), actor?.ids)
            assertEquals("Casilda Escolano Ibáñez", name)
            assertTrue(ids.isEmpty())
            assertEqualsUnordered(listOf(movieOrig, trakt).map { it.sources[0] }, sources)
        }
    }

    @Test
    fun traktAndDb() {
        val seimetz = ActorRefImpl("Amy Seimetz", SourceIds.of(TRAKT to "439158", IMDB to "nm1541272"))
        val perrella = fromJson<DubberRef>("""{"name": "Valentina Perrella", "ids": {"DUBDB": "5f3f4079b4843296c5f8da41", "WIKI": "Valentina_Perrella", "WIKIDATA": "Q111164158", "MONDO_DOPPIATORI": "doppiaggio/voci/vocivperre.htm"}, "parsed": true}""")
        val id = "63954cafb25c12a4c2f8ec64"

        val trakt = DubbedEntity(
            movie = movieRef,
            actor = seimetz,
            name = "Becky Ives",
            sources = mutableListOf(RawData(SourceId(TRAKT, "104439"), TRAKT_MOVIE, "Becky Ives")),
        )
        val fromDb = DubbedEntity(
            movie = movieRef,
            dubber = perrella,
            name = "Amy Seimetz",
            ids = SourceIds.of(DUBDB to id, WIKI to "Amy_Seimetz"),
            sources = mutableListOf(RawData(SourceId(WIKI, "Valentina_Perrella"), DUBBER, raw="""<a href="/wiki/Amy_Seimetz" title="Amy Seimetz">Amy Seimetz</a> in <i><a href="/wiki/Stranger_Things" title="Stranger Things">Stranger Things</a></i>""")),
        )

        val result = run(trakt, fromDb)

        assertNotNull(result.dubbedEntities)
        assertEquals(1, result.dubbedEntities!!.size)

        result.dubbedEntity?.apply {
            assertEquals(movieRef, movie)
            assertEquals(perrella, dubber)
            assertEquals(seimetz, actor)
            assertEquals("Becky Ives", name)
            assertEquals(SourceIds.of(DUBDB to id), ids)
            assertEqualsUnordered(listOf(trakt, fromDb).map { it.sources[0] }, sources)
        }
    }
}