package takutility.wikitext

// ════════════════════════════════════════════════════════════════════════════
//  AST Node Hierarchy
// ════════════════════════════════════════════════════════════════════════════

/**
 * Base class for every parsed wikitext element.
 *
 * Every node guarantees:
 *  • [rawText]   – the original wikitext fragment that produced this node.
 *  • [plainText] – a best-effort human-readable rendering (markup stripped).
 *  • [toString]  – always returns [rawText].
 *  • [children]  – direct child nodes (default: empty).
 */
sealed class WikiNode {
    abstract val rawText: String
    abstract val plainText: String
    open val children: List<WikiNode> get() = emptyList()

    /** Always reconstructs the original source. */
    override fun toString(): String = rawText

    /** Depth-first sequence of this node and all descendants. */
    fun walk(): Sequence<WikiNode> = sequence {
        yield(this@WikiNode)
        for (child in children) yieldAll(child.walk())
    }
}

data class WikiNodeSplit<T>(
    val pre: T?,
    val post: T?
) {
    fun isEmpty() = pre == null && post == null

    val size: Int
        get() {
        if (pre == null) {
            if (post == null)
                return 0
            return 1
        }
        if (post == null)
            return 1
        return 2
    }

    fun <E> map(mapper: (T) -> E): WikiNodeSplit<E> {
        val mpre = pre?.let { mapper(it) }
        val mpost = post?.let { mapper(it) }
        return WikiNodeSplit(mpre, mpost)
    }
}

interface SplittableWikiNode<T : WikiNode> {
    val plainText: String

    /**
     * Start to index (excluded), index to end
     */
    fun splitPlain(index: Int): WikiNodeSplit<T>
    fun splitPlain(string: String): WikiNodeSplit<T> =
        splitPlain(plainText.indexOf(string))
}


private fun <T: WikiNode> splitChildrenPlain(children: List<T>, index: Int,
                                             splitNode: (T, Int) -> WikiNodeSplit<T>)
        : WikiNodeSplit<List<T>> {
    if (children.isEmpty()) return WikiNodeSplit(null, null)
    if (index <= 0) return WikiNodeSplit(null, children)
    var remaining = index
    val pre = mutableListOf<T>()
    val post = mutableListOf<T>()
    children.forEach { child ->
        val plain = child.plainText
        if (remaining <= 0) {
            post.add(child)
        } else if (plain.length <= remaining) {
            pre.add(child)
            remaining -= plain.length
        } else if (child is SplittableWikiNode<*>) {
            val childSplit = splitNode(child, remaining)
            childSplit.pre?.let { pre.add(it) }
            childSplit.post?.let { post.add(it) }
            remaining = 0
        } else {
            remaining -= plain.length
            post.add(child)
        }
    }
    return WikiNodeSplit(pre, post)
}


// ─── 1. Document ─────────────────────────────────────────────────────────────

/**
 * Root node representing a complete wikitext document.
 * Its direct children are the top-level nodes produced by the parser.
 */
class WikiDocument(override val children: List<WikiNode>) : WikiNode(), SplittableWikiNode<WikiDocument> {
    override val rawText: String   get() = children.joinToString("") { it.rawText }
    override val plainText: String get() = children.joinToString("") { it.plainText }

    /** All templates found anywhere in the document (including nested ones). */
    fun allTemplates(): List<WikiTemplate> =
        walk().filterIsInstance<WikiTemplate>().toList()

    /** Top-level templates only (direct children of this document). */
    fun topLevelTemplates(): List<WikiTemplate> =
        children.filterIsInstance<WikiTemplate>()

    override fun toString(): String = rawText

    @Suppress("unchecked_cast")
    override fun splitPlain(index: Int) = splitChildrenPlain(children, index,
            { c, i -> (c as SplittableWikiNode<WikiNode>).splitPlain(i) })
        .map { c -> if (c == children) this else WikiDocument(c) }

}

// ─── 2. Plain Text ───────────────────────────────────────────────────────────

/**
 * A run of literal text with no wikitext markup.
 */
class TextNode(val text: String) : WikiNode(), SplittableWikiNode<TextNode> {
    override val rawText: String get() = text
    override val plainText: String get() = text
    override fun toString(): String = text

