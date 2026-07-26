package takutility.dubdb.db

import com.mongodb.client.FindIterable
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoCursor
import com.mongodb.client.model.ReplaceOptions
import org.bson.conversions.Bson
import org.bson.types.ObjectId
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import takutility.dubdb.entities.*
import java.time.LocalDate

internal class MongoRepositoryTest {

    private fun <T : Any> mockFindIterable(items: List<T>): FindIterable<T> {
        val findIterable = mock<FindIterable<T>>()
        val cursor = mock<MongoCursor<T>>()
        var index = 0

        whenever(cursor.hasNext()).thenAnswer { index < items.size }
        whenever(cursor.next()).thenAnswer { items[index++] }
        whenever(findIterable.iterator()).thenReturn(cursor)
        whenever(findIterable.sort(any())).thenReturn(findIterable)
        whenever(findIterable.into(any())).thenAnswer { inv ->
            val list = inv.getArgument<MutableList<T>>(0)
            list.addAll(items)
            list
        }
        return findIterable
    }

    @Test
    fun testSaveNewEntity() {
        val collection = mock<MongoCollection<Movie>>()
        val repo = MongoMovieRepository(collection)

        val movie = Movie("Inception")
        assertNull(movie.id)

        val saved = repo.save(movie)

        assertNotNull(saved.id)
        assertTrue(ObjectId.isValid(saved.id))
        verify(collection).insertOne(eq(movie))
    }

    @Test
    fun testSaveExistingEntity() {
        val collection = mock<MongoCollection<Movie>>()
        val repo = MongoMovieRepository(collection)

        val validId = ObjectId().toHexString()
        val movie = Movie("Inception").apply { id = validId }

        val saved = repo.save(movie)

        assertEquals(validId, saved.id)
        verify(collection).replaceOne(any<Bson>(), eq(movie), any<ReplaceOptions>())
    }

    @Test
    fun testFindByIdValid() {
        val collection = mock<MongoCollection<Movie>>()
        val repo = MongoMovieRepository(collection)

        val validId = ObjectId().toHexString()
        val movie = Movie("Inception").apply { id = validId }
        val findIterable = mockFindIterable(listOf(movie))

        whenever(collection.find(any<Bson>())).thenReturn(findIterable)

        val result = repo.findById(validId)

        assertNotNull(result)
        assertEquals("Inception", result?.name)
    }

    @Test
    fun testFindByIdInvalid() {
        val collection = mock<MongoCollection<Movie>>()
        val repo = MongoMovieRepository(collection)

        val result = repo.findById("invalid-id")
        assertNull(result)
        verify(collection, never()).find(any<Bson>())
    }

    @Test
    fun testFindBySource() {
        val collection = mock<MongoCollection<Movie>>()
        val repo = MongoMovieRepository(collection)

        val movie = Movie("Inception", ids = SourceIds.of(Source.IMDB to "tt1375666"))
        val findIterable = mockFindIterable(listOf(movie))

        whenever(collection.find(any<Bson>())).thenReturn(findIterable)

        val results = repo.findBySource(SourceId(Source.IMDB, "tt1375666"))

        assertEquals(1, results.size)
        assertEquals("Inception", results[0].name)
    }

    @Test
    fun testFindBySources() {
        val collection = mock<MongoCollection<Movie>>()
        val repo = MongoMovieRepository(collection)

        val movie = Movie("Inception", ids = SourceIds.of(Source.TRAKT to "12345"))
        val findIterable = mockFindIterable(listOf(movie))

        whenever(collection.find(any<Bson>())).thenReturn(findIterable)

        val results = repo.findBySources(SourceIds.of(Source.TRAKT to "12345"))

        assertEquals(1, results.size)
        assertEquals("Inception", results[0].name)
    }

    @Test
    fun testDubberRepositoryFindMostRecent() {
        val collection = mock<MongoCollection<Dubber>>()
        val repo = MongoDubberRepository(collection)

        val dubber1 = Dubber("Dubber 1", lastUpdate = LocalDate.parse("2024-08-18"))
        val dubber2 = Dubber("Dubber 2", lastUpdate = LocalDate.parse("2024-09-22"))
        val findIterable = mockFindIterable(listOf(dubber2, dubber1))

        whenever(collection.find(any<Bson>())).thenReturn(findIterable)

        val recent = repo.findMostRecent(5)

        assertEquals(2, recent.size)
        assertEquals("Dubber 2", recent[0].name)
        assertEquals("Dubber 1", recent[1].name)
    }

    @Test
    fun testDubbedEntityRepositoryFindByRef() {
        val collection = mock<MongoCollection<DubbedEntity>>()
        val repo = MongoDubbedEntityRepository(collection)

        val dubberRef = DubberRefImpl("Dubber Ref", SourceIds.of(Source.MONDO_DOPPIATORI to "123"))
        val entity = DubbedEntity(
            name = "Test Entity",
            movie = movieRefOf("Movie Ref"),
            dubber = dubberRef
        )
        val findIterable = mockFindIterable(listOf(entity))

        whenever(collection.find(any<Bson>())).thenReturn(findIterable)

        val results = repo.findByRef(dubberRef)

        assertEquals(1, results.size)
        assertEquals("Test Entity", results[0].name)
    }
}
