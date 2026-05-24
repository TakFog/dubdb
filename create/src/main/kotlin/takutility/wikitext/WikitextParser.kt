package takutility.wikitext

import takutility.wikitext.WikitextParser.buildListTree


// ════════════════════════════════════════════════════════════════════════════
//  Main Recursive-Descent Parser
// ════════════════════════════════════════════════════════════════════════════

/**
 * Parses arbitrary wikitext into a tree of [WikiNode] objects.
 *
 * Usage:
 * ```kotlin
 * val doc = WikitextParser.parse(someWikitext)
 * ```
 *
 * The parser handles four structured constructs (each self-delimiting and
 * nestable inside one another):
 *
 * | Construct       | Syntax                    | Node type        |
 * |-----------------|---------------------------|------------------|
 * | Template        | `{{Name|…}}`              | [WikiTemplate]   |
 * | Internal link   | `[[Target|…]]`            | [WikiLink]       |
 * | External link   | `[url label]`             | [ExternalLink]   |
 * | Table           | `{|…|}`                   | [WikiTable]      |
 * | List            | lines starting with `*#;:` | [WikiList]       |
 *
 * Everything else becomes a [TextNode].  All constructs are parsed
 * recursively: a template argument's value is itself a parsed [WikiDocument],
 * a wikilink's display is parsed, table cells are parsed, list item content
 * is parsed, and so on.
 */
object WikitextParser {

    // ── Public entry point ────────────────────────────────────────────────────

    /** Parse [wikitext] and return the root [WikiDocument]. */
    fun parse(wikitext: String): WikiDocument =
        WikiDocument(parseNodes(wikitext))

    // ── Core recursive node parser ────────────────────────────────────────────

    /**
     * Turn [text] into a list of [WikiNode]s, consuming one top-level
     * construct at a time and recursing into each.
     */
    internal fun parseNodes(text: String): List<WikiNode> {
        val nodes = mutableListOf<WikiNode>()
        val plain = StringBuilder()
        var i = 0

        fun flushPlain() {
            if (plain.isNotEmpty()) {
                nodes.add(TextNode(plain.toString()))
                plain.clear()
            }
        }

        while (i < text.length) {
            when {
                // ── Wiki table {| … |} — must be tested before {{ ─────────────
                text.startsWith("{|", i) -> {
                    val end = findMatchingClose(text, i, "{|", "|}")
                    if (end != -1) {
                        flushPlain()
                        nodes.add(parseTable(text.substring(i, end), i))
                        i = end
                    } else { plain.append(text[i]); i++ }
                }

                // ── Template {{ … }} ──────────────────────────────────────────
                text.startsWith("{{", i) -> {
                    val end = findMatchingClose(text, i, "{{", "}}")
                    if (end != -1) {
                        flushPlain()
                        nodes.add(parseTemplate(text.substring(i, end)))
                        i = end
                    } else { plain.append(text[i]); i++ }
                }

                // ── Internal link [[ … ]] ─────────────────────────────────────
                text.startsWith("[[", i) -> {
                    val end = findMatchingClose(text, i, "[[", "]]")
                    if (end != -1) {
                        flushPlain()
                        nodes.add(parseWikiLink(text.substring(i, end)))
                        i = end
                    } else { plain.append(text[i]); i++ }
                }

                // ── Wiki list (* # ; :) — line-start marker ──────────────────
                // A list block is one or more consecutive lines whose first
                // non-empty character is a list marker.  We must be at the
                // very start of a line (position 0 or immediately after '\n').
                (i == 0 || text[i - 1] == '\n') && text[i] in "*#;:" -> {
                    val listStart = i
                    var j = i
                    while (j < text.length) {
                        // Find where this line ends (include the '\n')
                        val lineEnd = text.indexOf('\n', j)
                            .let { if (it == -1) text.length else it + 1 }
                        if (j < text.length && text[j] in "*#;:") j = lineEnd else break
                    }
                    flushPlain()
                    val listEnd = if (j > listStart && text.getOrNull(j - 1) == '\n') j - 1 else j
                    nodes.add(parseListBlock(text.substring(listStart, listEnd)))
                    i = listEnd   // '\n' falls through to the next plain-text accumulation
                }

                // ── External link [ url label ] ───────────────────────────────
                text[i] == '[' &&
                (i + 1 >= text.length || text[i + 1] != '[') &&
                isUrlStart(text, i + 1) -> {
                    val end = findMatchingClose(text, i, "[", "]")
                    if (end != -1) {
                        flushPlain()
                        nodes.add(parseExternalLink(text.substring(i, end)))
                        i = end
                    } else { plain.append(text[i]); i++ }
                }

                // ── Plain character ───────────────────────────────────────────
                else -> { plain.append(text[i]); i++ }
            }
        }
        flushPlain()
        return nodes
    }

