package takutility.dubdb.tasks.internal

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import takutility.dubdb.assertEqualsUnordered
import takutility.dubdb.entities.*
import takutility.dubdb.fromJson
import takutility.dubdb.tasks.TaskResult


internal class MergeEntityByNameTest {
    val movieRef = movieRefOf(ids = SourceIds.of(Source.WIKI to "movie", Source.TRAKT to "123456"))

    lateinit var task: MergeEntityByName

    @BeforeEach
    fun before() {
        task = MergeEntityByName()
    }

    fun run(vararg entities: DubbedEntity): TaskResult = task.run(listOf(*entities))

    @Test
    fun single_ultron() {
        val thorActor = act("Chris Hemsworth" to "Thor")
        val hillActor = act("Cobie Smulders" to "Maria Hill")
        val thorDubber = dub("Massimiliano Manfredi" to "Thor")
        val hillDubber = dub("Federica De Bortoli" to "Maria Hill")

        val res = run(thorActor, hillActor, thorDubber, hillDubber)

        assertMerge(res, thorDubber, thorActor)
        assertMerge(res, hillDubber, hillActor)
    }

    @Test
    fun single_valerian() {
        val valerianActor = act("Dane DeHaan" to "Maggiore Valerian")
        val laurelineActor = act("Cara Delevingne" to "Sergente Laureline")
        val filittActor = act("Clive Owen" to "Comandante Arün Filitt")
        val bubbleActor = act("Rihanna" to "Bubble")
        val valerianDubber = dub("Davide Perino" to "Maggiore Valerian")
        val laurelineDubber = dub("Valentina Favazza" to "Sergente Laureline")
        val filittDubber = dub("Fabio Boccanera" to "Comandante Arün Filitt")
        val bubbleDubber = dub("Domitilla D'Amico" to "Bubble")

        val res = run(
                valerianActor,
                laurelineActor,
                filittActor,
                bubbleActor,
                valerianDubber,
                laurelineDubber,
                filittDubber,
                bubbleDubber,
        )

        assertMerge(res, valerianDubber, valerianActor)
        assertMerge(res, laurelineDubber, laurelineActor)
        assertMerge(res, filittDubber, filittActor)
        assertMerge(res, bubbleDubber, bubbleActor)
    }

    @Test
    fun multi_ultron() {
        val starkActor = act("Robert Downey Jr." to "Tony Stark")
        val ironmanActor = act("Robert Downey Jr." to "Iron Man")
        val bartonActor = act("Jeremy Renner" to "Clint Barton")
        val falcoActor = act("Jeremy Renner" to "Occhio di Falco")
        val visioneActor = act("Paul Bettany" to "Visione")
        val jarvisActor = act("Paul Bettany" to "J.A.R.V.I.S.")
        val starkDubber = dub("Angelo Maggi" to "Tony Stark")
        val ironmanDubber = dub("Angelo Maggi" to "Iron Man")
        val bartonDubber = dub("Christian Iansante" to "Clint Barton")
        val falcoDubber = dub("Christian Iansante" to "Occhio di Falco")
        val visioneDubber = dub("Nino D'Agata" to "Visione")
        val jarvisDubber = dub("Nino D'Agata" to "J.A.R.V.I.S.")

        val res = run(
            starkActor,
            ironmanActor,
            bartonActor,
            falcoActor,
            visioneActor,
            jarvisActor,
            starkDubber,
            ironmanDubber,
            bartonDubber,
            falcoDubber,
            visioneDubber,
            jarvisDubber,
        )

        assertMerge(res, starkDubber, starkActor)
        assertMerge(res, ironmanDubber, ironmanActor)
        assertMerge(res, bartonDubber, bartonActor)
        assertMerge(res, falcoDubber, falcoActor)
        assertMerge(res, visioneDubber, visioneActor)
        assertMerge(res, jarvisDubber, jarvisActor)
    }

