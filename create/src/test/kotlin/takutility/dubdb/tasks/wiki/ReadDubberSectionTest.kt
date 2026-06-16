package takutility.dubdb.tasks.wiki

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import takutility.dubdb.DubDbContext
import takutility.dubdb.entities.*
import takutility.dubdb.tasks.TaskResult

internal open class ReadDubberSectionBaseTest: WikiPageTest<ReadDubberSection>() {
    override fun newTask(context: DubDbContext) = ReadDubberSection(context)

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
        assertLinkedEntity(res, "Jack Coleman", "Castle")

        assertEntity(res, "Ryo Nagare",
            "Il Grande Mazinga contro Getta Robot",
            "Il Grande Mazinga contro Getta Robot G",
            "UFO Robot Goldrake, il Grande Mazinga e Getta Robot G contro il Dragosauro",
        )
        assertLinkedEntity(res, "Guile", "Street Fighter II V")
    }

    @Test
    fun differentLink_angeloMaggi() {
        val res = run("Angelo_Maggi")
        val scimmia = find(res, "Scimmia")

        assertMovie(scimmia, "Kung Fu Panda")
        assertMovie(scimmia, "Kung Fu Panda 2")
        assertMovie(scimmia, "Kung Fu Panda 3")
        assertMovie(scimmia, "Kung Fu Panda - Mitiche avventure")

        scimmia.forEach {
            if (it.movie.name == "Kung Fu Panda - Mitiche avventure")
                assertNull(it.wiki)
            else
                assertNotNull(it.wiki)
        }
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

        find(res, "Tim Roth", "Invincibile").sources.apply {
            assertEquals(1, size)
            assertEquals("""* [[Tim Roth]] in ''[[Invincibile]]'', ''[[Dark Water (film 2005)|Dark Water]]'', ''[[Un'altra giovinezza]]'', ''[[Grace di Monaco (film)|Grace di Monaco]]''"""
                , get(0).raw)
        }

        find(res, "Padre di Azur", "Azur e Asmar").sources.apply {
            assertEquals(1, size)
            assertEquals("""* Padre di Azur in ''[[Azur e Asmar]]''"""
                , get(0).raw)
        }
    }

    @Test
    @Disabled("no more missing detection")
    fun actorMissing_angeloMaggi() {
        val res = run("Angelo_Maggi")

        assertMissingEntity(res, "David Aston", "Matrix")
        assertMissingEntity(res, "Tim Preece", "L'uomo nell'ombra")
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
    @Disabled("no more missing detection")
    fun actorMissing_gabrielePatriarca() {
        val res = run("Gabriele_Patriarca_(doppiatore)")
        assertMissingEntity(res, "Martin Svetlik",
            "I fratelli Grimm e l'incantevole strega")
    }

    @Test
    @Disabled("no more missing detection")
    fun movieMissing_gabrielePatriarca() {
        val res = run("Gabriele_Patriarca_(doppiatore)")

        find(res, "Frank Brown").forEach{
            assertNull(it.movie.wikiId, "${it.name} wiki")
            assertNotNull(it.movie.ids[Source.WIKI_MISSING], "${it.name} missing")
        }

        find(res, "Brian").forEach{
            if (it.name != "Taddeo l'esploratore e la tavola di smeraldo") return
            assertNull(it.movie.wikiId, "${it.name} wiki")
            assertNotNull(it.movie.ids[Source.WIKI_MISSING], "${it.name} missing")
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

    @Test
    fun maurizioMerluzzo() {
        val res = run("Maurizio_Merluzzo")
        assertNotNull(res.dubbedEntities)
        assertFalse(res.dubbedEntities!!.isEmpty())

        val movies = arrayOf(
            "Shazam!",
            "The Mauritanian",
            "Shazam! Furia degli dei",
            "Il magico mondo di Harold",
            "Il bambino di cristallo",
            "La fantastica signora Maisel",
        )

        assertLinkedEntity(res, "Zachary Levi", *movies)
        val levi = find(res, "Zachary Levi")
        assertEquals(movies.size, levi.size, levi.map { it.movie.name }.toString())
    }

    @Test
    fun alexPolidori() {
        val res = run("Alex_Polidori")
        assertNotNull(res.dubbedEntities)
        val entities = res.dubbedEntities!!
        assertFalse(entities.isEmpty())

        assertEquals(0, entities.count { it.name.contains("Musa d'Oro") }, "Musa d'Oro")
        assertEquals(0, entities.count { it.name.contains("Voce di Cartoonia") }, "Voce di Cartoonia")
    }
}

@Disabled
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
            """* [[Clancy Winchester|Commissario Winchester]] (2ª voce e principale, dall'ep.5.5) e [[Timothy Lovejoy|Reverendo Lovejoy]] (ep.5.22) in ''[[I Simpson]]'""")
    }

    @Test
    fun movieAttr_angeloMaggi() {
        val res = run("Angelo_Maggi")

        assertLinkedEntity(res, "Clark Kent", "Superman")
        assertLinkedEntity(res, "Superman", "Superman")
        assertSource(find(res, "Superman", "Superman"),
            """* [[Superman|Clark Kent/Superman]] in ''[[Superman (serie animata 1996)|Superman]]'' (stagioni 1-2)""")
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

        assertEntity(res, "Olivia nelle prime due stagioni", "Giust'in tempo")
        assertEntity(res, "Miana nell'edizione home video"
            , "Godzilla - Minaccia sulla città"
            , "Godzilla mangiapianeti"
        )

        assertLinkedEntity(res, "Asuka Soryu Langley"
            , "Neon Genesis Evangelion"
            , "Neon Genesis Evangelion: Death & Rebirth"
            , "Neon Genesis Evangelion: The End of Evangelion"
        )
        assertSource(find(res, "Asuka Soryu Langley", "Neon Genesis Evangelion"),
            """* [[Asuka Sōryū Langley|Asuka Soryu Langley]] nell'edizione Netflix di ''[[Neon Genesis Evangelion: Death & Rebirth]]'' e ''[[Neon Genesis Evangelion: The End of Evangelion]]''""")
        assertSource(find(res, "Asuka Soryu Langley", "Neon Genesis Evangelion: Death & Rebirth"),
            """* [[Asuka Sōryū Langley|Asuka Soryu Langley]] nell'edizione Netflix di ''[[Neon Genesis Evangelion: Death & Rebirth]]'' e ''[[Neon Genesis Evangelion: The End of Evangelion]]''""")

    }

}

