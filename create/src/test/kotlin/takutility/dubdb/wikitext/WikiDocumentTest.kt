package takutility.dubdb.wikitext

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import takutility.wikitext.WikitextParser

class WikiDocumentTest {

    @Test
    fun `empty document has no children`() {
        val doc = WikitextParser.parse("")
        Assertions.assertTrue(doc.children.isEmpty())
        Assertions.assertEquals("", doc.rawText)
        Assertions.assertEquals("", doc.plainText)
    }

    @Test
    fun `rawText reconstructs original`() {
        val src = "Hello [[World]] and {{tmpl}}"
        val doc = WikitextParser.parse(src)
        Assertions.assertEquals(src, doc.rawText)
        Assertions.assertEquals(src, doc.toString())
    }

    @Test
    fun `topLevelTemplates returns only direct template children`() {
        val doc = WikitextParser.parse("{{A}} text {{B|{{nested}}}}")
        val top = doc.topLevelTemplates()
        Assertions.assertEquals(2, top.size)
        Assertions.assertEquals("A", top[0].name)
        Assertions.assertEquals("B", top[1].name)
    }

    @Test
    fun `allTemplates returns templates at every depth`() {
        val doc = WikitextParser.parse("{{Outer|{{Inner}}}}")
        val all = doc.allTemplates()
        Assertions.assertEquals(2, all.size)
        val names = all.map { it.name }.toSet()
        Assertions.assertTrue("Outer" in names)
        Assertions.assertTrue("Inner" in names)
    }

    @Test
    fun `walk visits every node depth-first`() {
        val doc = WikitextParser.parse("{{T|[[Link]]}}")
        val types = doc.walk().map { it::class.simpleName }.toList()
        // WikiDocument → WikiTemplate → TemplateArgument → WikiLink (inside value) …
        Assertions.assertTrue("WikiDocument" in types)
        Assertions.assertTrue("WikiTemplate" in types)
        Assertions.assertTrue("WikiLink" in types)
    }

    @Test
    fun `plainText concatenates children plain texts`() {
        val doc = WikitextParser.parse("Hello [[World|world]]!")
        Assertions.assertEquals("Hello world!", doc.plainText)
    }
}