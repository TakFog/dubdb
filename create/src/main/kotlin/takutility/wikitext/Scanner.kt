package takutility.wikitext

// ════════════════════════════════════════════════════════════════════════════
//  Low-level scanner helpers
// ════════════════════════════════════════════════════════════════════════════

/** Known external-link URL schemes (bare `//` is also valid in wikitext). */
private val URL_SCHEMES = listOf(
    "https://", "http://", "ftp://", "ftps://", "//",
    "mailto:", "news:", "irc://", "ircs://", "gopher://",
)

/** Returns true when position [i] in [text] starts a recognised URL. */
internal fun isUrlStart(text: String, i: Int): Boolean =
    URL_SCHEMES.any { text.startsWith(it, i) }

/**
 * Starting from [start] (which must point at an [open] token), scan forward
 * and return the exclusive index just past the matching [close] token,
 * respecting nesting.  Returns -1 if there is no match.
 *
 * Works correctly for symmetric openers/closers ({{ }}, [[ ]], {| |}).
 */
internal fun findMatchingClose(
    text: String,
    start: Int,
    open: String,
    close: String,
): Int {
    // Sanity: open and close must not be identical (would loop forever)
    require(open != close) { "open and close tokens must differ" }
    var depth = 0
    var i = start
    while (i < text.length) {
        when {
            text.startsWith(open, i)  -> { depth++; i += open.length }
            text.startsWith(close, i) -> {
                depth--; i += close.length
                if (depth == 0) return i
            }
            else -> i++
        }
    }
    return -1
}

/**
 * Split [text] on `|` characters that are at bracket depth 0.
 * Nested `{{ }}`, `[[ ]]`, `[ ]`, and `{| |}` blocks are skipped intact.
 */
internal fun splitOnTopLevelPipes(text: String): List<String> {
    val parts = mutableListOf<String>()
    val sb    = StringBuilder()
    var i     = 0
    while (i < text.length) {
        when {
            // Wiki table — must be checked before `{{`
            text.startsWith("{|", i) -> {
                val end = findMatchingClose(text, i, "{|", "|}")
                if (end != -1) { sb.append(text, i, end); i = end }
                else { sb.append(text[i]); i++ }
            }
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
            // External link — only a real link bracket, not `[[`
            text[i] == '[' && (i + 1 >= text.length || text[i + 1] != '[') -> {
                val closeIdx = text.indexOf(']', i + 1)
                if (closeIdx != -1) {
                    sb.append(text, i, closeIdx + 1); i = closeIdx + 1
                } else { sb.append(text[i]); i++ }
            }
            text[i] == '|' -> { parts.add(sb.toString()); sb.clear(); i++ }
            else            -> { sb.append(text[i]); i++ }
        }
    }
    parts.add(sb.toString())
    return parts
}

/**
 * In [text], find the index of the first `=` that is at bracket depth 0.
 * Returns -1 if none is found.
 */
internal fun findFirstTopLevelEquals(text: String): Int {
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
            text[i] == '[' && (i + 1 >= text.length || text[i + 1] != '[') -> {
                val closeIdx = text.indexOf(']', i + 1)
                i = if (closeIdx != -1) closeIdx + 1 else i + 1
            }
            text[i] == '=' -> return i
            else -> i++
        }
    }
    return -1
}