    // ── Template parser ───────────────────────────────────────────────────────

    /**
     * Parse a raw template string (including surrounding `{{` … `}}`).
     *
     * The inner content is split on top-level `|` characters, giving:
     *   parts[0] → template name
     *   parts[1..] → argument fragments, each either `value` or `key=value`
     *
     * Both key and value are recursively parsed into [WikiDocument]s.
     */
    fun parseTemplate(raw: String): WikiTemplate {
        require(raw.startsWith("{{") && raw.endsWith("}}")) {
            "parseTemplate called with non-template string: $raw"
        }
        val inner = raw.substring(2, raw.length - 2)
        val parts = splitOnTopLevelPipes(inner)

        val name = parts.firstOrNull()?.trim() ?: ""
        val arguments = mutableListOf<TemplateArgument>()
        var positionalCounter = 1

        for (idx in 1 until parts.size) {
            val part = parts[idx]
            val eqPos = findFirstTopLevelEquals(part)
            if (eqPos != -1) {
                val keyRaw = part.substring(0, eqPos)
                val valRaw = part.substring(eqPos + 1)
                arguments.add(
                    TemplateArgument(
                        index   = null,
                        name    = WikiDocument(parseNodes(keyRaw.trim())),
                        value   = WikiDocument(parseNodes(valRaw.trim())),
                        rawText = part,
                    )
                )
            } else {
                arguments.add(
                    TemplateArgument(
                        index   = positionalCounter++,
                        name    = null,
                        value   = WikiDocument(parseNodes(part)),
                        rawText = part,
                    )
                )
            }
        }
        return WikiTemplate(name = name, arguments = arguments, rawText = raw)
    }

    // ── Wikilink parser ───────────────────────────────────────────────────────

    /**
     * Parse a raw wikilink string (including `[[` … `]]`).
     *
     * Format: `[[Target]]` or `[[Target|Display]]`.
     * The display part may itself contain templates, links, etc., so it is
     * recursively parsed.  The target is kept as a raw string (page names
     * should not be further interpreted).
     */
    fun parseWikiLink(raw: String): WikiLink {
        require(raw.startsWith("[[") && raw.endsWith("]]")) {
            "parseWikiLink called with non-link string: $raw"
        }
        val inner = raw.substring(2, raw.length - 2)

        // Split on first top-level `|` only
        val pipeIdx = findFirstTopLevelPipe(inner)
        val target: String
        val displayDoc: WikiDocument

        if (pipeIdx == -1) {
            target     = inner
            displayDoc = WikiDocument(emptyList())
        } else {
            target     = inner.substring(0, pipeIdx)
            displayDoc = WikiDocument(parseNodes(inner.substring(pipeIdx + 1)))
        }
        return WikiLink(target = target, displayNodes = displayDoc, rawText = raw)
    }

    // ── External link parser ──────────────────────────────────────────────────

