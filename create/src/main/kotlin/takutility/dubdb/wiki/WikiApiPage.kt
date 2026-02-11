package takutility.dubdb.wiki

import takutility.dubdb.service.wikiapi.WikiApi
import takutility.dubdb.service.wikiapi.queryValue
import java.time.Instant
import java.util.*
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

fun WikiApi.page(title: String) = WikiApiPage(this, title)

class WikiApiPage(private val api: WikiApi, override val title: String) : WikiPage {
    private var isParsed = false

    override var id: Long? by ParsedValue()
    override var mainSection: WikiSection? by ParsedValue()
    override var sections: Map<String, WikiSection>? by ParsedValue()
    override var langLink: Map<String, String>? by ParsedValue()
    override var sort: String? by ParsedValue()
    override var image: String? by ParsedValue()
    override var wikidata: String? by ParsedValue()
    override var revisionId: Long? by ParsedValue()
    override var lastRead: Instant? by ParsedValue()

    private var exists: Boolean? = null


    override fun exists(): Boolean {
        loadParse()
        return exists ?: false
    }

    private fun loadParse() {
        if (isParsed) return
        isParsed = true
        // Fetch parse data including sections, properties, and language links
        api.parse(title, prop = "sections|properties|langlinks")?.queryValue()?.let { parse ->
            exists = true
            id = parse.pageid
            revisionId = parse.revid
            langLink = parse.langlinks?.associate { (it.lang ?: "") to (it.url ?: "") }
            sort = parse.properties?.defaultsort
            image = parse.properties?.pageImageFree
            wikidata = parse.properties?.wikibaseItem

            mainSection = WikiApiSection("", index = 0, offset = 0, subsections = mutableMapOf())

            // Reconstruct the section tree from the flat list of sections
            sections = parse.tocdata?.sections?.let { rawSections ->
                val rootSections = LinkedHashMap<String, WikiSection>()
                val stack = Stack<WikiApiSection>()

                rawSections.forEach { s ->
                    val section = WikiApiSection(
                        title = s.line ?: "",
                        index = s.index?.toIntOrNull() ?: -1,
                        offset = s.codepointOffset?.toInt() ?: 0,
                        subsections = LinkedHashMap()
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

    private inner class WikiApiSection(
        override val title: String,
        val index: Int,
        override val offset: Int,
        override val subsections: MutableMap<String, WikiApiSection> = LinkedHashMap(),
        var parent: WikiApiSection? = null,
    ) : WikiSection {
        private var _content: String? = null
        override val content: String
            get() = _content ?: loadContent()

        fun addSection(section: WikiApiSection) {
            section.parent = this
            subsections[section.title] = section
        }

        private fun loadContent(): String {
            val wikitext = api.parse(revid = revisionId, section = index, prop = "wikitext")?.queryValue()
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
            return wikitext
        }
    }
}