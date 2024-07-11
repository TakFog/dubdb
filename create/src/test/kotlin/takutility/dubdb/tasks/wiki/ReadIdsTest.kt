package takutility.dubdb.tasks.wiki

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import takutility.dubdb.entities.Source
import takutility.dubdb.entities.Source.*
import takutility.dubdb.entities.SourceId
import takutility.dubdb.tasks.TaskResult
import takutility.dubdb.wiki.CachedWikiPageLoader

internal class ReadIdsTest {

    companion object {

        private lateinit var task: ReadIds

        @BeforeAll
        @JvmStatic
        fun setup() {
            task = ReadIds(CachedWikiPageLoader("src/test/resources/cache"))
        }

        fun run(title: String): TaskResult = task.run(SourceId(Source.WIKI, title))
    }

    @Test
    fun missing() {
        assertTrue(task.run(null).isEmpty())
    }

    @Test
    fun wrongId() {
        assertTrue(task.run(SourceId(Source.MONDO_DOPPIATORI, "pippo")).isEmpty())
    }

    @Test
    fun avengers() {
        assertIds(run("Avengers:_Age_of_Ultron"),
                MONDO_DOPPIATORI to "doppiaggio/film1/avengers-ageofultron.htm",
                IMDB to "tt2395427",
                WIKIDATA to "Q14171368",
                WIKI_EN to "Avengers:_Age_of_Ultron",
        ) }

    @Test
    fun angeloMaggi() {
        assertIds(run("Angelo_Maggi"),
                MONDO_DOPPIATORI to "doppiaggio/voci/vociamag.htm",
                IMDB to "nm0535947",
                WIKIDATA to "Q3617056",
                WIKI_EN to "Angelo_Maggi",
        )
    }

    @Test
    fun robertDowneyJr() {
        assertIds(run("Robert_Downey_Jr."),
                IMDB to "nm0000375",
                WIKIDATA to "Q165219",
                WIKI_EN to "Robert_Downey_Jr.",
        )
    }
}

fun assertId(expected: String, res: TaskResult, source: Source) {
    assertEquals(expected, res.id(source), source.name)
}

fun assertIds(res: TaskResult, vararg values: Pair<Source, String>) {
    values.forEach { assertId(it.second, res, it.first) }
    assertEquals(values.size, res.sourceIds.size)
}