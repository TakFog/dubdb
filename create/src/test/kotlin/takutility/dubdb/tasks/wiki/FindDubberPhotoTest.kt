package takutility.dubdb.tasks.wiki

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import takutility.dubdb.entities.Dubber
import takutility.dubdb.entities.Source
import takutility.dubdb.entities.SourceIds
import takutility.dubdb.tasks.TaskResult
import takutility.dubdb.wiki.WikiPageLoader


internal class FindDubberPhotoTest: WikiPageTest<FindDubberPhoto>() {
    override fun newTask(loader: WikiPageLoader) = FindDubberPhoto(loader)
    fun run(name: String): TaskResult = task.run(Dubber(name, ids = SourceIds.of(Source.WIKI to name)))

    @Test
    fun unlinkedDubber() {
        val res = task.run(Dubber("dummy"))
        assertTrue(res.isEmpty())
    }

    @Test
    fun angeloMaggi() {
        val res = run("Angelo_Maggi")
        assertWikimedia(res, "Angelo_Maggi_20240113.jpg")
    }

    @Test
    fun maxTurilli() {
        val res = run("Max_Turilli")
        assertWikimedia(res, "Max_Turilli_Sequestro_di_persona.png")
    }

    @Test
    fun ninoDAgata() {
        val res = run("Nino_D'Agata")
        assertWikimedia(res)
    }

    fun assertWikimedia(res: TaskResult, expected: String? = null) {
        assertWikimedia(res.dubber(), expected)
    }
}