internal class ReadDubberSectionSplitTest: ReadDubberSectionBaseTest() {

    @Test
    fun linked() {
        val res = run("Angelo_Maggi")

        findMovie(res, "I Simpson - Il film").forEach {
            assertSource(it, """* [[Clancy Winchester|Commissario Winchester]] e [[Tom Hanks]] in ''[[I Simpson - Il film]]''""")
        }
        assertLinkedEntity(res, "Commissario Winchester", "I Simpson - Il film")
        assertLinkedEntity(res, "Tom Hanks", "I Simpson - Il film")
    }

    @Disabled
    @Test
    fun linkedAttributes() {
        val res = run("Angelo_Maggi")

        findMovie(res, "I Simpson").forEach {
            assertSource(it, """* [[Clancy Winchester|Commissario Winchester]] (2ª voce e principale, dall'ep.5.5) e [[Timothy Lovejoy|Reverendo Lovejoy]] (ep.5.22) in ''[[I Simpson]]''""")
        }
        assertLinkedEntity(res, "Commissario Winchester", "I Simpson")
        assertLinkedEntity(res, "Reverendo Lovejoy", "I Simpson")
    }

    @Test
    fun linkedSlash() {
        val res = run("Angelo_Maggi")

        findMovie(res, "Iron Man: Rise of Technovore").forEach {
            assertSource(it, """* [[Iron Man|Tony Stark/Iron Man]] in ''[[Iron Man: Rise of Technovore]]''""")
        }
        assertLinkedEntity(res, "Tony Stark", "Iron Man: Rise of Technovore", "What If...?")
        assertLinkedEntity(res, "Iron Man", "Iron Man: Rise of Technovore", "What If...?")

        findMovie(res, "Superman").forEach {
            assertSource(it, """* [[Superman|Clark Kent/Superman]] in ''[[Superman (serie animata 1996)|Superman]]'' (stagioni 1-2)""")
        }
        assertLinkedEntity(res, "Clark Kent", "Superman")
        assertLinkedEntity(res, "Superman", "Superman")
    }

