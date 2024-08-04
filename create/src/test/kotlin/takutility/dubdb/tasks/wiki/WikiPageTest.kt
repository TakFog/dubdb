package takutility.dubdb.tasks.wiki

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import takutility.dubdb.entities.EntityRef
import takutility.dubdb.entities.ImmutableSourceIds
import takutility.dubdb.entities.Source
import takutility.dubdb.wiki.CachedWikiPageLoader
import takutility.dubdb.wiki.WikiPageLoader

abstract class WikiPageTest<T: WikiPageTask> {
    companion object {

        private lateinit var loader: CachedWikiPageLoader

        @BeforeAll
        @JvmStatic
        fun setup() {
            loader = CachedWikiPageLoader("src/test/resources/cache")
        }
    }

    protected lateinit var task: T

    @BeforeEach
    fun before() {
        task = newTask(loader)
    }

    abstract fun newTask(loader: WikiPageLoader): T

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