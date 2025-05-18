package takutility.dubdb.tasks.internal

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import takutility.dubdb.TestContext
import takutility.dubdb.entities.*
import takutility.dubdb.tasks.TaskResult


internal class MergeEntityByNameTest {
    val movieRef = movieRefOf(ids = SourceIds.of(Source.WIKI to "movie", Source.TRAKT to "123456"))

    lateinit var task: MergeEntityByName

    @BeforeEach
    fun before() {
        val context = TestContext.mocked()
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