    @Disabled
    @Test
    fun mixed() {
        val res = run("Sara_Labidi")

        val henryDanger = findMovie(res, "Henry Danger")
        henryDanger.forEach {
            assertSource(it, """* Maeve Tomalty, [[Jade Pettyjohn]] e Sedona Cohen in ''[[Henry Danger]]''""")
        }
        assertEntity(res, "Maeve Tomalty", "Henry Danger")
        assertLinkedEntity(res, "Jade Pettyjohn", "Henry Danger")
        assertEntity(res, "Sedona", "Henry Danger")
    }

    @Disabled
    @Test
    fun unlink() {
        val res = run("Sara_Labidi")

        findMovie(res, "The 100").forEach {
            assertSource(it, """* Izabela Vidovic e Lola Flanery in ''[[The 100]]''""")
        }
        assertEntity(res, "Izabela Vidovic", "The 100")
        assertEntity(res, "Lola Flanery", "The 100")

        find(res, "Komi Can't Communicate").forEach {
            assertSource(it, """* Hoshiko Teshigawara e Ayami Sasaki in ''[[Komi Can't Communicate]]''""")
        }
        assertEntity(res, "Hoshiko Teshigawara", "Komi Can't Communicate")
        assertEntity(res, "Ayami Sasaki", "Komi Can't Communicate")
    }

}

fun find(res: TaskResult, name: String): List<DubbedEntity> {
    val dubbed = res.dubbedEntities?.filter { it.name == name }
    assertNotNull(dubbed, "$name not found")
    assertFalse(dubbed!!.isEmpty(), "$name not found")
    return dubbed
}

fun findMovie(res: TaskResult, movie: String): List<DubbedEntity> {
    val dubbed = res.dubbedEntities?.filter { it.movie.name == movie }
    assertNotNull(dubbed, "$movie not found")
    assertFalse(dubbed!!.isEmpty(), "$movie not found")
    return dubbed
}

fun find(res: TaskResult, name: String, movie: String): DubbedEntity {
    val dubbed = res.dubbedEntities?.first { it.name == name && it.movie.name == movie }
    assertNotNull(dubbed, name)
    return dubbed!!
}

fun assertEntity(res: TaskResult, dubber: DubberRef, name: String, linked: Boolean, vararg movies: String) {
    val entities = find(res, name)
    entities.drop(1).forEach { assertNotSame(entities[0].ids, it.ids) }
    entities.forEach { assertEquals(dubber, it.dubber, it.name) }
    entities.forEach { assertEquals(entities[0].actor, it.actor, "$name actor refs") }
    if (linked)
        entities.forEach { assertNotNull(it.wikiId, "${it.name} wiki") }
    else
        entities.forEach { assertNull(it.wikiId, "${it.name} wiki") }
    movies.forEach { assertMovie(entities, it) }
}

fun assertMovie(entities: List<DubbedEntity>, movie: String) {
    assertFalse(entities.isEmpty(), "$movie empty")
    val entity = try {
        entities.first { it.movie.name == movie }
    } catch (e: NoSuchElementException) {
        fail("$movie not found in ${entities.map { it.movie.name }}")
    }
    assertNotNull(entity, movie)
    assertNotNull(entity.movie.wikiId, "movie $movie not linked")
}

fun assertSource(entity: DubbedEntity, raw: String) {
    assertEquals(1, entity.sources.size, "${entity.name} sources")
    assertEquals(raw, entity.sources[0].raw)
}