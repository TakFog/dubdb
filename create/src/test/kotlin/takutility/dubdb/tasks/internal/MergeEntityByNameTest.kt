package takutility.dubdb.tasks.internal

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import takutility.dubdb.TestContext
import takutility.dubdb.entities.*
import takutility.dubdb.tasks.TaskResult


internal class MergeEntityByNameTest {
    val movieRef = movieRefOf()

    protected lateinit var task: MergeEntityByName

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

    /// utility functions

    fun assertMerge(res: TaskResult, vararg sources: DubbedEntity) {
        val name = sources[0].name
        val entity = res.dubbedEntities?.find { it.name == name } ?: fail("$name not found")
        assertNull(entity.id, "id")
        sources.forEach { s ->
            s.actor?.apply { assertEquals(this, entity.actor, "actor") }
            s.dubber?.apply { assertEquals(this, entity.dubber, "dubber") }
            assertTrue(entity.ids.containsAll(s.ids.filter { it.source != Source.DUBDB }), "${s.ids} in ${entity.ids}")
            assertTrue(entity.sources.containsAll(s.sources), "${s.sources} in ${entity.sources}")
        }
    }

    fun assertMissing(res: TaskResult, name: String)
            = assertTrue(res.dubbedEntities!!.none { it.name == name }, "$name missing")

    fun act(pair: Pair<String,String>) = act(pair.second, pair.first)
    fun act(name: String, actor: String): DubbedEntity {
        val actorId = SourceId(Source.DUBDB, actor)
        return DubbedEntity(
            name = name,
            actor = ActorRefImpl(actor, SourceIds.of(actorId)),
            ids = SourceIds.of(Source.DUBDB to "$name|a:${actor}", Source.WIKI_EN to actor),
            movie = movieRef,
            sources = mutableListOf(RawData(actorId, DataSource.MOVIE_ORIG, "<li> $name: $actor")),
        )
    }

    fun dub(pair: Pair<String,String>) = dub(pair.second, pair.first)
    fun dub(name: String, dubber: String): DubbedEntity {
        val dubberId = SourceId(Source.DUBDB, dubber)
        return DubbedEntity(
            name = name,
            dubber = DubberRefImpl(dubber, SourceIds.of(dubberId)),
            ids = SourceIds.of(Source.DUBDB to "$name|d:${dubber}", Source.MONDO_DOPPIATORI to dubber),
            movie = movieRef,
            sources = mutableListOf(RawData(dubberId, DataSource.MOVIE_DUB, "<li> $name: $dubber")),
        )
    }
}