package takutility.dubdb.tasks.wiki

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import takutility.dubdb.entities.*
import takutility.dubdb.tasks.TaskResult
import takutility.dubdb.wiki.WikiPageLoader

internal open class ReadDubberSectionBaseTest: WikiPageTest<ReadDubberSection>() {
    override fun newTask(loader: WikiPageLoader) = ReadDubberSection(loader)

    var dubber: DubberRef = DubberRefImpl()

    fun dubber(title: String): DubberRef {
        dubber = DubberRefImpl(ids = SourceIds.of(Source.WIKI to title))
        return dubber
    }

    fun run(title: String? = null): TaskResult {
        if (title != null) dubber(title)
        return task.run(dubber)
    }

    fun run(dubber: DubberRef) = task.run(dubber)

    fun assertLinkedEntity(res: TaskResult, name: String, vararg movies: String) {
        assertEntity(res, dubber, name, true, *movies)
    }

    fun assertMissingEntity(res: TaskResult, name: String, vararg movies: String) {
        assertEntity(res, dubber, name, false, *movies)
        val entities = find(res, name)
        entities.forEach { assertNotNull(it.ids[Source.WIKI_MISSING], "${it.name} missing wiki") }
    }

    fun assertEntity(res: TaskResult, name: String, vararg movies: String) {
        assertEntity(res, dubber, name, false, *movies)
    }
}

internal class ReadDubberSectionTest: ReadDubberSectionBaseTest() {

    @Test
    fun angeloMaggi() {
        val res = run("Angelo_Maggi")
        assertNotNull(res.dubbedEntities)
        assertFalse(res.dubbedEntities!!.isEmpty())

        assertLinkedEntity(res, "Tom Hanks"
                , "Lo schermo velato"
                , "Cast Away"
                , "Prova a prendermi"
                , "The Terminal"
                , "La guerra di Charlie Wilson"
                , "The Pixar Story"
                , "Molto forte, incredibilmente vicino"
                , "Cloud Atlas"
                , "Captain Phillips - Attacco in mare aperto"
                , "Saving Mr. Banks"
                , "Ithaca - L'attesa di un ritorno"
                , "Il ponte delle spie"
                , "Sully"
                , "The Circle"
                , "The Post"
                , "Un amico straordinario"
                , "Greyhound - Il nemico invisibile"
                , "Elvis"
                , "30 Rock"
        )

        assertLinkedEntity(res, "Ted Levine", "Shutter Island")
        assertLinkedEntity(res, "Tim Preece", "L'uomo nell'ombra")
        assertLinkedEntity(res, "Jack Coleman", "Castle")
        assertEntity(res, "Scimmia",
            "Kung Fu Panda",
            "Kung Fu Panda 2",
            "Kung Fu Panda 3",
            "Kung Fu Panda - Mitiche avventure"
        )
        assertEntity(res, "Ryo Nagare",
            "Il Grande Mazinga contro Getta Robot",
            "Il Grande Mazinga contro Getta Robot G",
            "Il Grande Mazinga, Getta Robot G, UFO Robot Goldrake contro il Dragosauro"
        )
        assertLinkedEntity(res, "Guile", "Street Fighter II V")
        assertLinkedEntity(res, "David Aston", "Matrix")
    }

    @Test
    fun sources_angeloMaggi() {
        val dubber = dubber("Angelo_Maggi")
        val res = run(dubber)

        res.dubbedEntities!!.forEach {
            assertEquals(1, it.sources.size)
            assertEquals(DataSource.DUBBER, it.sources[0].dataSource)
            assertEquals(dubber.wiki, it.sources[0].sourceId)
        }
    }

    @Test
    fun actorMissing_angeloMaggi() {
        val res = run("Angelo_Maggi")
        assertMissingEntity(res, "Andy Secombe",
            "Star Wars: Episodio I - La minaccia fantasma",
            "Star Wars: Episodio II - L'attacco dei cloni"
        )
        assertMissingEntity(res, "Ronald Falk",
            "Star Wars: Episodio II - L'attacco dei cloni"
        )
    }

    @Test
    fun gabrielePatriarca() {
        val res = run("Gabriele_Patriarca_(doppiatore)")
        assertNotNull(res.dubbedEntities)
        assertFalse(res.dubbedEntities!!.isEmpty())

        assertLinkedEntity(
            res, "Christopher Mintz-Plasse",
            "Kick-Ass",
            "Kick-Ass 2",
            "Comic Movie",
            "Cattivi vicini",
            "Cattivi vicini 2"
        )
        assertEntity(res, "Mammolo", "I 7N")
        assertLinkedEntity(res, "Davide", "La grande storia di Davide e Golia")
    }

    @Test
    fun actorMissing_gabrielePatriarca() {
        val res = run("Gabriele_Patriarca_(doppiatore)")
        assertMissingEntity(res, "Martin Svetlik",
            "I fratelli Grimm e l'incantevole strega")
    }

