package takutility.dubdb.entities


enum class Source(val domain: String, pathPrefix: String = "") {
    DUBDB("")
    , IMDB("imdb.com", "/Name")
    , MONDO_DOPPIATORI("antoniogenna.net")
    , WIKI("it.wikipedia.org", "/wiki")
    , WIKI_EN("en.wikipedia.org", "/wiki")
    , WIKIDATA("wikidata.org")
    , TRAKT("trakt.tv")
    ;

    val urlPrefix = domain + pathPrefix

    fun urlToId(url: String): String? {
        if (!url.contains(this.domain)) return null

        if (this == WIKIDATA) {
            val start = url.lastIndexOf("Q")
            return if (start > 0) url.substring(start)
                else null
        }

        var start = url.indexOf(urlPrefix)
        if (start < 0) return null
        start += urlPrefix.length + 1
        return url.substring(start)
    }
}

class SourceId(
    val source: Source,
    val id: String
    ) {

    companion object {
        fun fromUrl(source: Source, url: String): SourceId? {
            val id = source.urlToId(url) ?: return null
            return SourceId(source, id)
        }
    }
}

class DataSource(sourceId: SourceId, raw: String)

open class ImmutableSourceIds(open val data: Map<Source, SourceId>) : Collection<SourceId> {

    override val size: Int
        get() = data.size

    override fun isEmpty() = data.isEmpty()

    override fun iterator() = data.values.iterator()

    override fun containsAll(elements: Collection<SourceId>) = data.keys.containsAll(elements.map { it.source })

    fun containsAllSrc(sources: Collection<Source>) = data.keys.containsAll(sources)

    override fun contains(element: SourceId) = data.keys.contains(element.source)

    operator fun contains(source: Source) = data.keys.contains(source)

    operator fun get(source: Source) = data[source]

    fun getId(source: Source) = data[source]?.id

    fun allMatch(other: ImmutableSourceIds): Boolean {
        if (isEmpty() || other.isEmpty()) return false

        return data.keys == other.data.keys
                && data.all { e -> other.data[e.key]?.id == e.value.id }
    }

    fun anyMatch(other: ImmutableSourceIds): Boolean {
        if (isEmpty() || other.isEmpty()) return false

        return data.any { e -> other.data[e.key]?.id == e.value.id }
    }

    open fun toImmutable(): ImmutableSourceIds = this

    open fun toMutable(): SourceIds = SourceIds(data.toMutableMap())

}

class SourceIds(data: MutableMap<Source, SourceId>) : ImmutableSourceIds(data) {

    companion object {
        /**
         * Immutable read-only source ids
         */
        val empty = ImmutableSourceIds(mapOf())

        fun mutable() = SourceIds()
    }

    constructor() : this(mutableMapOf())

    override val data: MutableMap<Source, SourceId> = mutableMapOf()

    operator fun plusAssign(element: SourceId) {
        data[element.source] = element
    }

    fun add(element: SourceId) {
        data[element.source] = element
    }

    operator fun set(source: Source, id: Any) {
        data[source] = SourceId(source, id.toString())
    }

    override fun toImmutable() = ImmutableSourceIds(data.toMap())

    override fun toMutable() = this
}
