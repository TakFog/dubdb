package takutility.dubdb.wiki

import takutility.dubdb.service.wikiapi.WikiApi
import java.time.Instant
import java.util.*
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class WikiApiPage(
    private val api: WikiApi,
    title: String,
    private val changeListener: ((WikiApiPage) -> Unit)? = null
) : WikiPage {
    private var isParsed = false
    private var _title = title
    private var exists: Boolean? by ParsedValue()

    override val title: String
        get() {
            loadParse()
            return _title
        }
    override var id: Long? by ParsedValue()
    override var mainSection: WikiSection? by ParsedValue()
    override var sections: Map<String, WikiSection>? by ParsedValue()
    override var langLink: Map<String, String>? by ParsedValue()
    override var sort: String? by ParsedValue()
    override var image: String? by ParsedValue()
    override var wikidata: String? by ParsedValue()
    override var revisionId: Long? by ParsedValue()
    override var lastRead: Instant? by ParsedValue()

    constructor(api: WikiApi,
                simple: SimpleWikiPage,
                changeListener: ((WikiApiPage) -> Unit)? = null)
    : this(api, simple.title, changeListener) {
        isParsed = (simple.exists ?: simple.lastRead) != null
        if (isParsed)
            exists  = simple.exists ?: (simple.lastRead != null)
        id = simple.id
        langLink = simple.langLink
        sort = simple.sort
        image = simple.image
        wikidata = simple.wikidata
        revisionId = simple.revisionId
        lastRead = simple.lastRead
        mainSection = simple.mainSection?.let { WikiApiSection(it) }
        sections = simple.sections?.let { simSec ->
            val root = LinkedHashMap<String, WikiSection>()
            simSec.forEach { root[it.title] = WikiApiSection(it) }
            root
        }
    }

    override fun exists(): Boolean = exists ?: false

    private fun loadParse() {
        if (isParsed) return
        isParsed = true
        // Fetch parse data including sections, properties, and language links
        val oldRevId = revisionId
        api.parse(title = _title, revid = oldRevId, prop = "tocdata|properties|langlinks|revid")?.parse?.let { parse ->
            exists = true
            _title = parse.title
            id = parse.pageid
            revisionId = parse.revid
            langLink = parse.langlinks?.associate { (it.lang ?: "") to (it.url ?: "") }
            sort = parse.properties?.defaultsort
            image = parse.properties?.pageImageFree
            wikidata = parse.properties?.wikibaseItem

            mainSection = WikiApiSection("", index = 0, offset = 0)

            // Reconstruct the section tree from the flat list of sections
            sections = parse.tocdata?.sections?.let { rawSections ->
                val rootSections = LinkedHashMap<String, WikiSection>()
                val stack = Stack<WikiApiSection>()

                rawSections.forEach { s ->
                    val section = WikiApiSection(
                        title = s.line ?: "",
                        index = s.index?.toIntOrNull() ?: -1,
                        offset = s.codepointOffset?.toInt() ?: 0,
                    )

                    val level = (s.tocLevel ?: 1).toInt()

                    // Pop sections from the stack that are deeper or at the same level
                    // This creates the hierarchy by attaching finished sections to their parents
                    while (stack.isNotEmpty() && stack.size >= level) {
                        val child = stack.pop()
                        if (stack.isNotEmpty()) {
                            // Add to parent
                            stack.peek().addSection(child)
                        } else {
                            // No parent means it's a root section
                            rootSections[child.title] = child
                        }
                    }
                    stack.push(section)
                }

                // Process any remaining sections on the stack
                while (stack.isNotEmpty()) {
                     val child = stack.pop()
                     if (stack.isNotEmpty()) {
                        stack.peek().addSection(child)
                     } else {
                        rootSections[child.title] = child
                     }
                }

                rootSections
            }

        } ?: run {
            exists = false
        }
        lastRead = Instant.now()
        changeListener?.invoke(this)
    }

    private inner class ParsedValue<T> : ReadWriteProperty<Any?, T?> {
        private var _value: T? = null

        override fun getValue(thisRef: Any?, property: KProperty<*>): T? {
            loadParse()
            return _value
        }

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: T?) {
            _value = value
        }
    }

    fun toSimple(): SimpleWikiPage {
        if (!isParsed)
            return SimpleWikiPage(_title)
        return SimpleWikiPage(
            title = title,
            exists = exists?.takeIf { id == null },
            id = id,
            mainSection = mainSection?.let { (it as WikiApiSection).toSimple() },
            sections = sections?.values?.map { (it as WikiApiSection).toSimple() },
            langLink = langLink,
            sort = sort,
            image = image,
            wikidata = wikidata,
            revisionId = revisionId,
            lastRead = lastRead,
        )
    }

    private inner class WikiApiSection(
        override val title: String,
        val index: Int,
        override val offset: Int,
    ) : WikiSection {
        override val subsections: MutableMap<String, WikiApiSection> = LinkedHashMap()
        private var _content: String? = null
        override val content: String
            get() = _content ?: loadContent()

        constructor(simple: SimpleWikiApiSection) : this(
            title = simple.title,
            index = simple.index,
            offset = simple.offset,
        ) {
            _content = simple.content
            simple.subsections?.forEach { subsimple -> addSection(WikiApiSection(subsimple)) }
        }

        fun addSection(section: WikiApiSection) {
            subsections[section.title] = section
        }

        private fun loadContent(): String {
            val wikitext = api.parse(revid = revisionId, section = index, prop = "wikitext")?.parse
                ?.wikitext ?: ""
            _content = wikitext
            if (subsections.isNotEmpty()) {
                val starts = subsections.values.map { it.offset }.sorted()
                val startToText = starts.zipWithNext()
                    .associate { se -> se.first to wikitext.substring(se.first - offset, se.second - offset) }
                subsections.values.forEach { ss ->
                    ss._content = startToText[ss.offset] ?: wikitext.substring(ss.offset - offset)
                }
            }
            changeListener?.invoke(this@WikiApiPage)
            return wikitext
        }

        fun toSimple(): SimpleWikiApiSection {
            return SimpleWikiApiSection(
                title = title,
                index = index,
                offset = offset,
                content = _content,
                subsections = subsections.takeIf { it.isNotEmpty() }?.values?.map { it.toSimple() }
            )
        }
    }

}

data class SimpleWikiPage(
    val title: String,
    val exists: Boolean? = null,
    val id: Long? = null,
    val mainSection: SimpleWikiApiSection? = null,
    val sections: List<SimpleWikiApiSection>? = null,
    val langLink: Map<String, String>? = null,
    val sort: String? = null,
    val image: String? = null,
    val wikidata: String? = null,
    val revisionId: Long? = null,
    val lastRead: Instant? = null
)

data class SimpleWikiApiSection(
    val title: String,
    val index: Int,
    val subsections: List<SimpleWikiApiSection>?,
    val offset: Int,
    val content: String?
)