    @Test
    fun movieMissing_gabrielePatriarca() {
        val res = run("Gabriele_Patriarca_(doppiatore)")

        find(res, "Frank Brown").forEach{
            assertNull(it.wikiId, "${it.name} wiki")
            assertNotNull(it.ids[Source.WIKI_MISSING], "${it.name} missing")
        }

        find(res, "Brian").forEach{
            if (it.name != "Taddeo l'esploratore e la tavola di smeraldo") return
            assertNull(it.wikiId, "${it.name} wiki")
            assertNotNull(it.ids[Source.WIKI_MISSING], "${it.name} missing")
        }
    }

    @Test
    fun ilariaStagni() {
        val res = run("Ilaria_Stagni")
        assertNotNull(res.dubbedEntities)
        assertFalse(res.dubbedEntities!!.isEmpty())

        assertLinkedEntity(res, "Isabela Garcia", "Agua Viva")
        assertEntity(res, "Ana Carolina Valsagna", "Soy Luna")
        assertEntity(res, "Jack-Jack Parr", "Gli Incredibili 2")
        assertLinkedEntity(res, "Shippo", "Inuyasha")
        assertLinkedEntity(res, "Laura Brent",
            "Le cronache di Narnia - Il viaggio del veliero")

        res.dubbedEntities?.forEach {
            assertTrue(it.name.length <= 100, "${it.name} too long")
        }
    }

    @Test
    fun saraLabidi() {
        val res = run("Sara_Labidi")
        assertNotNull(res.dubbedEntities)
        assertFalse(res.dubbedEntities!!.isEmpty())

        assertLinkedEntity(res, "Cassady McClincy", "The Walking Dead")
        assertLinkedEntity(res, "Maisie Williams", "Il Trono di Spade", "Doctor Who", "The New Mutants")
        assertEntity(res, "Morgan Turner", "Jumanji - Benvenuti nella giungla")
        assertEntity(res, "Juno", "Beastars")
    }

}

internal class ReadDubberSectionDubAttributesTest: ReadDubberSectionBaseTest() {

    @Test
    fun unlinked_angeloMaggi() {
        val res = run("Angelo_Maggi")
        assertEntity(res, "Yotsuya (1ª voce)", "Cara dolce Kyoko")
        assertEntity(res, "Jeremy (2ª vers. italiana)", "Jenny la tennista")
    }

    @Test
    fun linked_angeloMaggi() {
        val res = run("Angelo_Maggi")

        assertLinkedEntity(res, "Commissario Winchester")

        assertLinkedEntity(res, "Reverendo Lovejoy", "I Simpson")
        assertSource(find(res, "Reverendo Lovejoy", "I Simpson"),
            """<a href="/wiki/Clancy_Winchester" title="Clancy Winchester">Commissario Winchester</a> (2ª voce e principale, ep.5.5+) e <a href="/wiki/Timothy_Lovejoy" title="Timothy Lovejoy">Reverendo Lovejoy</a> (ep.5.22) in <i><a href="/wiki/I_Simpson" title="I Simpson">I Simpson</a></i>""")
    }

    @Test
    fun ilariaStagni() {
        val res = run("Ilaria_Stagni")
        assertEntity(res, "Pocahontas (dialoghi)",
            "Pocahontas",
            "Pocahontas II - Viaggio nel nuovo mondo",
            "Ralph spacca Internet"
        )
    }

    @Test
    fun saraLabidi() {
        val res = run("Sara_Labidi")

        assertLinkedEntity(res, "Asuka Soryu Langley"
            , "Neon Genesis Evangelion"
            , "Neon Genesis Evangelion: Death & Rebirth"
            , "Neon Genesis Evangelion: The End of Evangelion"
        )
        assertSource(find(res, "Asuka Soryu Langley", "Neon Genesis Evangelion"),
            """<a href="/wiki/Asuka_S%C5%8Dry%C5%AB_Langley" title="Asuka Sōryū Langley">Asuka Soryu Langley</a> nell'edizione <a href="/wiki/Netflix" title="Netflix">Netflix</a> di <i><a href="/wiki/Neon_Genesis_Evangelion" title="Neon Genesis Evangelion">Neon Genesis Evangelion</a></i>""")
        assertSource(find(res, "Asuka Soryu Langley", "Neon Genesis Evangelion: Death & Rebirth"),
            """<a href="/wiki/Asuka_S%C5%8Dry%C5%AB_Langley" title="Asuka Sōryū Langley">Asuka Soryu Langley</a> nell'edizione Netflix di <i><a href="/wiki/Neon_Genesis_Evangelion:_Death_%26_Rebirth" title="Neon Genesis Evangelion: Death &amp; Rebirth">Neon Genesis Evangelion: Death &amp; Rebirth</a></i> e <i><a href="/wiki/Neon_Genesis_Evangelion:_The_End_of_Evangelion" title="Neon Genesis Evangelion: The End of Evangelion">Neon Genesis Evangelion: The End of Evangelion</a></i>""")

        assertEntity(res, "Olivia nelle prime due stagioni", "Giust'in tempo")
        assertEntity(res, "Miana nell'edizione home video"
            , "Godzilla - Minaccia sulla città"
            , "Godzilla mangiapianeti"
        )

    }

}

