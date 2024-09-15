package takutility.dubdb

import org.junit.jupiter.api.Assertions.*
import takutility.dubdb.entities.EntityRef
import takutility.dubdb.entities.MovieRef

fun <R: EntityRef> assertRefEquals(expected: R?, actual: R?) {
    if (expected == null)
        assertNull(actual)
    else {
        assertNotNull(actual)
        assertEquals(expected.name, actual!!.name, "name")
        assertEquals(expected.ids, actual.ids, "ids")
    }
}

fun assertRefEquals(expected: MovieRef?, actual: MovieRef?) {
    if (expected == null) {
        assertNull(actual)
        return
    }
    assertRefEquals(expected as EntityRef, actual as EntityRef)
    assertEquals(expected.type, actual.type)
}