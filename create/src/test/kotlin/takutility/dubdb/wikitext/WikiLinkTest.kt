package takutility.dubdb.wikitext

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import takutility.wikitext.WikiLink
import takutility.wikitext.WikiTemplate

class WikiLinkTest {

    @Test
    fun `bare link target and plainText`() {
        val link = parseSingle("[[Quantum computing]]").assertIs<WikiLink>()
        Assertions.assertEquals("Quantum computing", link.target)
        Assertions.assertFalse(link.hasDisplay)
        link.assertPlain("Quantum computing")
        link.assertRaw("[[Quantum computing]]")
    }

    @Test
    fun `link with display text uses display as plainText`() {
        val link = parseSingle("[[Massachusetts Institute of Technology|MIT]]").assertIs<WikiLink>()
        Assertions.assertEquals("Massachusetts Institute of Technology", link.target)
        Assertions.assertTrue(link.hasDisplay)
        link.assertPlain("MIT")
    }

    @Test
    fun `namespace prefix stripped from bare link plainText`() {
        val link = parseSingle("[[File:Image.jpg]]").assertIs<WikiLink>()
        Assertions.assertEquals("File:Image.jpg", link.target)
        link.assertPlain("Image.jpg")
    }

    @Test
    fun `anchor fragment stripped from bare link plainText`() {
        val link = parseSingle("[[History#Ancient]]").assertIs<WikiLink>()
        link.assertPlain("History")
    }

    @Test
    fun `display content is recursively parsed`() {
        val link = parseSingle("[[Page|see {{em|this}}]]").assertIs<WikiLink>()
        val templates = link.displayNodes.walk().filterIsInstance<WikiTemplate>().toList()
        Assertions.assertEquals(1, templates.size)
        Assertions.assertEquals("em", templates.first().name)
    }

    @Test
    fun `link rawText reconstructs source`() {
        val src = "[[Some page|display text]]"
        val link = parseSingle(src).assertIs<WikiLink>()
        link.assertRaw(src)
        Assertions.assertEquals(src, link.toString())
    }

    @Test
    fun `link with no display has empty children`() {
        val link = parseSingle("[[Page]]").assertIs<WikiLink>()
        Assertions.assertTrue(link.children.isEmpty())
    }

    @Test
    fun `link with display text has children from display`() {
        val link = parseSingle("[[Page|hello world]]").assertIs<WikiLink>()
        Assertions.assertFalse(link.children.isEmpty())
    }
}