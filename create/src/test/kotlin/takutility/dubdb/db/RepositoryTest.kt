package takutility.dubdb.db

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import takutility.dubdb.entities.*


internal abstract class RepositoryTest<E: Entity> {
    lateinit var repo: EntityRepository<E>

    @BeforeEach
    open fun setup() {
        repo = newRepo()
    }

    abstract fun newRepo(): EntityRepository<E>

    abstract fun newEntity(
        name: String,
        ids: SourceIds = SourceIds(),
        parsed: Boolean = false,
        sources: MutableList<RawData> = mutableListOf(),
    ): E

    @Test
    fun insert() {
        save(newEntity(
            "name",
            ids = SourceIds.of(Source.WIKI to "Wiki_Name"),
            parsed = true,
            sources = mutableListOf(RawData(SourceId(Source.WIKI, "Wiki_src"), DataSource.MOVIE_DUB, "raw text"))
        ))
    }

    open fun save(input: E): E {
        val name = input.name
        val ids = input.ids.toImmutable()
        val parsed = input.parsed
        val sources = input.sources.toList()
        val insert = input.id == null

        val saved = repo.save(input)

        assertEquals(name, saved.name)
        assertEquals(parsed, saved.parsed)
        assertEquals(sources, saved.sources)
        if (insert) {
            assertNotNull(saved.id)
            ids.forEach { assertTrue(it in saved.ids) }
            assertEquals(ids.size + 1, saved.ids.size)
        } else {
            assertEquals(ids, saved.ids)
        }

        return saved
    }

    @Test
    fun update() {
        val saved = save(
            newEntity(
                "name",
                ids = SourceIds.of(Source.WIKI to "Wiki_Name"),
                parsed = false,
                sources = mutableListOf(RawData(SourceId(Source.WIKI_EN, "En_wiki"), DataSource.MOVIE_DUB, "raw text"))
            )
        )
        saved.ids[Source.TRAKT] = 123456
        saved.parsed = true
        saved.sources.add(RawData(SourceId(Source.WIKI, "Wiki_dub"), DataSource.DUBBER, "raw dub text"))

        save(saved)
    }

    @Test
    fun findById() {
        val newEntity = newEntity(
            "name",
            ids = SourceIds.of(Source.WIKI to "Wiki_Name"),
            parsed = false,
            sources = mutableListOf(RawData(SourceId(Source.WIKI_EN, "En_wiki"), DataSource.MOVIE_DUB, "raw text"))
        )
        val saved = repo.save(newEntity)

        val found = repo.findById(saved.id!!)

        assertEquals(saved, found)
    }

    @Test
    fun updateAndFind() {
        val saved = save(
            newEntity(
                "name",
                ids = SourceIds.of(Source.WIKI to "Wiki_Name"),
                parsed = false,
                sources = mutableListOf(RawData(SourceId(Source.WIKI_EN, "En_wiki"), DataSource.MOVIE_DUB, "raw text"))
            )
        )

        val findById = repo.findById(saved.id!!)
        saved.ids[Source.TRAKT] = 123456
        saved.parsed = true
        saved.sources.add(RawData(SourceId(Source.WIKI, "Wiki_dub"), DataSource.DUBBER, "raw dub text"))

        val updated = save(saved)

        val findAfterUpdate = repo.findById(saved.id!!)

        assertEquals(findAfterUpdate, updated)
    }
}