    @Test
    fun multiOrig_ultron() {
        val actor = DubbedEntity(
            name = "Hulk",
            actor = ActorRefImpl("Mark Ruffalo", SourceIds.of(Source.WIKI to "Mark_Ruffalo")),
            movie = movieRef,
            sources = mutableListOf(RawData(movieRef.wiki!!, DataSource.MOVIE_ORIG, "<li> Mark Ruffalo: Bruce Banner / Hulk")),
        )
        val origDub = DubbedEntity(
            name = "Hulk",
            actor = ActorRefImpl("Lou Ferrigno", SourceIds.of(Source.WIKI to "Lou_Ferrigno")),
            movie = movieRef,
            sources = mutableListOf(RawData(movieRef.wiki!!, DataSource.MOVIE_ORIG_DUB, "<li> Lou Ferrigno: Hulk")),
        )
        val itDub = dub("Riccardo Rossi" to "Hulk")

        val res = run(actor, origDub, itDub)

        assertNull(res.dubbedEntities)
    }

    @Test
    fun trakt_ultron() {
        val starkActor = act("Robert Downey Jr." to "Tony Stark")
        val ironmanActor = act("Robert Downey Jr." to "Iron Man")
        val bartonActor = act("Jeremy Renner" to "Clint Barton")
        val falcoActor = act("Jeremy Renner" to "Occhio di Falco")
        val visioneActor = act("Paul Bettany" to "Visione")
        val jarvisActor = act("Paul Bettany" to "J.A.R.V.I.S.")
        val starkDubber = dub("Angelo Maggi" to "Tony Stark")
        val ironmanDubber = dub("Angelo Maggi" to "Iron Man")
        val bartonDubber = dub("Christian Iansante" to "Clint Barton")
        val falcoDubber = dub("Christian Iansante" to "Occhio di Falco")
        val visioneDubber = dub("Nino D'Agata" to "Visione")
        val jarvisDubber = dub("Nino D'Agata" to "J.A.R.V.I.S.")
        val starkTrakt = trk("Robert Downey Jr." to "Tony Stark")
        val ironmanTrakt = trk("Robert Downey Jr." to "Iron Man")
        val bartonTrakt = trk("Jeremy Renner" to "Clint Barton")
        val falcoTrakt = trk("Jeremy Renner" to "Hawkeye")
        val visioneTrakt = trk("Paul Bettany" to "Vision")
        val jarvisTrakt = trk("Paul Bettany" to "Jarvis")

        val res = run(
            starkActor,
            ironmanActor,
            bartonActor,
            falcoActor,
            visioneActor,
            jarvisActor,
            starkDubber,
            ironmanDubber,
            bartonDubber,
            falcoDubber,
            visioneDubber,
            jarvisDubber,
            starkTrakt,
            ironmanTrakt,
            bartonTrakt,
            falcoTrakt,
            visioneTrakt,
            jarvisTrakt,
        )

        val stark = assertMergeNoActor(res, starkDubber, starkActor, starkTrakt)
        assertEquals(mergeActorIds(starkActor, starkTrakt), stark.actor?.ids)
        val ironman = assertMergeNoActor(res, ironmanDubber, ironmanActor, ironmanTrakt)
        assertEquals(mergeActorIds(ironmanActor, ironmanTrakt), ironman.actor?.ids)

        val barton = assertMergeNoActor(res, bartonDubber, bartonActor, bartonTrakt)
        assertEquals(mergeActorIds(bartonActor, bartonTrakt), barton.actor?.ids)
        assertMerge(res, falcoDubber, falcoActor)
        assertMissing(res, falcoTrakt.name)

        assertMerge(res, visioneDubber, visioneActor)
        assertMissing(res, visioneTrakt.name)
        assertMerge(res, jarvisDubber, jarvisActor)
        assertMissing(res, jarvisTrakt.name)
    }

    private fun mergeActorIds(a: DubbedEntity, b: DubbedEntity) = a.actor!!.ids + b.actor!!.ids

