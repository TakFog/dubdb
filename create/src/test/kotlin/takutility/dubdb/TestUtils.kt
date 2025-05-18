package takutility.dubdb

import org.bson.codecs.DecoderContext
import org.bson.json.JsonReader
import org.junit.jupiter.api.Assertions.*
import takutility.dubdb.db.codec.codecRegistry
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

fun <T> assertEqualsUnordered(expected: List<T>?, actual: List<T>?) {
    if (expected == null) {
        assertNull(actual)
        return
    }
    assertEquals(expected.size, actual!!.size)
    expected.forEach { assertTrue(actual.contains(it), "$it not found") }
    actual.forEach { assertTrue(expected.contains(it), "$it unexpected") }
}

inline fun <reified T> fromJson(json: String): T = codecRegistry[T::class.java].decode(JsonReader(json),
    DecoderContext.builder().build())
