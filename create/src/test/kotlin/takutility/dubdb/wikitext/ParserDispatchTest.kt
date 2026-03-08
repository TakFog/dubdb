package takutility.dubdb.wikitext

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import takutility.wikitext.*

// Parser dispatch — mixed documents
class ParserDispatchTest {

    @Test
    fun `document with all five node types at top level`() {
        val src = "Text {{T}} [[L]] [https://x.com X]\n* item"
        val doc = WikitextParser.parse(src)
        val types = doc.children.map { it::class.simpleName }.toSet()
        Assertions.assertTrue("TextNode" in types)
        Assertions.assertTrue("WikiTemplate" in types)
        Assertions.assertTrue("WikiLink" in types)
        Assertions.assertTrue("ExternalLink" in types)
        Assertions.assertTrue("WikiList" in types)
    }

    @Test
    fun `table at top level surrounded by text`() {
        val src = "Before\n{|\n|-\n| cell\n|}\nAfter"
        val doc = WikitextParser.parse(src)
        Assertions.assertTrue(doc.children.any { it is WikiTable })
        Assertions.assertTrue(doc.children.any { it is TextNode })
    }

    @Test
    fun `two consecutive templates`() {
        val doc = WikitextParser.parse("{{A}}{{B}}")
        Assertions.assertEquals(2, doc.topLevelTemplates().size)
        Assertions.assertEquals("A", doc.topLevelTemplates()[0].name)
        Assertions.assertEquals("B", doc.topLevelTemplates()[1].name)
    }

    @Test
    fun `rawText of document equals input exactly`() {
        val src = "Hello {{T|x=[[Page]]}} world\n* item\n[https://x.com Y]"
        Assertions.assertEquals(src, WikitextParser.parse(src).rawText)
    }

    @Test
    fun `unclosed template brace falls back to plain text`() {
        // "{{unclosed" has no matching "}}", should become TextNode
        val doc = WikitextParser.parse("{{unclosed")
        Assertions.assertTrue(doc.walk().filterIsInstance<WikiTemplate>().none())
        Assertions.assertTrue(doc.walk().filterIsInstance<TextNode>().any())
    }

    @Test
    fun `unclosed wikilink falls back to plain text`() {
        val doc = WikitextParser.parse("[[unclosed")
        Assertions.assertTrue(doc.walk().filterIsInstance<WikiLink>().none())
    }

    @Test
    fun `external link without closing bracket falls back to plain text`() {
        val doc = WikitextParser.parse("[https://example.com no close")
        Assertions.assertTrue(doc.walk().filterIsInstance<ExternalLink>().none())
    }

    @Test
    fun `list immediately at start of string`() {
        val doc = WikitextParser.parse("* first item")
        Assertions.assertTrue(doc.children.any { it is WikiList })
    }

    @Test
    fun `consecutive list blocks separated by blank line are separate nodes`() {
        val src = "* A\n* B\n\n* C\n* D"
        val doc = WikitextParser.parse(src)
        val lists = doc.children.filterIsInstance<WikiList>()
        Assertions.assertEquals(2, lists.size)
        Assertions.assertEquals(2, lists[0].items.size)
        Assertions.assertEquals(2, lists[1].items.size)
    }
}