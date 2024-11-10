package takutility.dubdb.tasks.wiki

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import takutility.dubdb.DubDbContext
import takutility.dubdb.entities.Source
import takutility.dubdb.entities.Source.*
import takutility.dubdb.entities.SourceId
import takutility.dubdb.tasks.TaskResult

internal class ReadIdsTest: WikiPageTest<ReadIds>() {

    override fun newTask(context: DubDbContext) = ReadIds(context)
    fun run(title: String): TaskResult = task.run(SourceId(WIKI, title))

    @Test
    fun missing() {
        assertTrue(task.run(null).isEmpty())
    }

    @Test
    fun wrongId() {
        assertTrue(task.run(SourceId(MONDO_DOPPIATORI, "pippo")).isEmpty())
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
    fun maxTurilli() {
        assertIds(run("Max_Turilli"),
            MONDO_DOPPIATORI to "doppiaggio/voci/vocimturi.htm",
            IMDB to "nm0850442",
            WIKIDATA to "Q3853081",
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