    @Test
    fun multi_teamAmerica() {
        val pennDubber = dub("Massimo Rossi" to "Sean Penn")
        val johnstonDubber = dub("Massimiliano Alto" to "Gary Johnston")
        val ilDubber = dub("Roberto Pedicini" to "Kim Jong Il")
        val pennActor = act("Trey Parker" to "Sean Penn")
        val johnstonActor = act("Trey Parker" to "Gary Johnston")
        val ilActor = act("Trey Parker" to "Kim Jong Il")

        val res = run(
            pennDubber,
            johnstonDubber,
            ilDubber,
            pennActor,
            johnstonActor,
            ilActor,
        )

        assertMerge(res, pennDubber, pennActor)
        assertMerge(res, johnstonDubber, johnstonActor)
        assertMerge(res, ilDubber, ilActor)
    }

    @Test
    fun trakt_teamAmerica() {
        val pennDubber = dub("Massimo Rossi" to "Sean Penn")
        val johnstonDubber = dub("Massimiliano Alto" to "Gary Johnston")
        val ilDubber = dub("Roberto Pedicini" to "Kim Jong Il")
        val susanDubber = dub("Alessandra Korompay" to "Susan Sarandon")
        val lisaDubber = dub("Letizia Ciampa" to "Lisa")
        val pennActor = act("Trey Parker" to "Sean Penn")
        val johnstonActor = act("Trey Parker" to "Gary Johnston")
        val ilActor = act("Trey Parker" to "Kim Jong Il")
        val susanActor = act("Trey Parker" to "Susan Sarandon")
        val lisaActor = act("Kristen Miller" to "Lisa")
        val pennTrakt = trk("Trey Parker" to "Sean Penn")
        val johnstonTrakt = trk("Trey Parker" to "Gary Johnston")
        val ilTrakt = trk("Trey Parker" to "Kim Jong Il")
        val susanTrakt = trk("Trey Parker" to "Susan Sarandon (voice)")
        val lisaTrakt = trk("Kristen Miller" to "Lisa (voice)")

        val res = run(
            pennDubber,
            johnstonDubber,
            ilDubber,
            susanDubber,
            lisaDubber,
            pennActor,
            johnstonActor,
            ilActor,
            susanActor,
            lisaActor,
            pennTrakt,
            johnstonTrakt,
            ilTrakt,
            susanTrakt,
            lisaTrakt,
        )

        assertMergeMultiActor(res, pennDubber, pennActor, pennTrakt)
        assertMergeMultiActor(res, johnstonDubber, johnstonActor, johnstonTrakt)
        assertMergeMultiActor(res, ilDubber, ilActor, ilTrakt)
        assertMergeMultiActor(res, susanDubber, susanActor, susanTrakt)
        assertMergeMultiActor(res, lisaDubber, lisaActor, lisaTrakt)
    }

    @Test
    fun multiname_ultron() {
        val bannerActor = act("Mark Ruffalo" to "Bruce Banner")
        val hulkRActor = act("Mark Ruffalo" to "Hulk")
        val hulkFActor = act("Lou Ferrigno" to "Hulk")
        val bannerDubber = dub("Riccardo Rossi" to "Bruce Banner")
        val hulkDubber = dub("Riccardo Rossi" to "Hulk")

        val res = run(
            bannerActor,
            hulkRActor,
            hulkFActor,
            bannerDubber,
            hulkDubber,
        )

        assertMerge(res, bannerDubber, bannerActor)
        assertMissing(res, "Hulk")
    }

