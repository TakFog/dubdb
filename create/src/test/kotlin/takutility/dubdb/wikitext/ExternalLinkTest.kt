package takutility.dubdb.wikitext

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import takutility.wikitext.ExternalLink
import takutility.wikitext.WikiLink
import takutility.wikitext.WikitextParser

class ExternalLinkTest {

    @Test
    fun `bare external link URL is plainText`() {
        val link = parseSingle("[https://example.com]").assertIs<ExternalLink>()
        Assertions.assertEquals("https://example.com", link.url)
        Assertions.assertFalse(link.hasLabel)
        link.assertPlain("https://example.com")
    }

    @Test
    fun `external link with label uses label as plainText`() {
        val link = parseSingle("[https://example.com Example site]").assertIs<ExternalLink>()
        Assertions.assertEquals("https://example.com", link.url)
        Assertions.assertTrue(link.hasLabel)
        link.assertPlain("Example site")
    }

    @Test
    fun `external link rawText reconstructs source`() {
        val src = "[https://example.com My site]"
        val link = parseSingle(src).assertIs<ExternalLink>()
        link.assertRaw(src)
        Assertions.assertEquals(src, link.toString())
    }

    @Test
    fun `label is recursively parsed`() {
        val link = parseSingle("[https://example.com See [[Article|here]]]").assertIs<ExternalLink>()
        val wikiLinks = link.labelNodes.walk().filterIsInstance<WikiLink>().toList()
        Assertions.assertEquals(1, wikiLinks.size)
        Assertions.assertEquals("Article", wikiLinks.first().target)
    }

    @Test
    fun `http scheme is recognised`() {
        val link = parseSingle("[http://example.com label]").assertIs<ExternalLink>()
        Assertions.assertEquals("http://example.com", link.url)
    }

    @Test
    fun `ftp scheme is recognised`() {
        val link = parseSingle("[ftp://files.example.com FTP]").assertIs<ExternalLink>()
        Assertions.assertEquals("ftp://files.example.com", link.url)
    }

    @Test
    fun `bare bracket with no url scheme is not parsed as external link`() {
        // "[not-a-url]" should be left as plain text
        val doc = WikitextParser.parse("[not-a-url]")
        Assertions.assertTrue(doc.walk().filterIsInstance<ExternalLink>().none())
    }
}