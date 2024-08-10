package takutility.dubdb.tasks.wiki

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import takutility.dubdb.entities.*
import takutility.dubdb.tasks.TaskResult
import takutility.dubdb.wiki.WikiPageLoader

internal class ReadDubberSectionTest: WikiPageTest<ReadDubberSection>() {
    override fun newTask(loader: WikiPageLoader) = ReadDubberSection(loader)

    var dubber: DubberRef = DubberRefImpl()

    private fun dubber(title: String): DubberRef {
        dubber = DubberRefImpl(ids = SourceIds.of(Source.WIKI to title))
        return dubber
    }

    fun run(title: String? = null): TaskResult {
        if (title != null) dubber(title)
        return task.run(dubber)
    }

    @Test
    fun angeloMaggi() {
        val parser = run("Angelo_Maggi")
        assertNotNull(parser.dubbedEntities)
        assertFalse(parser.dubbedEntities!!.isEmpty())

        assertLinkedEntity(parser, "Tom Hanks"
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

        assertLinkedEntity(parser, "Ted Levine", "Shutter Island")
        assertLinkedEntity(parser, "Tim Preece", "L'uomo nell'ombra")
        assertLinkedEntity(parser, "Jack Coleman", "Castle")
        assertEntity(parser, "Scimmia",
            "Kung Fu Panda",
            "Kung Fu Panda 2",
            "Kung Fu Panda 3",
            "Kung Fu Panda - Mitiche avventure"
        )
        assertEntity(parser, "Ryo Nagare",
            "Il Grande Mazinga contro Getta Robot",
            "Il Grande Mazinga contro Getta Robot G",
            "Il Grande Mazinga, Getta Robot G, UFO Robot Goldrake contro il Dragosauro"
        )
        assertLinkedEntity(parser, "Guile", "Street Fighter II V")
        assertLinkedEntity(parser, "David Aston", "Matrix")
    }

    @Test
    fun actorMissing_angeloMaggi() {
        val parser = run("Angelo_Maggi")

        //Andy Secombe
        assertEntity(parser, "Andy Secombe",
            "Star Wars: Episodio I - La minaccia fantasma",
            "Star Wars: Episodio II - L'attacco dei cloni"
        )
        val secombe = find(parser, "Andy Secombe")[0]
        assertEquals("Andy_Secombe", secombe.ids[Source.WIKI_MISSING])

        // Ronald Falk
        assertEntity(parser, "Ronald Falk",
            "Star Wars: Episodio II - L'attacco dei cloni"
        )
        val falk = find(parser, "Ronald Falk")[0]
        assertEquals("Ronald_Falk", falk.ids[Source.WIKI_MISSING])
    }

    @Test
    fun dubAttributes_angeloMaggi() {
        val parser = run("Angelo_Maggi")
        assertEntity(parser, "Yotsuya (1ª voce)", "Cara dolce Kyoko")
        assertEntity(parser, "Jeremy (2ª vers. italiana)", "Jenny la tennista")
    }

    @Test
    fun splitLinked_angeloMaggi() {
        val parser = run("Angelo_Maggi")
        assertLinkedEntity(parser, "Commissario Winchester", "I Simpson")
        assertLinkedEntity(parser, "Reverendo Lovejoy", "I Simpson")

        assertLinkedEntity(parser, "Commissario Winchester", "I Simpson - Il film")
        assertLinkedEntity(parser, "Tom Hanks", "I Simpson - Il film")

        assertLinkedEntity(parser, "Tony Stark", "Iron Man: Rise of Technovore")
        assertLinkedEntity(parser, "Iron Man", "Iron Man: Rise of Technovore")
    }

    // -----------------------------

    fun find(res: TaskResult, name: String): List<DubbedEntity> {
        val dubbed = res.dubbedEntities?.filter { it.name == name }
        assertNotNull(dubbed, name)
        assertFalse(dubbed!!.isEmpty(), name)
        return dubbed
    }

    fun assertLinkedEntity(res: TaskResult, name: String, vararg movies: String) {
        assertEntity(res, dubber, name, true, *movies)
    }

    fun assertEntity(res: TaskResult, name: String, vararg movies: String) {
        assertEntity(res, dubber, name, false, *movies)
    }

    fun assertEntity(res: TaskResult, dubber: DubberRef, name: String, linked: Boolean, vararg movies: String) {
        val entities = find(res, name)
        entities.forEach { assertEquals(dubber, it.dubber, it.name) }
        if (linked)
            assertNotNull(entities[0].actor?.wikiId)
        else
            assertNull(entities[0].actor?.wikiId)
        entities.forEach { assertEquals(entities[0].actor, it.actor, "$name actor refs") }
        movies.forEach { assertMovie(entities, it) }
    }

    fun assertMovie(entities: List<DubbedEntity>, movie: String) {
        val entity = entities.first { it.movie.name == movie }
        assertNotNull(entity, movie)
        assertNotNull(entity.movie.wikiId, "movie $movie not linked")
    }
}