    override fun splitPlain(index: Int): WikiNodeSplit<TextNode> {
        if (index <= 0) return WikiNodeSplit(null, this)
        if (index >= text.length) return WikiNodeSplit(this, null)
        return WikiNodeSplit(
            TextNode(text.substring(0, index)),
            TextNode(text.substring(index))
        )
    }
}

// ─── 3. Template ─────────────────────────────────────────────────────────────

/**
 * A wikitext template: `{{Name|arg1|key=value|…}}`.
 *
 * @param name       The template name (trimmed, as a string).
 * @param arguments  Ordered list of [TemplateArgument] nodes.
 * @param rawText    The verbatim source including `{{` and `}}`.
 */
class WikiTemplate(
    val name: String,
    val arguments: List<TemplateArgument>,
    override val rawText: String,
) : WikiNode() {

    override val children: List<WikiNode> get() = arguments

    /**
     * Plain text for a template cannot be reliably rendered without transclusion,
     * so we emit a bracketed placeholder showing the name.
     */
    override val plainText: String get() = "[${name}]"

    // ── Argument accessors ────────────────────────────────────────────────────

    /** Find a named argument by name (trimmed, case-sensitive). */
    fun named(name: String): TemplateArgument? =
        arguments.firstOrNull { it.name?.plainText?.trim() == name.trim() }

    /** Find a positional argument by 1-based index. */
    fun positional(index: Int): TemplateArgument? =
        arguments.firstOrNull { it.index == index }

    /** All named arguments as a map of (trimmed name → argument). */
    val namedArgs: Map<String, TemplateArgument>
        get() = arguments
            .filter { it.name != null }
            .associateBy { it.name!!.plainText.trim() }

    /** All positional arguments, sorted by index. */
    val positionalArgs: List<TemplateArgument>
        get() = arguments.filter { it.index != null }.sortedBy { it.index }

    override fun toString(): String = rawText

    override fun equals(other: Any?): Boolean =
        other is WikiTemplate && rawText == other.rawText
    override fun hashCode(): Int = rawText.hashCode()
}

/**
 * A single argument inside a `WikiTemplate`.
 *
 * For **named** arguments (`|key=value`):
 *   - [name]  is a parsed list of nodes representing the key part.
 *   - [index] is `null`.
 *
 * For **positional** arguments (`|value`):
 *   - [name]  is `null`.
 *   - [index] is a 1-based integer.
 *
 * [value] is always a parsed list of nodes (may contain nested templates,
 * links, etc.).
 */
class TemplateArgument(
    val index: Int?,
    val name: WikiDocument?,        // parsed key (named args)
    val value: WikiDocument,        // parsed value
    override val rawText: String,   // verbatim `key=value` or just `value`
) : WikiNode() {

    override val children: List<WikiNode>
        get() = buildList {
            name?.let { addAll(it.children) }
            addAll(value.children)
        }

    override val plainText: String get() = value.plainText

    val isNamed: Boolean     get() = name != null
    val isPositional: Boolean get() = index != null

    override fun toString(): String = rawText
}

// ─── 4. Wikilink ─────────────────────────────────────────────────────────────

/**
 * An internal wiki link: `[[Target]]` or `[[Target|Display text]]`.
 *
 * @param target       The link target (page name / anchor), as a raw string.
 * @param displayNodes Parsed display content nodes (empty list when absent,
 *                     meaning the target itself is displayed).
 * @param rawText      Verbatim source including `[[` and `]]`.
 */
class WikiLink(
    val target: String,
    val displayNodes: WikiDocument,
    override val rawText: String,
) : WikiNode() {

    override val children: List<WikiNode> get() = displayNodes.children

    /**
     * Plain text: use display content if present, otherwise the target
     * (stripping any namespace prefix for readability, e.g. "File:Foo" → "Foo").
     */
    override val plainText: String
        get() = if (displayNodes.children.isNotEmpty()) {
            displayNodes.plainText
        } else {
            // Strip namespace prefix and anchor
            target.substringAfterLast(':').substringBefore('#').trim()
        }

    val hasDisplay: Boolean get() = displayNodes.children.isNotEmpty()

    override fun toString(): String = rawText
}

// ─── 5. External Link ────────────────────────────────────────────────────────