    @Test
    fun fromDb() {
        //{"_id": {"${'$'}oid": "56cf4765bab7a470136d6c24"}, "name": "Barb Howard", "sources": [{"source": "WIKI", "sourceId": "Fallout_(serie_televisiva)", "dataSource": "MOVIE_ORIG", "raw": "* [[Frances Turner]]: Barb Howard"}, {"source": "TRAKT", "sourceId": "163965", "dataSource": "TRAKT_MOVIE", "raw": "Barb Howard"}, {"source": "WIKI", "sourceId": "Valentina_Favazza", "dataSource": "DUBBER", "raw": "<a href=\"/w/index.php?title=Frances_Turner&amp;action=edit&amp;redlink=1\" class=\"new\" title=\"Frances Turner (la pagina non esiste)\">Frances Turner</a> in <i><a href=\"/wiki/Fallout_(serie_televisiva)\" title=\"Fallout (serie televisiva)\">Fallout</a></i>"}], "movie": {"name": "Fallout", "ids": {"TRAKT": "163965", "IMDB": "tt12637874", "WIKIDATA": "Q113127312", "WIKI": "Fallout_(serie_televisiva)", "WIKI_EN": "Fallout_(American_TV_series)", "MONDO_DOPPIATORI": "doppiaggio/telefilm/fallout", "DUBDB": "585d48078b4d844f26a01ff3"}, "parsed": true, "type": "SERIES"}, "dubber": {"name": "Valentina Favazza", "ids": {"WIKI": "Valentina_Favazza", "WIKIDATA": "Q21418862", "WIKI_EN": "Valentina_Favazza", "MONDO_DOPPIATORI": "doppiaggio/voci/vocivfav.htm", "WIKIMEDIA": "Valentina_Favazza_-_Lucca_Comics_&_Games_2015.JPG", "DUBDB": "73e14c6b93bb86b820ecf2d3"}, "parsed": true}, "actor": {"name": "Frances Turner", "ids": {"TRAKT": "930982", "IMDB": "nm2204675", "WIKIDATA": "Q137216599", "WIKI_EN": "Frances_Turner", "WIKI": "Frances Turner", "WIKI_MISSING": "Frances_Turner"}}}
        //{"name": "Barb Howard", "sources": [{"source": "WIKI", "sourceId": "Fallout_(serie_televisiva)", "dataSource": "MOVIE_DUB", "raw": "* [[Valentina Favazza]]: Barb Howard"}], "movie": {"name": "Fallout", "ids": {"TRAKT": "163965", "IMDB": "tt12637874", "WIKIDATA": "Q113127312", "WIKI": "Fallout_(serie_televisiva)", "WIKI_EN": "Fallout_(American_TV_series)", "MONDO_DOPPIATORI": "doppiaggio/telefilm/fallout", "DUBDB": "585d48078b4d844f26a01ff3"}, "parsed": true, "type": "SERIES"}, "dubber": {"name": "Valentina Favazza", "ids": {"WIKI": "Valentina Favazza"}, "parsed": false}}
        val name = "Barb Howard"
        val movie = movieRefOf("Fallout")

        val ent1 = DubbedEntity(
            name = name,
            movie = movie,
            ids = SourceIds.of(Source.DUBDB to "56cf4765bab7a470136d6c24"),
            sources = mutableListOf(
                RawData(SourceId(Source.WIKI, "Fallout_(serie_televisiva)"), DataSource.MOVIE_ORIG, raw = "* [[Frances Turner]]: Barb Howard"),
                RawData(SourceId(Source.TRAKT, "163965"), DataSource.TRAKT_MOVIE, raw = "Barb Howard"),
                RawData(SourceId(Source.WIKI, "Valentina_Favazza"), DataSource.DUBBER, raw = ""),
            ),
            dubber = DubberRefImpl("Valentina Favazza",
                ids = SourceIds.of(
                    Source.WIKI to "Valentina_Favazza",
                    Source.WIKIDATA to "Q21418862",
                    Source.WIKI_EN to "Valentina_Favazza",
                    Source.MONDO_DOPPIATORI to "doppiaggio/voci/vocivfav.htm",
                    Source.WIKIMEDIA to "Valentina_Favazza_-_Lucca_Comics_&_Games_2015.JPG",
                )),
            actor = ActorRefImpl("Frances Turner",
                ids = SourceIds.of(
                    Source.TRAKT to "930982",
                    Source.IMDB to "nm2204675",
                    Source.WIKIDATA to "Q137216599",
                    Source.WIKI_EN to "Frances_Turner",
                    Source.WIKI to "Frances Turner",
                )),
        )
        val ent2 = DubbedEntity(
            name = name,
            movie = movie,
            sources = mutableListOf(
                RawData(SourceId(Source.WIKI, "Fallout_(serie_televisiva)"), DataSource.MOVIE_DUB, raw = "* [[Valentina Favazza]]: Barb Howard"),
            ),
            dubber = DubberRefImpl("Valentina Favazza",
                ids = SourceIds.of(                    Source.WIKI to "Valentina_Favazza")),
        )

        val res = run(ent1, ent2)

        assertEquals(1, res.dubbedEntities?.size)
    }

