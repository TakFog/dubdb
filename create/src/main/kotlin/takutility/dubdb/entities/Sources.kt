package takutility.dubdb.entities

private val wikiMissingPattern: Regex = Regex("""https://it\.wikipedia.org/w/index\.php\?title=([^&]+)&action=edit&redlink=1""")

enum class Source(val domain: String, pathPrefix: String = "") {
    DUBDB("")
    , IMDB("imdb.com", "/Name")
    , MONDO_DOPPIATORI("antoniogenna.net")
    , WIKI("it.wikipedia.org", "/wiki")
    , WIKIMEDIA("wikimedia.org")
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

    fun normalize(id: String): String {
        return when (this) {
            WIKI, WIKI_EN -> id.replace("_", " ")
            else -> id
        }
    }
}

data class SourceId(
    val source: Source,
    val id: String
    ) {

    companion object {
        fun normalized(source: Source, id: String) = SourceId(source, source.normalize(id))

        fun fromUrl(source: Source, url: String): SourceId?
            = source.urlToId(url)?.let { normalized(source, it) }

        fun fromUrlOrNull(url:String): SourceId?
            = Source.entries.firstNotNullOfOrNull { fromUrl(it, url) }

        fun fromUrl(url:String): SourceId = fromUrlOrNull(url) ?: normalized(Source.UNK, url)
    }

    fun notUnk() : SourceId? = if (source == Source.UNK) null else this

    fun toInt() = id.toIntOrNull()
}

interface AnySourceIds: Collection<SourceId> {
    val data: Map<Source, SourceId>
    fun containsAllSrc(sources: Collection<Source>): Boolean
    operator fun contains(source: Source): Boolean
    operator fun get(source: Source): SourceId?
    operator fun plus(other: AnySourceIds): AnySourceIds
    fun getId(source: Source): String?
    fun containsId(sourceId: SourceId): Boolean
    fun allMatch(other: AnySourceIds): Boolean
    fun anyMatch(other: AnySourceIds): Boolean
    fun noMismatch(other: AnySourceIds): Boolean
    fun isCompatible(other: AnySourceIds): Boolean
    fun toImmutable(): ImmutableSourceIds
    /**
     * Returns a mutable copy of this SourceIds
     */
    fun toMutable(): SourceIds
}

open class ImmutableSourceIds(override val data: Map<Source, SourceId>) : AnySourceIds {

    override val size: Int
        get() = data.size

    override fun isEmpty() = data.isEmpty()

    override fun iterator() = data.values.iterator()

    override fun containsAll(elements: Collection<SourceId>) = data.keys.containsAll(elements.map { it.source })

    override fun containsAllSrc(sources: Collection<Source>) = data.keys.containsAll(sources)

    override fun contains(element: SourceId) = data.keys.contains(element.source)

    override operator fun contains(source: Source) = data.keys.contains(source)

    override operator fun get(source: Source) = data[source]
    override fun plus(other: AnySourceIds) = ImmutableSourceIds(data + other.data)

    override fun getId(source: Source) = data[source]?.id

    override fun containsId(sourceId: SourceId): Boolean = sourceId == data[sourceId.source]

    override fun allMatch(other: AnySourceIds): Boolean {
        if (isEmpty() || other.isEmpty()) return false

        return data.keys == other.data.keys
                && data.all { e -> other.data[e.key]?.id == e.value.id }
    }

    override fun anyMatch(other: AnySourceIds): Boolean {
        if (isEmpty() || other.isEmpty()) return false

        return data.any { e -> other.data[e.key]?.id == e.value.id }
    }

    override fun noMismatch(other: AnySourceIds): Boolean {
        if (isEmpty() || other.isEmpty()) return true

        return data.none { e -> e.key in other.data && other.data[e.key]?.id != e.value.id }
    }

    /**
     * Checks whether this collection of source IDs is compatible with another.
     *
     * Compatibility is defined as:
     * - Both this and the `other` collection must be non-empty.
     * - For every key that exists in both collections, the associated IDs must match.
     * - At least one matching key-value pair (by key and ID) must be present.
     * - If any shared key has differing IDs, the collections are considered incompatible.
     *
     * @param other The other `AnySourceIds` to compare against.
     * @return `true` if the collections are non-empty, have at least one matching ID for a shared key,
     *         and no conflicting IDs for any shared keys; `false` otherwise.
     */
    override fun isCompatible(other: AnySourceIds): Boolean {
        if (isEmpty() || other.isEmpty()) return false

        var match = false
        data.forEach { e ->
            if (e.key in other.data) {
                if (e.value.id == other.data[e.key]?.id)
                    match = true
                else
                    return false
            }
        }
        return match
    }

    override fun toImmutable(): ImmutableSourceIds = this

    /**
     * Returns a mutable copy of this SourceIds
     */
    override fun toMutable(): SourceIds = SourceIds(data.toMutableMap())

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ImmutableSourceIds) return false

        if (data != other.data) return false

        return true
    }

    override fun hashCode(): Int {
        return data.hashCode()
    }

    override fun toString(): String {
        return data.entries.joinToString(", ", "{", "}") { "${it.key}: ${it.value.id}" }
    }
}

class SourceIds(override val data: MutableMap<Source, SourceId>) : ImmutableSourceIds(data) {

    companion object {
        /**
         * Immutable read-only source ids
         */
        val empty = ImmutableSourceIds(mapOf())

        fun mutable() = SourceIds()

        fun of(vararg values: Pair<Source, String?>): SourceIds {
            return SourceIds(mutableMapOf(*values
                .mapNotNull {p ->
                    p.second?.let { p.first to SourceId.normalized(p.first, it) }
                }
                .toTypedArray()))
        }

        fun of(vararg values: SourceId): SourceIds = SourceIds().apply { values.forEach(::add) }

        fun of(values: Collection<SourceId>): SourceIds = SourceIds().apply { values.forEach(::add) }

        fun join(vararg ids: AnySourceIds): SourceIds = SourceIds().apply { ids.forEach(::plusAssign) }
        fun join(ids: Iterable<AnySourceIds>): SourceIds = SourceIds().apply { ids.forEach(::plusAssign) }

    }

    constructor() : this(mutableMapOf())

    override fun plus(other: AnySourceIds) = SourceIds(data.toMutableMap().apply { this += other.data})

    operator fun plusAssign(element: SourceId?) {
        element?.let { data[it.source] = it }
    }

    operator fun plusAssign(other: AnySourceIds) {
        data += other.data
    }

    fun add(element: SourceId?) {
        element?.let { data[it.source] = it }
    }

    fun remove(source: Source) = data.remove(source)

    fun clear() = data.clear()

    operator fun set(source: Source, id: Any?) {
        if (id == null)
            data.remove(source)
        else
            data[source] = SourceId.normalized(source, id.toString())
    }

    override fun toImmutable() = ImmutableSourceIds(data.toMap())

}
