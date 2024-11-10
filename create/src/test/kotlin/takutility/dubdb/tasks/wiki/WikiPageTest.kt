package takutility.dubdb.tasks.wiki

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import takutility.dubdb.DubDbContext
import takutility.dubdb.TestContext
import takutility.dubdb.entities.EntityRef
import takutility.dubdb.entities.ImmutableSourceIds
import takutility.dubdb.entities.Source

abstract class WikiPageTest<T: WikiPageTask> {
    protected lateinit var task: T

    @BeforeEach
    fun before() {
        val context = TestContext.mocked()
        task = newTask(context)
    }

    abstract fun newTask(context: DubDbContext): T

    fun assertWikimedia(entity: EntityRef?, expected: String? = null) {
        assertNotNull(entity, "missing entity")
        assertWikimedia(entity!!.ids, expected)
    }

    fun assertWikimedia(ids: ImmutableSourceIds?, expected: String? = null) {
        val id = ids?.get(Source.WIKIMEDIA)?.id
        if (expected != null) {
            assertEquals(expected, id)
        } else
            assertNull(id)
    }
}