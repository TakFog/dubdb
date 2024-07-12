package takutility.dubdb.tasks.wiki

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import takutility.dubdb.entities.*
import takutility.dubdb.tasks.TaskResult
import takutility.dubdb.wiki.WikiPageLoader

internal class ReadMovieInfoboxTest: WikiPageTest<ReadMovieInfobox>() {
    override fun newTask(loader: WikiPageLoader) = ReadMovieInfobox(loader)
    fun run(title: String): TaskResult = task.run(movieRefOf(ids = SourceIds.of(Source.WIKI to title)))

    @Test
    fun unlinkedMovie() {
        val res = task.run(movieRefOf())
        assertTrue(res.isEmpty())
    }

    @Test
    fun single_ultron() {
        val res = run("Avengers:_Age_of_Ultron")
        assertActor(res,
            "Chris Hemsworth" to "Thor",
            "Cobie Smulders" to "Maria Hill",
            "Paul Bettany" to "Visione",
            "Paul Bettany" to "J.A.R.V.I.S.",
        )
        assertDubber(res,
            "Massimiliano Manfredi" to "Thor",
            "Federica De Bortoli" to "Maria Hill",
        )
    }

    @Test
    fun single_valerian() {
        val res = run("Valerian_e_la_città_dei_mille_pianeti")

        assertDubber(res,
            "Davide Perino" to "Maggiore Valerian",
            "Valentina Favazza" to "Sergente Laureline",
            "Fabio Boccanera" to "Comandante Arün Filitt",
            "Domitilla D'Amico" to "Bubble",
        )
        assertActor(res,
            "Dane DeHaan" to "Maggiore Valerian",
            "Cara Delevingne" to "Sergente Laureline",
            "Clive Owen" to "Comandante Arün Filitt",
            "Rihanna" to "Bubble",
        )
    }

    @Test
    fun multi_ultron() {
        val res = run("Avengers:_Age_of_Ultron")
        assertActor(res,
            "Robert Downey Jr." to "Tony Stark",
            "Robert Downey Jr." to "Iron Man",
            "Jeremy Renner" to "Clint Barton",
            "Jeremy Renner" to "Occhio di Falco",
            "Mark Ruffalo" to "Bruce Banner",
            "Mark Ruffalo" to "Hulk",
            "Lou Ferrigno" to "Hulk",
        )
        assertDubber(res,
            "Angelo Maggi" to "Tony Stark",
            "Angelo Maggi" to "Iron Man",
            "Nino D'Agata" to "J.A.R.V.I.S.",
            "Nino D'Agata" to "Visione",
            "Christian Iansante" to "Clint Barton",
            "Christian Iansante" to "Occhio di Falco",
        )
    }

    @Test
    fun multi_teamAmerica() {
        val res = run("Team_America:_World_Police")

        assertDubber(res,
            "Massimo Rossi" to "Sean Penn",
            "Massimiliano Alto" to "Gary Johnston",
            "Roberto Pedicini" to "Kim Jong Il",
        )
        assertActor(res,
            "Trey Parker" to "Sean Penn",
            "Trey Parker" to "Gary Johnston",
            "Trey Parker" to "Kim Jong Il",
        )
    }

    @Test
    fun multi_lego() {
        val res = run("The_LEGO_Movie")

        assertActor(res,
            "Liam Neeson" to "Poliduro",
            "Liam Neeson" to "Politenero",
            "Liam Neeson" to "Polipà",
        )
        assertDubber(res,
            "Pino Insegno" to "Lord Business",
            "Pino Insegno" to "Uomo di sopra",
            "Pino Insegno" to "Padre di Finn",
        )
    }

    @Test
    fun missingLink() {
        val res = run("Deadpool_2")
        assertActor(res,
            "Andre Tricoteux" to "Colosso", //link to missing page
        )
        assertDubber(res,
            "Dodo Versino" to "Black Tom", //no link in text
            "Marco Manca" to "Peter", //link to missing page
        )
    }

    @Test
    fun dubberLink() {
        val res = run("Deadpool_2")

        // correct url
        assertEquals("Francesco_Venditti", find(res,"Deadpool", dubber = "Francesco Venditti").dubber?.wikiId)
        assertEquals("Francesco_De_Francesco_(doppiatore)",
            find(res,"Fenomeno", dubber = "Francesco De Francesco").dubber?.wikiId)

        //no link in text
        assertNull(find(res,"Black Tom", dubber = "Dodo Versino").dubber?.wikiId)

        //link to missing page
        val manca = find(res, "Peter", dubber = "Marco Manca").dubber
        assertNotNull(manca)
        assertNull(manca!!.wikiId)
        assertEquals("Marco_Manca", manca.ids[Source.WIKI_MISSING]?.id)
    }