    @Test
    fun unmatch() {
        val actor1 = act("actor1" to "char1")
        val actor2 = act("actor2" to "char2")
        val actor4 = act("actor4" to "char4")
        val actor5 = act("actor5" to "char5")
        val dubber1 = dub("dubber1" to "char1")
        val dubber3 = dub("dubber3" to "char3")
        val dubber4 = dub("dubber4" to "char4")

        val res = run(
            actor1,
            actor2,
            actor4,
            actor5,
            dubber1,
            dubber3,
            dubber4,
        )

        assertMerge(res, dubber1, actor1)
        assertMissing(res, "char2")
        assertMissing(res, "char3")
        assertMerge(res, dubber4, actor4)
        assertMissing(res, "char5")
    }

    @Test
    fun tooManyDubbers() {
        val actor1 = act("actor1" to "char1")
        val actor2 = act("actor2" to "char2")
        val dubber1 = dub("dubber1" to "char1")
        val dubber2a = dub("dubber2a" to "char2")
        val dubber2b = dub("dubber2b" to "char2")

        val res = run(
            actor1,
            actor2,
            dubber1,
            dubber2a,
            dubber2b,
        )

        assertMerge(res, dubber1, actor1)
        assertMissing(res, "char2")
    }

    @Test
    fun mergeDubbers() {
        val actor1 = act("actor1" to "char1")
        val actor2 = act("actor2" to "char2")
        val dubber1 = dub("dubber1" to "char1")
        val dubber2a = dub("dubber2" to "char2")
        val dubber2b = dub("dubber2" to "char2").apply {
            dubber?.ids?.remove(Source.DUBDB)
            ids.clear()
            sources.clear()
            sources.add(RawData(dubber?.wiki!!, DataSource.DUBBER, "<li> $name: ${movie.wikiId}"))
        }

        val res = run(
            actor1,
            actor2,
            dubber1,
            dubber2a,
            dubber2b,
        )

        assertMerge(res, dubber1, actor1)
        val char2 = assertMerge(res, false, true, dubber2a, actor2, dubber2b)
        assertEquals(dubber2a.dubber, char2.dubber)
    }

    @Test
    fun tooManyActors() {
        val actor1 = act("actor1" to "char1")
        val actor2a = act("actor2a" to "char2")
        val actor2b = act("actor2b" to "char2")
        val dubber1 = dub("dubber1" to "char1")
        val dubber2 = dub("dubber2" to "char2")

        val res = run(
            actor1,
            actor2a,
            actor2b,
            dubber1,
            dubber2,
        )

        assertMerge(res, dubber1, actor1)
        assertMissing(res, "char2")
    }

    @Test
    fun withTrakt() {
        val actor1 = act("actor1" to "char1")
        val actor2 = act("actor2" to "char2")
        val actor4 = act("actor4" to "char4")
        val actor5 = act("actor5" to "char5")
        val dubber1 = dub("dubber1" to "char1")
        val dubber3 = dub("dubber3" to "char3")
        val dubber4 = dub("dubber4" to "char4")
        val dubber6 = dub("dubber6" to "char6")
        val trakt1 = trk("trakt1" to "char1")
        val trakt3 = trk("trakt3" to "char3")
        val trakt5 = trk("trakt5" to "char5")
        val trakt7 = trk("trakt7" to "char7")

        val res = run(
            trakt1,
            actor1,
            actor2,
            actor4,
            actor5,
            dubber1,
            dubber3,
            dubber4,
            dubber6,
            trakt3,
            trakt5,
            trakt7,
        )

        assertMergeMultiActor(res, dubber1, actor1, trakt1)
        assertMissing(res, "char2")
        assertMerge(res, dubber3, trakt3)
        assertMerge(res, dubber4, actor4)
        assertMerge(res, checkDubber = false, checkActor = false, actor5, trakt5)
            .also { assertEquals(mergeActorIds(actor5, trakt5), it.actor?.ids) }
        assertMissing(res, "char6")
        assertMissing(res, "char7")
    }