/**
 * An external link: `[https://example.com Label text]`.
 *
 * @param url        The raw URL string.
 * @param labelNodes Parsed label content nodes (may be empty for bare links).
 * @param rawText    Verbatim source including outer brackets.
 */
class ExternalLink(
    val url: String,
    val labelNodes: WikiDocument,
    override val rawText: String,
) : WikiNode() {

    override val children: List<WikiNode> get() = labelNodes.children

    /** Plain text: label if present, otherwise the raw URL. */
    override val plainText: String
        get() = if (labelNodes.children.isNotEmpty()) labelNodes.plainText else url

    val hasLabel: Boolean get() = labelNodes.children.isNotEmpty()

    override fun toString(): String = rawText
}

// ─── 6. Wiki List ────────────────────────────────────────────────────────────

/**
 * The kind of list determined by the dominant/first marker character.
 * A list whose top-level items use more than one marker family is [Mixed].
 */
enum class ListType {
    /** Items introduced by `*` — rendered as bullet points. */
    Unordered,
    /** Items introduced by `#` — rendered as numbered list. */
    Ordered,
    /**
     * Items introduced by `;` (term) and/or `:` (description/indent).
     * HTML equivalent: `<dl>/<dt>/<dd>`.
     */
    Definition,
    /** Top-level items mix more than one of the above marker families. */
    Mixed,
}

/**
 * A contiguous block of wikitext list lines.
 *
 * Wikitext lists are line-oriented: each line that starts with one or more
 * marker characters (`*`, `#`, `;`, `:`) is a list item.  The **depth** of
 * an item equals the number of leading marker characters on that line
 * (e.g. `**` is depth 2, `*#` is depth 2 with a mixed path).  Items at
 * depth n+1 immediately following an item at depth n are its children and
 * appear as a nested [WikiList] in [WikiListItem.subList].
 *
 * @param listType  Dominant list type of the direct children.
 * @param items     Direct child [WikiListItem]s (not grandchildren).
 * @param rawText   Verbatim source lines, newline-joined.
 */
class WikiList(
    val listType: ListType,
    val items: List<WikiListItem>,
    override val rawText: String,
) : WikiNode(), SplittableWikiNode<WikiList> {

    override val children: List<WikiNode> get() = items

    /**
     * Plain-text rendering: each item on its own line, indented by depth,
     * with a type-appropriate bullet.  Ordered lists are numbered within
     * their sibling group.
     */
    override val plainText: String
        get() = buildString {
            var ordinal = 1
            for (item in items) {
                val indent = "  ".repeat(item.depth - 1)
                when (item.marker) {
                    '*'  -> appendLine("$indent• ${item.content.plainText.trim()}")
                    '#'  -> { appendLine("$indent${ordinal++}. ${item.content.plainText.trim()}") }
                    ';'  -> appendLine("$indent${item.content.plainText.trim()}:")
                    ':'  -> appendLine("${indent}  ${item.content.plainText.trim()}")
                    else -> appendLine("$indent• ${item.content.plainText.trim()}")
                }
                if (item.marker != '#') ordinal = 1
                item.subList?.let { append(it.plainText) }
            }
        }.trimEnd('\n')

    override fun toString(): String = rawText

    override fun splitPlain(index: Int) = splitChildrenPlain(items, index,
        {c, i -> c.splitPlain(i)})
        .map {c -> if (c == children) this else WikiList(listType, c,
            c.joinToString("\n") { it.rawText })}
}

/**
 * A single item inside a [WikiList].
 *
 * @param marker   The marker character at this item's own level
 *                 (`*` unordered, `#` ordered, `;` term, `:` description).
 * @param depth    Absolute nesting depth, 1-based (matches number of leading
 *                 marker chars on the source line).
 * @param content  Parsed item content — everything after the marker chars on
 *                 that line.  May contain templates, links, etc.
 * @param subList  Optional nested [WikiList] for items at depth+1 that
 *                 immediately follow this item in the source.
 * @param rawText  Verbatim source line(s) that make up this item and its
 *                 descendant sub-items.
 */
