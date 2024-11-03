package takutility.dubdb.ops.dubber

import org.junit.jupiter.api.BeforeEach
import takutility.dubdb.DubDbContext
import takutility.dubdb.TestContext
import takutility.dubdb.db.MemDubbedEntityRepository
import takutility.dubdb.db.MemDubberRepository

internal class ExtractDubberTest {
    lateinit var ctx: DubDbContext

    @BeforeEach
    fun setUp() {
        ctx = TestContext.mocked {
            dubberDb = MemDubberRepository()
            dubEntityDb = MemDubbedEntityRepository()
        }
    }
}