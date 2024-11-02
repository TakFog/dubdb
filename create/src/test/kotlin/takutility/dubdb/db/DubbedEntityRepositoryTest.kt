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
        val movies = listOf<MovieRef>(
            /* 0 */ movieRefOf("Everest", ids = SourceIds.of(WIKI to "Everest")),
            /* 1 */ movieRefOf("Most Wanted", ids = SourceIds.of(WIKI to "Most_Wanted")),
            /* 2 */ movieRefOf("Rules of Engagement", ids = SourceIds.of(WIKI to "Rules_of_Engagement", DUBDB to "d2a1e3df-9396-4df9-822f-5497a2ad3164")),
            /* 3 */ movieRefOf("Mogambo", ids = SourceIds.of(WIKI to "Mogambo")),
            /* 4 */ movieRefOf("Alice in Wonderland", ids = SourceIds.of(WIKI to "Alice_in_Wonderland")),
            /* 5 */ movieRefOf("Everyone Else", ids = SourceIds.of(WIKI to "Everyone_Else_(Alle_Anderen)")),
            /* 6 */ movieRefOf("Aladdin", ids = SourceIds.of(WIKI to "Aladdin", DUBDB to "ac9bd1db-cf3a-40f3-8adc-b5f1218a3b29")),
        )

        repo.save(DubbedEntity("Alan", clone(movies[0])))
        repo.save(DubbedEntity("Lilly", clone(movies[5])))
        repo.save(DubbedEntity("Leroi", clone(movies[3])))
        repo.save(DubbedEntity("Bridie", clone(movies[4])))
        repo.save(DubbedEntity("Cherrita", clone(movies[3])))
        repo.save(DubbedEntity("Lanie", clone(movies[4])))
        repo.save(DubbedEntity("Frieda", clone(movies[0])))
        repo.save(DubbedEntity("Merilee", clone(movies[2])))
        repo.save(DubbedEntity("Des", clone(movies[4])))
        repo.save(DubbedEntity("Lane", clone(movies[6])))
        repo.save(DubbedEntity("Melisse", clone(movies[0])))
        repo.save(DubbedEntity("Amii", clone(movies[6])))
        repo.save(DubbedEntity("Marie-ann", clone(movies[3])))
        repo.save(DubbedEntity("Lincoln", clone(movies[4])))
        repo.save(DubbedEntity("Briny", clone(movies[1])))
        repo.save(DubbedEntity("Silva", clone(movies[2])))
        repo.save(DubbedEntity("Moises", clone(movies[6])))
        repo.save(DubbedEntity("Udale", clone(movies[3])))
        repo.save(DubbedEntity("Rania", clone(movies[6])))
        repo.save(DubbedEntity("Wyatt", clone(movies[6])))
        repo.save(DubbedEntity("Inna", clone(movies[3])))

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
        assertEquals(movies[3].ids, result[0].ids)
        assertEquals(movies[4].ids, result[1].ids)
        assertEquals(movies[0].ids, result[2].ids)
    }

    @Test
    fun findMostCommonDubbers() {
        val dubbers = listOf<DubberRef>(
            /* 0 */ DubberRefImpl("Luca Ward", ids = SourceIds.of(WIKI to "Luca_Ward")),
            /* 1 */ DubberRefImpl("Roberto Pedicini", ids = SourceIds.of(WIKI to "Roberto_Pedicini")),
            /* 2 */ DubberRefImpl("Laura Boccanera", ids = SourceIds.of(WIKI to "Laura_Boccanera", DUBDB to "d2a1e3df-9396-4df9-822f-5497a2ad3164")),
            /* 3 */ DubberRefImpl("Francesco Pannofino", ids = SourceIds.of(WIKI to "Francesco_Pannofino")),
            /* 4 */ DubberRefImpl("Emanuela Rossi", ids = SourceIds.of(WIKI to "Emanuela_Rossi")),
            /* 5 */ DubberRefImpl("Massimo Corvo", ids = SourceIds.of(WIKI to "Massimo_Corvo")),
            /* 6 */ DubberRefImpl("Ilaria Stagni", ids = SourceIds.of(WIKI to "Ilaria_Stagni", DUBDB to "ac9bd1db-cf3a-40f3-8adc-b5f1218a3b29")),
        )

        val movie = Movie("Alice in Wonderland", ids = SourceIds.of(WIKI to "Alice_in_Wonderland"))

        repo.save(DubbedEntity("Alan", movie, dubber = clone(dubbers[0])))
        repo.save(DubbedEntity("Lilly", movie, dubber = clone(dubbers[5])))
        repo.save(DubbedEntity("Leroi", movie, dubber = clone(dubbers[3])))
        repo.save(DubbedEntity("Bridie", movie, dubber = clone(dubbers[4])))
        repo.save(DubbedEntity("Cherrita", movie, dubber = clone(dubbers[3])))
        repo.save(DubbedEntity("Lanie", movie, dubber = clone(dubbers[4])))
        repo.save(DubbedEntity("Frieda", movie, dubber = clone(dubbers[0])))
        repo.save(DubbedEntity("Shurwood", movie))
        repo.save(DubbedEntity("Merilee", movie, dubber = clone(dubbers[2])))
        repo.save(DubbedEntity("Des", movie, dubber = clone(dubbers[4])))
        repo.save(DubbedEntity("Finlay", movie))
        repo.save(DubbedEntity("Lane", movie, dubber = clone(dubbers[6])))
        repo.save(DubbedEntity("Melisse", movie, dubber = clone(dubbers[0])))
        repo.save(DubbedEntity("Amii", movie, dubber = clone(dubbers[6])))
        repo.save(DubbedEntity("Marie-ann", movie, dubber = clone(dubbers[3])))
        repo.save(DubbedEntity("Lincoln", movie, dubber = clone(dubbers[4])))
        repo.save(DubbedEntity("Ewan", movie))
        repo.save(DubbedEntity("Briny", movie, dubber = clone(dubbers[1])))
        repo.save(DubbedEntity("Silva", movie, dubber = clone(dubbers[2])))
        repo.save(DubbedEntity("Moises", movie, dubber = clone(dubbers[6])))
        repo.save(DubbedEntity("Trent", movie))
        repo.save(DubbedEntity("Udale", movie, dubber = clone(dubbers[3])))
        repo.save(DubbedEntity("Rania", movie, dubber = clone(dubbers[6])))
        repo.save(DubbedEntity("Wyatt", movie, dubber = clone(dubbers[6])))
        repo.save(DubbedEntity("Inna", movie, dubber = clone(dubbers[3])))

        /*
        dubbers[3]	5
        dubbers[6]	5
        dubbers[4]	4
        dubbers[0]	3
        dubbers[2]	2
        dubbers[1]	1
        dubbers[5]	1
         */

        val result = repo.findMostCommonDubbers(3)

        assertEquals(3, result.size)
        assertEquals(dubbers[3].ids, result[0].ids)
        assertEquals(dubbers[4].ids, result[1].ids)
        assertEquals(dubbers[0].ids, result[2].ids)
    }

    @Test
    fun findMostCommonActors() {
        val actors = listOf<ActorRef>(
            /* 0 */ ActorRefImpl("Leonardo DiCaprio", ids = SourceIds.of(WIKI to "Leonardo_DiCaprio")),
            /* 1 */ ActorRefImpl("Meryl Streep", ids = SourceIds.of(WIKI to "Meryl_Streep")),
            /* 2 */ ActorRefImpl("Tom Hanks", ids = SourceIds.of(WIKI to "Tom_Hanks", DUBDB to "d2a1e3df-9396-4df9-822f-5497a2ad3164")),
            /* 3 */ ActorRefImpl("Scarlett Johansson", ids = SourceIds.of(WIKI to "Scarlett_Johansson")),
            /* 4 */ ActorRefImpl("Denzel Washington", ids = SourceIds.of(WIKI to "Denzel_Washington")),
            /* 5 */ ActorRefImpl("Cate Blanchett", ids = SourceIds.of(WIKI to "Cate_Blanchett")),
            /* 6 */ ActorRefImpl("Morgan Freeman", ids = SourceIds.of(WIKI to "Morgan_Freeman", DUBDB to "ac9bd1db-cf3a-40f3-8adc-b5f1218a3b29")),
        )

        val movie = Movie("Alice in Wonderland", ids = SourceIds.of(WIKI to "Alice_in_Wonderland"))

        repo.save(DubbedEntity("Alan", movie, actor = clone(actors[0])))
        repo.save(DubbedEntity("Lilly", movie, actor = clone(actors[5])))
        repo.save(DubbedEntity("Leroi", movie, actor = clone(actors[3])))
        repo.save(DubbedEntity("Bridie", movie, actor = clone(actors[4])))
        repo.save(DubbedEntity("Cherrita", movie, actor = clone(actors[3])))
        repo.save(DubbedEntity("Lanie", movie, actor = clone(actors[4])))
        repo.save(DubbedEntity("Frieda", movie, actor = clone(actors[0])))
        repo.save(DubbedEntity("Shurwood", movie))
        repo.save(DubbedEntity("Merilee", movie, actor = clone(actors[2])))
        repo.save(DubbedEntity("Des", movie, actor = clone(actors[4])))
        repo.save(DubbedEntity("Finlay", movie))
        repo.save(DubbedEntity("Lane", movie, actor = clone(actors[6])))
        repo.save(DubbedEntity("Melisse", movie, actor = clone(actors[0])))
        repo.save(DubbedEntity("Amii", movie, actor = clone(actors[6])))
        repo.save(DubbedEntity("Marie-ann", movie, actor = clone(actors[3])))
        repo.save(DubbedEntity("Lincoln", movie, actor = clone(actors[4])))
        repo.save(DubbedEntity("Ewan", movie))
        repo.save(DubbedEntity("Briny", movie, actor = clone(actors[1])))
        repo.save(DubbedEntity("Silva", movie, actor = clone(actors[2])))
        repo.save(DubbedEntity("Moises", movie, actor = clone(actors[6])))
        repo.save(DubbedEntity("Trent", movie))
        repo.save(DubbedEntity("Udale", movie, actor = clone(actors[3])))
        repo.save(DubbedEntity("Rania", movie, actor = clone(actors[6])))
        repo.save(DubbedEntity("Wyatt", movie, actor = clone(actors[6])))
        repo.save(DubbedEntity("Inna", movie, actor = clone(actors[3])))

        /*
        actors[3]	5
        actors[6]	5
        actors[4]	4
        actors[0]	3
        actors[2]	2
        actors[1]	1
        actors[5]	1
         */

        val result = repo.findMostCommonActors(3)

        assertEquals(3, result.size)
        assertEquals(actors[3].ids, result[0].ids)
        assertEquals(actors[4].ids, result[1].ids)
        assertEquals(actors[0].ids, result[2].ids)
    }
}

fun clone(movie: MovieRef) = movieRefOf(name = movie.name, type = movie.type, ids = movie.ids.toMutable())
fun clone(dubberRef: DubberRef) = DubberRefImpl(name = dubberRef.name, ids = dubberRef.ids.toMutable())
fun clone(actorRef: ActorRef) = ActorRefImpl(name = actorRef.name, ids = actorRef.ids.toMutable())