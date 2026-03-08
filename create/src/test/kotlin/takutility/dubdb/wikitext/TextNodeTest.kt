package takutility.dubdb.wikitext

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import takutility.wikitext.TextNode

class TextNodeTest {

    @Test
    fun `plain text is preserved verbatim`() {
        val node = parseSingle("Hello, world!").assertIs<TextNode>()
        node.assertRaw("Hello, world!")
        node.assertPlain("Hello, world!")
        Assertions.assertEquals("Hello, world!", node.toString())
    }

    @Test
    fun `text containing special characters`() {
        val src = "a < b & c > d"
        val node = parseSingle(src).assertIs<TextNode>()
        node.assertRaw(src)
    }

    @Test
    fun `text has no children`() {
        val node = TextNode("hi")
        Assertions.assertTrue(node.children.isEmpty())
    }
}