    /**
     * Parse a raw external link string (including outer `[` `]`).
     *
     * Format: `[https://url]` or `[https://url Label text]`.
     * The URL is terminated by the first whitespace; everything after that up
     * to the `]` is the label, recursively parsed.
     */
    fun parseExternalLink(raw: String): ExternalLink {
        require(raw.startsWith("[") && raw.endsWith("]")) {
            "parseExternalLink called with non-link string: $raw"
        }
        val inner = raw.substring(1, raw.length - 1)

        val spaceIdx = inner.indexOfFirst { it.isWhitespace() }
        val url: String
        val labelDoc: WikiDocument

        if (spaceIdx == -1) {
            url      = inner
            labelDoc = WikiDocument(emptyList())
        } else {
            url      = inner.substring(0, spaceIdx)
            labelDoc = WikiDocument(parseNodes(inner.substring(spaceIdx + 1)))
        }
        return ExternalLink(url = url, labelNodes = labelDoc, rawText = raw)
    }

    // ── List parser ───────────────────────────────────────────────────────────

    /**
     * Internal representation of a single parsed list line before tree
     * assembly.
     *
     * @param markers  The leading marker string, e.g. `"**"` or `"*#"`.
     * @param content  Everything on the line after the markers (trimmed).
     * @param rawLine  The verbatim line (without trailing newline).
     */
    private data class ParsedLine(
        val markers: String,
        val content: String,
        val rawLine: String,
    ) {
        val depth: Int  get() = markers.length
        val marker: Char get() = markers.last()
    }

