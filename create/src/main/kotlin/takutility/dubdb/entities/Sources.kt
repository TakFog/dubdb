package takutility.dubdb.entities

private val wikiMissingPattern: Regex = Regex("""https://it\.wikipedia.org/w/index\.php\?title=([^&]+)&action=edit&redlink=1""")

enum class Source(val domain: String, pathPrefix: String = "") {
    DUBDB("")
    , IMDB("imdb.com", "/Name")
    , MONDO_DOPPIATORI("antoniogenna.net")
    , WIKI("it.wikipedia.org", "/wiki")
    , WIKI_MISSING("it.wikipedia.org", "/w/index.php")
    , WIKI_EN("en.wikipedia.org", "/wiki")
    , WIKIDATA("wikidata.org")
    , TRAKT("trakt.tv")
    , UNK("")
    ;

    private val urlPrefix = domain + pathPrefix

    fun urlToId(url: String): String? {
        if (!url.contains(this.urlPrefix)) return null

        when (this) {
            DUBDB, UNK -> return null
            WIKIDATA -> {
                val start = url.lastIndexOf("Q")
                return if (start > 0) url.substring(start)
                    else null
            }
            WIKI_MISSING -> {
                wikiMissingPattern.find(url)
                    ?.let { return it.groups[1]?.value }
                    ?: return null
            }
            else -> {}
        }

        var start = url.indexOf(urlPrefix)
        if (start < 0) return null
        start += urlPrefix.length + 1
        return url.substring(start)
    }
}

data class SourceId(
    val source: Source,
    val id: String
    ) {

    companion object {
        fun fromUrl(source: Source, url: String): SourceId?
            = source.urlToId(url)?.let { SourceId(source, it) }

        fun fromUrlOrNull(url:String): SourceId?
            = Source.values().firstNotNullOfOrNull { fromUrl(it, url) }

        fun fromUrl(url:String): SourceId = fromUrlOrNull(url) ?: SourceId(Source.UNK, url)
    }

    fun notUnk() : SourceId? = if (source == Source.UNK) null else this
}

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

class SourceIds(override val data: MutableMap<Source, SourceId>) : ImmutableSourceIds(data) {

    companion object {
        /**
         * Immutable read-only source ids
         */
        val empty = ImmutableSourceIds(mapOf())

        fun mutable() = SourceIds()

        fun of(vararg values: Pair<Source, String>): SourceIds {
            return SourceIds(mutableMapOf(*values
                .map { it.first to SourceId(it.first, it.second) }.toTypedArray()))
        }
    }

    constructor() : this(mutableMapOf())

    operator fun plusAssign(element: SourceId?) {
        element?.let { data[it.source] = it }
    }

    fun add(element: SourceId?) {
        element?.let { data[it.source] = it }
    }

    operator fun set(source: Source, id: Any?) {
        if (id == null)
            data.remove(source)
        else
            data[source] = SourceId(source, id.toString())
    }

    override fun toImmutable() = ImmutableSourceIds(data.toMap())

    override fun toMutable() = this
}
