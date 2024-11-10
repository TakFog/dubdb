package takutility.dubdb.tasks.wiki

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import takutility.dubdb.DubDbContext
import takutility.dubdb.entities.*
import takutility.dubdb.tasks.TaskResult


internal class FindPhotoTest: WikiPageTest<FindPhoto>() {
    override fun newTask(context: DubDbContext) = FindPhoto(context)
    fun run(name: String): TaskResult = task.run(EntityRefImpl(ids = SourceIds.of(Source.WIKI to name)))

    @Test
    fun unlinkedActor() {
        val res = task.run(Actor("dummy"))
        assertTrue(res.isEmpty())
    }

    @Test
    fun unlinkedDubber() {
        val res = task.run(Dubber("dummy"))
        assertTrue(res.isEmpty())
    }

    @Test
    fun robertDowneyJr() {
        val res = run("Robert_Downey_Jr.")
        assertWikimedia(res, "Robert_Downey_Jr_2014_Comic_Con_(cropped).jpg")
    }

    @Test
    fun maxTurilli() {
        val res = run("Max_Turilli")
        assertWikimedia(res, "Max_Turilli_Sequestro_di_persona.png")
    }

    @Test
    fun lewisTan() {
        val res = run("Lewis_Tan")
        assertWikimedia(res)
    }

    @Test
    fun angeloMaggi() {
        val res = run("Angelo_Maggi")
        assertWikimedia(res, "Angelo_Maggi_20240113.jpg")
    }

    @Test
    fun ninoDAgata() {
        val res = run("Nino_D'Agata")
        assertWikimedia(res)
    }

    fun assertWikimedia(res: TaskResult, expected: String? = null) {
        assertWikimedia(res.sourceIds, expected)
    }
}