class WikiListItem(
    val marker: Char,
    val depth: Int,
    val content: WikiDocument,
    val subList: WikiList?,
    override val rawText: String,
) : WikiNode(), SplittableWikiNode<WikiListItem> {

    override val children: List<WikiNode>
        get() = buildList {
            addAll(content.children)
            subList?.let { add(it) }
        }

    /** Plain text of this item's content (without bullet/ordinal prefix). */
    override val plainText: String
        get() = buildString {
            append(content.plainText.trim())
            subList?.let { append("\n"); append(it.plainText) }
        }

    val isUnordered:   Boolean get() = marker == '*'
    val isOrdered:     Boolean get() = marker == '#'
    val isTerm:        Boolean get() = marker == ';'
    val isDescription: Boolean get() = marker == ':'

    override fun toString(): String = rawText
    override fun splitPlain(index: Int): WikiNodeSplit<WikiListItem> {
        val contentSplit = content.splitPlain(index)
        if (subList == null) {
            return contentSplit
                .map { c -> WikiListItem(marker, depth, c, null, c.rawText) }
        }

        if (contentSplit.pre == null) return WikiNodeSplit<WikiListItem>(null, this)
        if (contentSplit.post != null) {
            val preRaw = contentSplit.pre.rawText
            return WikiNodeSplit<WikiListItem>(
                WikiListItem(marker, depth, contentSplit.pre, null, preRaw),
                WikiListItem(marker, depth, contentSplit.post, subList, rawText.substring(preRaw.length)),
            )
        }

        // split after content
        val subSplit = subList.splitPlain(index - content.plainText.length)
        if (subSplit.isEmpty()) {
            return WikiNodeSplit<WikiListItem>(null, this)
        }
        if (subSplit.size == 1) {
            return WikiNodeSplit<WikiListItem>(
                WikiListItem(marker, depth, content, null, content.rawText),
                WikiListItem(marker, depth, WikiDocument(emptyList()), subList, subList.rawText),
            )
        }
        val postRaw = subSplit.post!!.rawText
        return WikiNodeSplit<WikiListItem>(
            WikiListItem(marker, depth, content, subSplit.pre, rawText.substring(0, rawText.length - postRaw.length)),
            WikiListItem(marker, depth, WikiDocument(emptyList()), subSplit.post, postRaw),
        )
    }
}

// ─── 7. Wiki Table ───────────────────────────────────────────────────────────

/**
 * A wiki-syntax table: `{| … |}`.
 *
 * The table is decomposed into an optional [caption] and a list of [rows],
 * each containing [TableCell] objects. Cell and caption content is fully parsed.
 *
 * @param attributes  Raw attribute string from the opening `{|` line.
 * @param caption     Optional parsed caption node.
 * @param rows        List of table rows.
 * @param rawText     Verbatim source.
 */
class WikiTable(
    val attributes: String,
    val caption: TableCaption?,
    val rows: List<TableRow>,
    override val rawText: String,
) : WikiNode() {

    override val children: List<WikiNode>
        get() = buildList {
            caption?.let { add(it) }
            addAll(rows)
        }

    /** Plain text: concatenate all cell plain texts separated by whitespace. */
    override val plainText: String
        get() = buildString {
            caption?.let { append(it.plainText); append("\n") }
            for (row in rows) {
                append(row.cells.joinToString("\t") { it.plainText })
                append("\n")
            }
        }.trim()

    override fun toString(): String = rawText
}

/** The `|+` caption line of a wiki table. */
class TableCaption(
    val attributesRaw: String,
    val content: WikiDocument,
    override val rawText: String,
) : WikiNode() {
    override val plainText: String   get() = content.plainText
    override val children: List<WikiNode> get() = content.children
}

/** A `|-` row inside a wiki table. */
class TableRow(
    val attributesRaw: String,
    val cells: List<TableCell>,
    override val rawText: String,
) : WikiNode() {
    override val children: List<WikiNode> get() = cells
    override val plainText: String get() = cells.joinToString("\t") { it.plainText }
}

/** A single `|` (data) or `!` (header) cell inside a [TableRow]. */
class TableCell(
    val isHeader: Boolean,
    val attributesRaw: String,
    val content: WikiDocument,
    override val rawText: String,
) : WikiNode() {
    override val children: List<WikiNode> get() = content.children
    override val plainText: String get() = content.plainText
}
