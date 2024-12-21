package takutility.dubdb.db

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource
import takutility.dubdb.entities.*
import java.util.*
import java.util.stream.Stream


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal abstract class RepositoryTest<E: Entity, R: EntityRepository<E>> {
    lateinit var repo: R

    @BeforeEach
    open fun setup() {
        repo = newRepo()
    }

    abstract fun newRepo(): R

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
    fun findMissing() {
        assertNull(repo.findById("123456"))
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

    @ParameterizedTest
    @EnumSource(Source::class, names = ["DUBDB"], mode = EnumSource.Mode.EXCLUDE)
    fun findBySource(source: Source) {
        val values = initRepo(source, if (source == Source.WIKI) Source.IMDB else Source.WIKI)

        values.forEach { (k, entities) ->
            val found = repo.findBySource(SourceId(source, k)).sortedBy { it.name }
            assertEquals(entities, found, k)
        }
    }

    @ParameterizedTest
    @MethodSource("sourceGenerator")
    fun findBySources(source: Source, otherSources: Array<Source>) {
        val values = initRepo(source, *otherSources)
        val ids = SourceIds.mutable()
        values.values.flatten().forEach { ids += it.ids }

        values.forEach { (k, entities) ->
            ids[source] = k
            val found = repo.findBySources(ids).sortedBy { it.name }
            assertEquals(entities, found, k)
        }
    }

    @Test
    fun findBySources_duplicated() {
        val entity1 = newEntity(name = "e1", ids = SourceIds.of(Source.WIKI to "wiki", Source.WIKI_EN to "wikien"))
        val entity2 = newEntity(name = "e2", ids = SourceIds.of(Source.WIKI to "wiki", Source.WIKI_EN to "wikien2"))
        val entity3 = newEntity(name = "e3", ids = SourceIds.of(Source.WIKI to "wiki2", Source.WIKI_EN to "wikien"))
        repo.save(entity1)
        repo.save(entity2)
        repo.save(entity3)

        val found = repo.findBySources(SourceIds.of(Source.WIKI to "wiki", Source.WIKI_EN to "wikien"))

        assertEquals(listOf(entity1), found)

    }

    companion object {
        @JvmStatic
        fun sourceGenerator() = Stream.of(
            Arguments.of(Source.IMDB, arrayOf(Source.WIKI, Source.WIKI_EN, Source.WIKIDATA)),
            Arguments.of(Source.WIKIDATA, arrayOf(Source.WIKI, Source.WIKI_EN)),
            Arguments.of(Source.WIKI, arrayOf(Source.MONDO_DOPPIATORI)),
        )
    }

    private fun initRepo(source: Source, vararg otherSource: Source): MutableMap<String, List<Entity>> {
        val values = mutableMapOf<String, List<Entity>>()
        for (i in 1..3) {
            val key = "value $i"
            val list = mutableListOf<Entity>()
            for (j in 1..i) {
                val e = newEntity(
                    "name $i $j",
                    ids = SourceIds.of(SourceId(source, key),
                        *otherSource.map { SourceId(it, "value $j") }.toTypedArray()),
                )
                list.add(e)
                repo.save(e)
            }
            values[key] = list.sortedBy { it.name }
        }
        return values
    }
}
