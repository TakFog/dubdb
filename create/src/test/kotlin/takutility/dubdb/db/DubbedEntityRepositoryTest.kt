package takutility.dubdb.db

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import takutility.dubdb.entities.*
import takutility.dubdb.entities.Source.DUBDB
import takutility.dubdb.entities.Source.WIKI

internal abstract class DubbedEntityRepositoryTest<R: DubbedEntityRepository>: RepositoryTest<DubbedEntity, R>() {

    override fun newEntity(name: String, ids: SourceIds, parsed: Boolean, sources: MutableList<RawData>) = DubbedEntity(
        name = name, ids = ids, parsed = parsed, sources = sources,
        movie = movieRefOf()
    )

    @Test
    fun findMostCommonMovie() {
        val movies = listOf<Movie>(
            /* 0 */ Movie("Everest", ids = SourceIds.of(WIKI to "Everest")),
            /* 1 */ Movie("Most Wanted", ids = SourceIds.of(WIKI to "Most_Wanted")),
            /* 2 */ Movie("Rules of Engagement", ids = SourceIds.of(WIKI to "Rules_of_Engagement", DUBDB to "d2a1e3df-9396-4df9-822f-5497a2ad3164")),
            /* 3 */ Movie("Mogambo", ids = SourceIds.of(WIKI to "Mogambo")),
            /* 4 */ Movie("Alice in Wonderland", ids = SourceIds.of(WIKI to "Alice_in_Wonderland")),
            /* 5 */ Movie("Everyone Else", ids = SourceIds.of(WIKI to "Everyone_Else_(Alle_Anderen)")),
            /* 6 */ Movie("Aladdin", ids = SourceIds.of(WIKI to "Aladdin", DUBDB to "ac9bd1db-cf3a-40f3-8adc-b5f1218a3b29")),
        )

        repo.save(DubbedEntity("Alan", movies[0]))
        repo.save(DubbedEntity("Lilly", movies[5]))
        repo.save(DubbedEntity("Leroi", movies[3]))
        repo.save(DubbedEntity("Bridie", movies[4]))
        repo.save(DubbedEntity("Cherrita", movies[3]))
        repo.save(DubbedEntity("Lanie", movies[4]))
        repo.save(DubbedEntity("Frieda", movies[0]))
        repo.save(DubbedEntity("Merilee", movies[2]))
        repo.save(DubbedEntity("Des", movies[4]))
        repo.save(DubbedEntity("Lane", movies[6]))
        repo.save(DubbedEntity("Melisse", movies[0]))
        repo.save(DubbedEntity("Amii", movies[6]))
        repo.save(DubbedEntity("Marie-ann", movies[3]))
        repo.save(DubbedEntity("Lincoln", movies[4]))
        repo.save(DubbedEntity("Briny", movies[1]))
        repo.save(DubbedEntity("Silva", movies[2]))
        repo.save(DubbedEntity("Moises", movies[6]))
        repo.save(DubbedEntity("Udale", movies[3]))
        repo.save(DubbedEntity("Rania", movies[6]))
        repo.save(DubbedEntity("Wyatt", movies[6]))
        repo.save(DubbedEntity("Inna", movies[3]))

        /*
        movie[3]	5
        movie[6]	5
        movie[4]	4
        movie[0]	3
        movie[2]	2
        movie[1]	1
        movie[5]	1
         */

        val result = repo.findMostCommonMovies(3)

        assertEquals(3, result.size)
        assertEquals(movies[3], result[0])
        assertEquals(movies[4], result[1])
        assertEquals(movies[0], result[2])
    }
}