    /**
     * Parse a raw block of consecutive wikitext list lines into a [WikiList].
     *
     * The block is split into individual lines; each line has its leading
     * marker characters extracted to determine depth and type.  The resulting
     * flat list is handed to [buildListTree] which groups siblings and nests
     * children recursively.
     *
     * Lines that are empty or that do not start with a marker (e.g. a blank
     * line that slipped through) are silently skipped.
     */
    fun parseListBlock(raw: String): WikiList {
        val parsedLines = raw.lines()
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                val markers = line.takeWhile { it in "*#;:" }
                if (markers.isEmpty()) null
                else ParsedLine(
                    markers = markers,
                    content = line.substring(markers.length),   // keep leading space if any
                    rawLine = line,
                )
            }
        return buildListTree(parsedLines)
    }

    /**
     * Recursively assemble a [WikiList] from a flat sequence of [ParsedLine]s.
     *
     * Strategy: walk the list left-to-right.  For each item, collect all
     * immediately following lines whose depth is strictly greater than the
     * current line's depth — those become its [WikiListItem.subList].  The
     * scan then resumes at the first line whose depth is ≤ the current line's
     * depth (i.e. back at the same or outer level).
     *
     * This naturally handles depth-skipping (e.g. `*` immediately followed by
     * `***` with no `**` in between) by treating the deeper line as a direct
     * child.
     */
    private fun buildListTree(lines: List<ParsedLine>): WikiList {
        val items    = mutableListOf<WikiListItem>()
        var i        = 0

        while (i < lines.size) {
            val current = lines[i]

            // Collect child lines: everything whose depth > current.depth,
            // stopping as soon as we see a line at current.depth or shallower.
            val childLines = mutableListOf<ParsedLine>()
            var j = i + 1
            while (j < lines.size && lines[j].depth > current.depth) {
                childLines.add(lines[j])
                j++
            }

            val subList  = if (childLines.isNotEmpty()) buildListTree(childLines) else null
            val itemRaw  = (listOf(current) + childLines).joinToString("\n") { it.rawLine }

            items.add(
                WikiListItem(
                    marker  = current.marker,
                    depth   = current.depth,
                    content = WikiDocument(parseNodes(current.content)),
                    subList = subList,
                    rawText = itemRaw,
                )
            )
            i = j
        }

        return WikiList(
            listType = determineListType(items),
            items    = items,
            rawText  = lines.joinToString("\n") { it.rawLine },
        )
    }

    /**
     * Infer the [ListType] of a list from the markers used by its direct items.
     *
     * - All `*`         → [ListType.Unordered]
     * - All `#`         → [ListType.Ordered]
     * - Only `;` / `:`  → [ListType.Definition]
     * - Otherwise       → [ListType.Mixed]
     */
    private fun determineListType(items: List<WikiListItem>): ListType {
        val markers = items.map { it.marker }.toSet()
        return when {
            markers == setOf('*')              -> ListType.Unordered
            markers == setOf('#')              -> ListType.Ordered
            markers.all { it == ';' || it == ':' } -> ListType.Definition
            else                               -> ListType.Mixed
        }
    }



    /**
     * Parse a raw wiki table string (including `{|` … `|}`).
     *
     * Wiki table syntax:
     * ```
     * {| attributes
     * |+ Caption text
     * |-
     * ! Header1 !! Header2
     * |-
     * | Cell1 || Cell2
     * | Cell3 || Cell4
     * |}
     * ```
     *
     * Rows are delimited by `|-` lines (or the start of data after the opening
     * line).  Cells within a row are either on separate lines starting with `|`
     * / `!`, or separated by `||` / `!!` within a single line.  Each cell's
     * content is recursively parsed.
     */
    fun parseTable(raw: String, offset: Int = 0): WikiTable {
        val lines = raw.lines()
        // Opening line: `{| attributes`
        val attributesRaw = if (lines.isNotEmpty()) lines[0].removePrefix("{|").trim() else ""

        var caption: TableCaption? = null
        val rows = mutableListOf<TableRow>()

        // We process logical lines; each `|-` starts a new row accumulator.
        // We also need to handle implicit first row (data before any `|-`).
        var currentRowAttrs    = ""
        var currentRowRawLines = mutableListOf<String>()
        var inFirstImplicitRow = false

        // Helper: flush accumulated lines into a TableRow
        fun flushRow() {
            if (currentRowRawLines.isEmpty()) return
            val rowRaw  = currentRowRawLines.joinToString("\n")
            val cells   = parseCells(currentRowRawLines)
            rows.add(TableRow(attributesRaw = currentRowAttrs, cells = cells, rawText = rowRaw))
            currentRowAttrs    = ""
            currentRowRawLines = mutableListOf()
        }

        for (lineIdx in 1 until lines.size) {
            val line = lines[lineIdx]
            when {
                // End of table
                line.trimStart().startsWith("|}") -> flushRow()

                // Row separator `|-`
                line.trimStart().startsWith("|-") -> {
                    flushRow()
                    currentRowAttrs = line.trimStart().removePrefix("|-").trim()
                }

                // Caption `|+`
                line.trimStart().startsWith("|+") -> {
                    val captionContent = line.trimStart().removePrefix("|+").trimStart()
                    // Caption may have attributes: `|+ class="foo" | actual caption`
                    val (capAttrs, capText) = splitCellAttributesFromContent(captionContent)
                    caption = TableCaption(
                        attributesRaw = capAttrs,
                        content       = WikiDocument(parseNodes(capText)),
                        rawText       = line,
                    )
                }

                // Header cell line `!`
                line.trimStart().startsWith("!") -> {
                    inFirstImplicitRow = true
                    currentRowRawLines.add(line)
                }

                // Data cell line `|` (not `|-`, `|+`, `|}`)
                line.trimStart().startsWith("|") &&
                !line.trimStart().startsWith("|-") &&
                !line.trimStart().startsWith("|+") &&
                !line.trimStart().startsWith("|}") -> {
                    inFirstImplicitRow = true
                    currentRowRawLines.add(line)
                }

                // Continuation / template lines inside a cell — append to current row
                else -> {
                    if (currentRowRawLines.isNotEmpty()) {
                        currentRowRawLines.add(line)
                    }
                }
            }
        }
        // If table had no `|}` closing (malformed), flush whatever is left
        flushRow()

        return WikiTable(
            attributes = attributesRaw,
            caption    = caption,
            rows       = rows,
            rawText    = raw,
        )
    }

    // ── Table cell helpers ────────────────────────────────────────────────────

    /**
     * Given a list of raw cell lines belonging to one row, produce a list of
     * [TableCell] objects.  Handles `||` / `!!` inline separators and per-cell
     * attribute prefixes.
     */
    private fun parseCells(lines: List<String>): List<TableCell> {
        val cells = mutableListOf<TableCell>()
        for (line in lines) {
            val stripped = line.trimStart()
            val (isHeader, content) = when {
                stripped.startsWith("!") -> true  to stripped.removePrefix("!").trimStart()
                stripped.startsWith("|") -> false to stripped.removePrefix("|").trimStart()
                else -> continue
            }
            // Split on `||` (data) or `!!` (header) inline separators
            val separator  = if (isHeader) "!!" else "||"
            val cellParts  = splitOnInlineCellSeparator(content, separator)
            for (part in cellParts) {
                val (attrs, cellContent) = splitCellAttributesFromContent(part)
                cells.add(
                    TableCell(
                        isHeader     = isHeader,
                        attributesRaw = attrs,
                        content      = WikiDocument(parseNodes(cellContent)),
                        rawText      = part,
                    )
                )
            }
        }
        return cells
    }

    /**
     * Split a cell line on [separator] (`||` or `!!`) at positions that are
     * NOT inside a nested wikitext construct.
     */
    private fun splitOnInlineCellSeparator(text: String, separator: String): List<String> {
        val parts = mutableListOf<String>()
        val sb    = StringBuilder()
        var i     = 0
        while (i < text.length) {
            when {
                text.startsWith("{{", i) -> {
                    val end = findMatchingClose(text, i, "{{", "}}")
                    if (end != -1) { sb.append(text, i, end); i = end }
                    else { sb.append(text[i]); i++ }
                }
                text.startsWith("[[", i) -> {
                    val end = findMatchingClose(text, i, "[[", "]]")
                    if (end != -1) { sb.append(text, i, end); i = end }
                    else { sb.append(text[i]); i++ }
                }
                text.startsWith(separator, i) -> {
                    parts.add(sb.toString().trim())
                    sb.clear()
                    i += separator.length
                }
                else -> { sb.append(text[i]); i++ }
            }
        }
        parts.add(sb.toString().trim())
        return parts.filter { it.isNotBlank() }
    }

    /**
     * A wiki cell may start with HTML attributes followed by a bare `|`:
     * `class="wikitable" | actual content`.
     * If a top-level `|` is found (not inside a nested construct), everything
     * before it is attributes and everything after is content.
     * Otherwise the whole string is content.
     */
    private fun splitCellAttributesFromContent(text: String): Pair<String, String> {
        val pipeIdx = findFirstTopLevelPipe(text)
        return if (pipeIdx == -1) {
            "" to text
        } else {
            text.substring(0, pipeIdx).trim() to text.substring(pipeIdx + 1).trimStart()
        }
    }

    // ── Shared utility ────────────────────────────────────────────────────────

    /**
     * Find the index of the first top-level `|` in [text] (not inside nested
     * `{{ }}` or `[[ ]]` constructs).  Returns -1 if none found.
     */
    private fun findFirstTopLevelPipe(text: String): Int {
        var i = 0
        while (i < text.length) {
            when {
                text.startsWith("{|", i)  -> {
                    val end = findMatchingClose(text, i, "{|", "|}")
                    i = if (end != -1) end else i + 1
                }
                text.startsWith("{{", i) -> {
                    val end = findMatchingClose(text, i, "{{", "}}")
                    i = if (end != -1) end else i + 1
                }
                text.startsWith("[[", i) -> {
                    val end = findMatchingClose(text, i, "[[", "]]")
                    i = if (end != -1) end else i + 1
                }
                text[i] == '|' -> return i
                else -> i++
            }
        }
        return -1
    }
}
