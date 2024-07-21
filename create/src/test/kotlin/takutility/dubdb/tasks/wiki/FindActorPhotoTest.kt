package takutility.dubdb.tasks.wiki

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import takutility.dubdb.entities.Actor
import takutility.dubdb.entities.Source
import takutility.dubdb.entities.SourceIds
import takutility.dubdb.tasks.TaskResult
import takutility.dubdb.wiki.WikiPageLoader


internal class FindActorPhotoTest: WikiPageTest<FindActorPhoto>() {
    override fun newTask(loader: WikiPageLoader) = FindActorPhoto(loader)
    fun run(name: String): TaskResult = task.run(Actor(name, ids = SourceIds.of(Source.WIKI to name)))

    @Test
    fun unlinkedActor() {
        val res = task.run(Actor("dummy"))
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

    fun assertWikimedia(res: TaskResult, expected: String? = null) {
        assertWikimedia(res.actor(), expected)
    }
}