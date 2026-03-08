package takutility.dubdb.wikitext

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import takutility.wikitext.WikitextParser

// ════════════════════════════════════════════════════════════════════════════
//  10. Cross-cutting: rawText round-trip and plainText contracts
// ════════════════════════════════════════════════════════════════════════════
class ContractTest {

    private val corpus = listOf(
        "{{Short description|A page}}",
        "[[Main Page]]",
        "[[Main Page|go here]]",
        "[https://example.com Visit us]",
        "* bullet\n** nested",
        "# one\n# two",
        "; term\n: definition",
        "{|\n|-\n| A || B\n|}",
        "plain text only",
        "{{T|nested={{Inner}}|link=[[L|D]]}}",
    )

    @Test
    fun `rawText of document always equals input`() {
        for (src in corpus) {
            val doc = WikitextParser.parse(src)
            Assertions.assertEquals(src, doc.rawText, "rawText failed for: $src")
        }
    }

    @Test
    fun `rawText round-trip — list followed by external link on next line`() {
        // Regression: trailing '\n' of the last list line must not be swallowed
        // into the list's rawText, otherwise the '\n' vanishes from the document
        // rawText and the round-trip fails.
        val src = "Hello {{T|x=[[Page]]}} world\n* item\n[https://x.com Y]"
        Assertions.assertEquals(src, WikitextParser.parse(src).rawText)
    }

    @Test
    fun `rawText round-trip — list followed by plain text on next line`() {
        val src = "* item\nsome trailing text"
        Assertions.assertEquals(src, WikitextParser.parse(src).rawText)
    }

    @Test
    fun `rawText round-trip — list at end of string has no trailing newline`() {
        val src = "text\n* only item"
        Assertions.assertEquals(src, WikitextParser.parse(src).rawText)
    }

    @Test
    fun `rawText round-trip — list with trailing newline at end of string`() {
        val src = "* item\n"
        Assertions.assertEquals(src, WikitextParser.parse(src).rawText)
    }

    @Test
    fun `toString always returns rawText`() {
        for (src in corpus) {
            val doc = WikitextParser.parse(src)
            doc.walk().forEach { node ->
                Assertions.assertEquals(
                    node.rawText, node.toString(),
                    "toString ≠ rawText for ${node::class.simpleName}"
                )
            }
        }
    }

    @Test
    fun `plainText never contains double-braces`() {
        for (src in corpus) {
            val plain = WikitextParser.parse(src).plainText
            Assertions.assertFalse(
                plain.contains("{{"),
                "plainText should not contain '{{' for: $src\nGot: $plain"
            )
        }
    }

    @Test
    fun `plainText never contains double-brackets`() {
        for (src in corpus) {
            val plain = WikitextParser.parse(src).plainText
            Assertions.assertFalse(
                plain.contains("[["),
                "plainText should not contain '[[' for: $src\nGot: $plain"
            )
        }
    }
}