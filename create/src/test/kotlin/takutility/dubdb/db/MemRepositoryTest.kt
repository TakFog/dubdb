package takutility.dubdb.db

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import takutility.dubdb.assertRefEquals
import takutility.dubdb.entities.*

internal class MemMovieRepositoryTest: RepositoryTest<Movie>() {
    override fun newRepo(): EntityRepository<Movie> = MemMovieRepository()

    override fun newEntity(name: String, ids: SourceIds, parsed: Boolean, sources: MutableList<RawData>) = Movie(
        name = name, ids = ids, parsed = parsed, sources = sources
    )

    @Test
    fun insertFull() {
        save(Movie(
            "name",
            type = MovieType.MOVIE,
            year = 2014,
            ids = SourceIds.of(Source.WIKI to "Wiki_Name"),
            parsed = true,
            sources = mutableListOf(RawData(SourceId(Source.WIKI, "Wiki_src"), DataSource.MOVIE_DUB, "raw text"))
        ))
    }

    @Test
    fun updateFull() {
        val saved = save(Movie(
            "name",
            ids = SourceIds.of(Source.WIKI to "Wiki_Name"),
            parsed = false,
            sources = mutableListOf(RawData(SourceId(Source.WIKI_EN, "En_wiki"), DataSource.MOVIE_DUB, "raw text"))
        ))
        saved.type = MovieType.SERIES
        saved.year = 2019
        saved.ids[Source.TRAKT] = 123456
        saved.parsed = true
        saved.sources.add(RawData(SourceId(Source.WIKI, "Wiki_dub"), DataSource.DUBBER, "raw dub text"))

        save(saved)
    }

    override fun save(input: Movie): Movie {
        val type = input.type
        val year = input.year

        val saved = super.save(input)

        assertEquals(type, saved.type)
        assertEquals(year, saved.year)

        return saved
    }
}

internal class MemActorRepositoryTest: RepositoryTest<Actor>() {
    override fun newRepo(): EntityRepository<Actor> = MemActorRepository()

    override fun newEntity(name: String, ids: SourceIds, parsed: Boolean, sources: MutableList<RawData>) = Actor(
        name = name, ids = ids, parsed = parsed, sources = sources
    )
}

internal class MemDubberRepositoryTest: RepositoryTest<Dubber>() {
    override fun newRepo(): EntityRepository<Dubber> = MemDubberRepository()

    override fun newEntity(name: String, ids: SourceIds, parsed: Boolean, sources: MutableList<RawData>) = Dubber(
        name = name, ids = ids, parsed = parsed, sources = sources
    )
}

internal class MemDubbedEntityRepositoryTest: RepositoryTest<DubbedEntity>() {
    override fun newRepo(): EntityRepository<DubbedEntity> = MemDubbedEntityRepository()

    override fun newEntity(name: String, ids: SourceIds, parsed: Boolean, sources: MutableList<RawData>) = DubbedEntity(
        name = name, ids = ids, parsed = parsed, sources = sources,
        movie = movieRefOf()
    )

    @Test
    fun insertFull() {
        save(DubbedEntity(
            "name",
            movie = movieRefOf("movie", MovieType.SERIES, SourceIds.of(Source.TRAKT to "12345")),
            dubber = DubberRefImpl("dubber", SourceIds.of(Source.MONDO_DOPPIATORI to "voci/dub")),
            actor = ActorRefImpl("actor", SourceIds.of(Source.WIKI_EN to "Wiki_actor")),
            ids = SourceIds.of(Source.WIKI to "Wiki_Name"),
            parsed = true,
            sources = mutableListOf(RawData(SourceId(Source.WIKI, "Wiki_src"), DataSource.MOVIE_DUB, "raw text"))
        ))
    }

    @Test
    fun updateFull() {
        val saved = save(DubbedEntity(
            "name",
            movie = movieRefOf("movie", MovieType.SERIES, SourceIds.of(Source.TRAKT to "12345")),
            ids = SourceIds.of(Source.WIKI to "Wiki_Name"),
            parsed = false,
            sources = mutableListOf(RawData(SourceId(Source.WIKI_EN, "En_wiki"), DataSource.MOVIE_DUB, "raw text"))
        ))
        saved.dubber = DubberRefImpl("dubber", SourceIds.of(Source.MONDO_DOPPIATORI to "voci/dub"))
        saved.actor = ActorRefImpl("actor", SourceIds.of(Source.WIKI_EN to "Wiki_actor"))
        saved.ids[Source.TRAKT] = 123456
        saved.parsed = true
        saved.sources.add(RawData(SourceId(Source.WIKI, "Wiki_dub"), DataSource.DUBBER, "raw dub text"))

        save(saved)
    }

    override fun save(input: DubbedEntity): DubbedEntity {
        val movie = input.movie.apply { movieRefOf(name, type, ids.toMutable()) }
        val dubber = input.dubber?.apply { DubberRefImpl(name, ids.toMutable()) }
        val actor = input.actor?.apply { ActorRefImpl(name, ids.toMutable()) }

        val saved = super.save(input)

        assertRefEquals(movie, saved.movie)
        assertRefEquals(dubber, saved.dubber)
        assertRefEquals(actor, saved.actor)

        return saved
    }
}