    @Test
    fun strangerThings_afterActorMerge() {
        val matarazzo = fromJson<ActorRef>("""{"name": "Gaten Matarazzo", "ids": {"WIKI": "Gaten_Matarazzo", "WIKIDATA": "Q26704332", "WIKI_EN": "Gaten_Matarazzo", "IMDB": "nm7140802", "WIKIMEDIA": "Gaten_Matarazzo.jpg", "TRAKT": "686045", "DUBDB": "2c244701a72e71d6becb3efd"}, "parsed": true}""")
        val fabiano = fromJson<Dubber>("{\"_id\": {\"\$oid\": \"486049ce8d6f9a9517aaeed0\"}, \"name\": \"Mattia Fabiano\", \"ids\": {\"WIKI\": \"Mattia_Fabiano\", \"WIKIDATA\": \"Q112873540\", \"MONDO_DOPPIATORI\": \"doppiaggio/voci/vocimfab.htm\", \"IMDB\": \"nm7445018\"}, \"parseTs\": {\"\$date\": \"2025-03-08T15:36:58.938Z\"}, \"lastUpdate\": \"2025-02-24\"}")
        val id = SourceIds.of(Source.DUBDB to "a0e845ff9af5face8795a255")

        val merged = DubbedEntity(
            movie = movieRef,
            dubber = fabiano.toRef(),
            actor = matarazzo,
            name = "Dustin Henderson",
            ids = id,
            sources = mutableListOf(
                RawData(SourceId(Source.WIKI, "Stranger_Things"), DataSource.MOVIE_ORIG, "<a href=\"/wiki/Gaten_Matarazzo\" title=\"Gaten Matarazzo\">Gaten Matarazzo</a>: Dustin Henderson"),
                RawData(SourceId(Source.TRAKT, "104439"), DataSource.TRAKT_MOVIE, "Dustin Henderson"),
                RawData(SourceId(Source.WIKI, "Mattia_Fabiano"), DataSource.DUBBER, raw="""<a href="/wiki/Gaten_Matarazzo" title="Gaten Matarazzo">Gaten Matarazzo</a> in <i><a href="/wiki/Stranger_Things" title="Stranger Things">Stranger Things</a></i> e <i><a href="/wiki/Prank_Encounters_-_Scherzi_da_brivido" title="Prank Encounters - Scherzi da brivido">Prank Encounters - Scherzi da brivido</a></i>"""),
            )
        )
        val movieDub = DubbedEntity(
            movie = movieRef,
            dubber = fabiano,
            name = "Dustin Henderson",
            sources = mutableListOf(RawData(SourceId(Source.WIKI, "Stranger_Things"), DataSource.MOVIE_DUB, "<a href=\"/wiki/Mattia_Fabiano\" title=\"Mattia Fabiano\">Mattia Fabiano</a>: Dustin Henderson")),
        )

        val result = run(merged, movieDub)

        assertEquals(1, result.dubbedEntities?.size)
        result.dubbedEntity?.apply {
            assertEquals(movieRef, movie)
            assertEquals(fabiano.toRef(), dubber?.toRef())
            assertEquals(matarazzo, actor)
            assertEquals("Dustin Henderson", name)
            assertEquals(id, ids)
            assertEqualsUnordered(listOf(merged, movieDub).flatMap() { it.sources }, sources)
        }
    }