internal class ReadDubberSectionSplitTest: ReadDubberSectionBaseTest() {

    @Test
    fun linked() {
        val res = run("Angelo_Maggi")

        find(res, "I Simpson").forEach {
            assertSource(it, """<a href="/wiki/Clancy_Winchester" title="Clancy Winchester">Commissario Winchester</a> (2ª voce e principale, ep.5.5+) e <a href="/wiki/Timothy_Lovejoy" title="Timothy Lovejoy">Reverendo Lovejoy</a> (ep.5.22) in <i><a href="/wiki/I_Simpson" title="I Simpson">I Simpson</a></i>""")
        }
        assertLinkedEntity(res, "Commissario Winchester", "I Simpson")
        assertLinkedEntity(res, "Reverendo Lovejoy", "I Simpson")

        find(res, "I Simpson - Il film").forEach {
            assertSource(it, """<a href="/wiki/Clancy_Winchester" title="Clancy Winchester">Commissario Winchester</a> e <a href="/wiki/Tom_Hanks" title="Tom Hanks">Tom Hanks</a> in <i><a href="/wiki/I_Simpson_-_Il_film" title="I Simpson - Il film">I Simpson - Il film</a></i>""")
        }
        assertLinkedEntity(res, "Commissario Winchester", "I Simpson - Il film")
        assertLinkedEntity(res, "Tom Hanks", "I Simpson - Il film")

        assertLinkedEntity(res, "Tony Stark", "Iron Man: Rise of Technovore")
        assertLinkedEntity(res, "Iron Man", "Iron Man: Rise of Technovore")
    }

    @Test
    fun mixed() {
        val res = run("Sara_Labidi")

        val henryDanger = find(res, "Henry Danger")
        henryDanger.forEach {
            assertSource(it, """Maeve Tomalty, <a href="/wiki/Jade_Pettyjohn" title="Jade Pettyjohn">Jade Pettyjohn</a> e Sedona Cohen in <i><a href="/wiki/Henry_Danger" title="Henry Danger">Henry Danger</a></i>""")
        }
        assertEntity(res, "Maeve Tomalty", "Henry Danger")
        assertLinkedEntity(res, "Jade Pettyjohn", "Henry Danger")
        assertEntity(res, "Sedona", "Henry Danger")
    }

    @Test
    fun unlink() {
        val res = run("Sara_Labidi")

        find(res, "The 100").forEach {
            assertSource(it, """Izabela Vidovic e Lola Flanery in <i><a href="/wiki/The_100" title="The 100">The 100</a></i>""")
        }
        assertEntity(res, "Izabela Vidovic", "The 100")
        assertEntity(res, "Lola Flanery", "The 100")

        find(res, "Komi Can't Communicate").forEach {
            assertSource(it, """Hoshiko Teshigawara e Ayami Sasaki in <i><a href="/wiki/Komi_Can%27t_Communicate" title="Komi Can't Communicate">Komi Can't Communicate</a></i>""")
        }
        assertEntity(res, "Hoshiko Teshigawara", "Komi Can't Communicate")
        assertEntity(res, "Ayami Sasaki", "Komi Can't Communicate")
    }

}

fun find(res: TaskResult, name: String): List<DubbedEntity> {
    val dubbed = res.dubbedEntities?.filter { it.name == name }
    assertNotNull(dubbed, name)
    assertFalse(dubbed!!.isEmpty(), name)
    return dubbed
}

fun find(res: TaskResult, name: String, movie: String): DubbedEntity {
    val dubbed = res.dubbedEntities?.first { it.name == name && it.movie.name == movie }
    assertNotNull(dubbed, name)
    return dubbed!!
}

fun assertEntity(res: TaskResult, dubber: DubberRef, name: String, linked: Boolean, vararg movies: String) {
    val entities = find(res, name)
    entities.forEach { assertEquals(dubber, it.dubber, it.name) }
    entities.forEach { assertEquals(entities[0].actor, it.actor, "$name actor refs") }
    if (linked)
        entities.forEach { assertNotNull(it.wikiId, "${it.name} wiki") }
    else
        entities.forEach { assertNull(it.wikiId, "${it.name} wiki") }
    movies.forEach { assertMovie(entities, it) }
}

fun assertMovie(entities: List<DubbedEntity>, movie: String) {
    val entity = entities.first { it.movie.name == movie }
    assertNotNull(entity, movie)
    assertNotNull(entity.movie.wikiId, "movie $movie not linked")
}

fun assertSource(entity: DubbedEntity, raw: String) {
    assertEquals(1, entity.sources.size, "${entity.name} sources")
    assertEquals(raw, entity.sources[0].raw)
}