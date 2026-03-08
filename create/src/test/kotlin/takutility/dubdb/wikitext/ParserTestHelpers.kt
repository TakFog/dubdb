package takutility.dubdb.wikitext

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import takutility.wikitext.WikiNode
import takutility.wikitext.WikitextParser

// ─── helpers ─────────────────────────────────────────────────────────────────

/** Parse [wikitext] and return the single top-level node, asserting there is exactly one. */
fun parseSingle(wikitext: String): WikiNode {
    val doc = WikitextParser.parse(wikitext)
    assertEquals(1, doc.children.size,
        "Expected exactly 1 top-level node, got ${doc.children.size}: ${doc.children}")
    return doc.children.first()
}

/** Assert that [this] node's rawText equals [expected]. */
fun WikiNode.assertRaw(expected: String) =
    assertEquals(expected, rawText, "rawText mismatch")

/** Assert that [this] node's plainText equals [expected]. */
fun WikiNode.assertPlain(expected: String) =
    assertEquals(expected, plainText, "plainText mismatch")

/** Assert [this] is-a [T] and return it cast. */
inline fun <reified T : WikiNode> WikiNode.assertIs(msg: String = ""): T {
    assertInstanceOf(T::class.java, this, "Expected ${T::class.simpleName} but was ${this::class.simpleName}. $msg")
    return this as T
}

// ════════════════════════════════════════════════════════════════════════════
//  1. Scanner utilities
// ════════════════════════════════════════════════════════════════════════════

// ════════════════════════════════════════════════════════════════════════════
//  2. WikiDocument
// ════════════════════════════════════════════════════════════════════════════

// ════════════════════════════════════════════════════════════════════════════
//  3. TextNode
// ════════════════════════════════════════════════════════════════════════════

// ════════════════════════════════════════════════════════════════════════════
//  4. WikiTemplate
// ════════════════════════════════════════════════════════════════════════════

// ════════════════════════════════════════════════════════════════════════════
//  5. WikiLink
// ════════════════════════════════════════════════════════════════════════════

// ════════════════════════════════════════════════════════════════════════════
//  6. ExternalLink
// ════════════════════════════════════════════════════════════════════════════

// ════════════════════════════════════════════════════════════════════════════
//  7. WikiList
// ════════════════════════════════════════════════════════════════════════════

// ════════════════════════════════════════════════════════════════════════════
//  8. WikiTable
// ════════════════════════════════════════════════════════════════════════════

// ════════════════════════════════════════════════════════════════════════════
//  9. Parser dispatch — mixed documents
// ════════════════════════════════════════════════════════════════════════════