    @Test
    fun normalizeTrakt() {
        val actor1 = act("actor1" to "char1")
        val actor2 = act("actor2" to "char2")
        val actor3 = act("actor3" to "char3")
        val dubber1 = dub("dubber1" to "char1")
        val dubber2 = dub("dubber2" to "char2")
        val dubber3 = dub("dubber3" to "char3")
        val trakt1 = trk("trakt1" to "char1")
        val trakt2 = trk("trakt2" to "char2 (voice)")
        val trakt3 = trk("trakt3" to "char3 (voice, uncredited)")

        val res = run(
            actor1,
            actor2,
            actor3,
            dubber1,
            dubber2,
            dubber3,
            trakt1,
            trakt2,
            trakt3,
        )

        assertMergeMultiActor(res, dubber1, actor1, trakt1)
        assertMergeMultiActor(res, dubber2, actor2, trakt2)
        assertMergeMultiActor(res, dubber3, actor3, trakt3)
    }

    /// utility functions

    fun assertMerge(res: TaskResult, vararg sources: DubbedEntity) {
        assertMerge(res, true, true, *sources)
    }

    fun assertMergeNoActor(res: TaskResult, vararg sources: DubbedEntity): DubbedEntity {
        return assertMerge(res, checkDubber = true, checkActor = false, *sources)
    }

    fun assertMergeMultiActor(res: TaskResult, dubber: DubbedEntity, actorA: DubbedEntity, actorB: DubbedEntity) {
        val e = assertMergeNoActor(res, *(arrayOf(dubber, actorA, actorB)))
        assertEquals(mergeActorIds(actorA, actorB), e.actor?.ids)
    }

    fun assertMerge(res: TaskResult, checkDubber: Boolean, checkActor: Boolean, vararg sources: DubbedEntity): DubbedEntity {
        val name = sources[0].name
        val entity = res.dubbedEntities?.find { it.name == name } ?: fail("$name not found")
        assertEquals(sources[0].id, entity.id, "id")
        sources.forEach { s ->
            if (checkActor) s.actor?.apply { assertEquals(this, entity.actor, "actor") }
            if (checkDubber) s.dubber?.apply { assertEquals(this, entity.dubber, "dubber") }
            assertTrue(entity.ids.containsAll(s.ids.filter { it.source != Source.DUBDB }), "${s.ids} in ${entity.ids}")
            assertTrue(entity.sources.containsAll(s.sources), "${s.sources} in ${entity.sources}")
        }
        assertEquals(sources.sumOf { it.sources.size }, entity.sources.size, "sources count")
        return entity
    }

    fun assertMissing(res: TaskResult, name: String)
            = assertTrue(res.dubbedEntities!!.none { it.name == name }, "$name missing")

    fun act(pair: Pair<String,String>) = act(pair.second, pair.first)
    fun act(name: String, actor: String): DubbedEntity {
        return DubbedEntity(
            name = name,
            actor = ActorRefImpl(actor, SourceIds.of(Source.DUBDB to actor, Source.WIKI to actor)),
            ids = SourceIds.of(Source.DUBDB to "$name|a:${actor}", Source.WIKI_EN to actor),
            movie = movieRef,
            sources = mutableListOf(RawData(movieRef.wiki!!, DataSource.MOVIE_ORIG, "<li> $name: $actor")),
        )
    }

    fun trk(pair: Pair<String,String>) = trk(pair.second, pair.first)
    fun trk(name: String, actor: String): DubbedEntity {
        return DubbedEntity(
            name = name,
            actor = ActorRefImpl(actor, SourceIds.of(Source.TRAKT to actor)),
            ids = SourceIds.of(Source.TRAKT to name),
            movie = movieRef,
            sources = mutableListOf(RawData(movieRef.ids[Source.TRAKT]!!, DataSource.TRAKT_MOVIE, name)),
        )
    }

    fun dub(pair: Pair<String,String>) = dub(pair.second, pair.first)
    fun dub(name: String, dubber: String): DubbedEntity {
        return DubbedEntity(
            name = name,
            dubber = DubberRefImpl(dubber, SourceIds.of(Source.DUBDB to dubber, Source.WIKI to dubber)),
            ids = SourceIds.of(Source.DUBDB to "$name|d:${dubber}", Source.MONDO_DOPPIATORI to dubber),
            movie = movieRef,
            sources = mutableListOf(RawData(movieRef.wiki!!, DataSource.MOVIE_DUB, "<li> $name: $dubber")),
        )
    }
}