    @Test
    fun actorLink() {
        val res = run("Deadpool_2")

        // correct url
        assertEquals("Ryan_Reynolds", find(res,"Deadpool", actor = "Ryan Reynolds").actor?.wikiId)
        assertEquals("Stefan_Kapi%C4%8Di%C4%87", find(res,"Colosso", actor = "Stefan Kapičić").actor?.wikiId)

        //link to missing page
        val trico = find(res, "Colosso", actor = "Andre Tricoteux").actor
        assertNotNull(trico)
        assertNull(trico!!.wikiId)
        assertEquals("Andre_Tricoteux", trico.ids[Source.WIKI_MISSING]?.id)
    }

    @Test
    fun sources() {
        val sourceId = SourceId(Source.WIKI, "Deadpool_2")
        val res = run(sourceId.id)

        assertSource(find(res, "Deadpool", actor = "Ryan Reynolds"),
                sourceId = sourceId, dataSource = DataSource.MOVIE_ORIG,
                raw = """<a href="/wiki/Ryan_Reynolds" title="Ryan Reynolds">Ryan Reynolds</a>: Wade Wilson / Deadpool"""
        )

        assertSource(find(res, "Wade Wilson", actor = "Ryan Reynolds"),
                sourceId = sourceId, dataSource = DataSource.MOVIE_ORIG,
                raw = """<a href="/wiki/Ryan_Reynolds" title="Ryan Reynolds">Ryan Reynolds</a>: Wade Wilson / Deadpool"""
        )

        assertSource(find(res, "Colosso", actor = "Andre Tricoteux"),
                sourceId = sourceId, dataSource = DataSource.MOVIE_ORIG,
                raw = """<a href="/w/index.php?title=Andre_Tricoteux&amp;action=edit&amp;redlink=1" class="new" title="Andre Tricoteux (la pagina non esiste)">Andre Tricoteux</a>: Peter Rasputin / Colosso"""
        )

        assertSource(find(res, "Colosso", actor = "Stefan Kapičić"),
                sourceId = sourceId, dataSource = DataSource.MOVIE_ORIG_DUB,
                raw = """<a href="/wiki/Stefan_Kapi%C4%8Di%C4%87" title="Stefan Kapičić">Stefan Kapičić</a>: Peter Rasputin / Colosso"""
        )

        assertSource(find(res, "Deadpool", dubber = "Francesco Venditti"),
                sourceId = sourceId, dataSource = DataSource.MOVIE_DUB,
                raw = """<a href="/wiki/Francesco_Venditti" title="Francesco Venditti">Francesco Venditti</a>: Wade Wilson / Deadpool"""
        )

        assertSource(find(res, "Peter", dubber = "Marco Manca"),
                sourceId = sourceId, dataSource = DataSource.MOVIE_DUB,
                raw = """<a href="/w/index.php?title=Marco_Manca&amp;action=edit&amp;redlink=1" class="new" title="Marco Manca (la pagina non esiste)">Marco Manca</a>: Peter"""
        )

        assertSource(find(res, "Black Tom", dubber = "Dodo Versino"),
                sourceId = sourceId, dataSource = DataSource.MOVIE_DUB,
                raw = """Dodo Versino: Black Tom"""
        )

    }

    // -----------------------------

    fun find(res: TaskResult, name: String, actor: String? = null, dubber: String? = null): DubbedEntity {
        val dubbed = res.dubbedEntities?.first {
            it.name == name
                    && (actor == null || it.actor?.name == actor)
                    && (dubber == null || it.dubber?.name == dubber)
        }
        assertNotNull(dubbed, "$name $actor $dubber")
        return dubbed!!
    }

    fun assertActor(res: TaskResult, vararg actorCharas: Pair<String, String>) {
        assertNotNull(res.dubbedEntities, "dubbedEntities")
        actorCharas.forEach { ac ->
            val filtered = res.dubbedEntities
                ?.filter { it.actor?.name == ac.first && it.name == ac.second }
            assertFalse(filtered.isNullOrEmpty(), "actor $ac missing")
            assertEquals(1, filtered?.size, "$ac results")
        }
    }

    fun assertDubber(res: TaskResult, vararg dubberCharas: Pair<String, String>) {
        assertNotNull(res.dubbedEntities, "dubbedEntities")
        dubberCharas.forEach { dc ->
            val filtered = res.dubbedEntities
                ?.filter { it.dubber?.name == dc.first && it.name == dc.second }
            assertNotNull(filtered, "dubber $dc missing")
            assertEquals(1, filtered?.size, "$dc results")
        }
    }

    fun assertSource(entity: DubbedEntity, sourceId: SourceId, dataSource: DataSource, raw: String) {
        assertEquals(1, entity.sources.size, "sources count for $entity")
        val src = entity.sources[0]
        assertEquals(sourceId, src.sourceId, "sourceId for $entity")
        assertEquals(dataSource, src.dataSource, "dataSource for $entity")
        assertEquals(raw, src.raw, "raw for $entity")
    }
}