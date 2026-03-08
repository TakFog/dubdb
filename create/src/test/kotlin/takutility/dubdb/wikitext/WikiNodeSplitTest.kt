package takutility.dubdb.wikitext

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import takutility.wikitext.TextNode
import takutility.wikitext.WikiList
import takutility.wikitext.WikiNodeSplit
import takutility.wikitext.WikitextParser

class WikiNodeSplitTest {

    @Test
    fun `WikiNodeSplit methods`() {
        val emptySplit = WikiNodeSplit<String>(null, null)
        assertTrue(emptySplit.isEmpty())
        assertEquals(0, emptySplit.size)

        val preSplit = WikiNodeSplit("Pre", null)
        assertFalse(preSplit.isEmpty())
        assertEquals(1, preSplit.size)

        val postSplit = WikiNodeSplit(null, "Post")
        assertFalse(postSplit.isEmpty())
        assertEquals(1, postSplit.size)

        val fullSplit = WikiNodeSplit("Pre", "Post")
        assertFalse(fullSplit.isEmpty())
        assertEquals(2, fullSplit.size)

        val mapped = fullSplit.map { it.length }
        assertEquals(3, mapped.pre)
        assertEquals(4, mapped.post)
    }

    @Test
    fun `TextNode split at middle`() {
        val node = TextNode("Hello World")
        val split = node.splitPlain(5)
        
        assertNotNull(split.pre)
        assertNotNull(split.post)
        assertEquals("Hello", split.pre?.plainText)
        assertEquals(" World", split.post?.plainText)
        assertEquals(2, split.size)
    }

    @Test
    fun `TextNode split at 0`() {
        val node = TextNode("Hello World")
        val split = node.splitPlain(0)
        
        assertNull(split.pre)
        assertNotNull(split.post)
        assertEquals("Hello World", split.post?.plainText)
        assertEquals(1, split.size)
    }

    @Test
    fun `TextNode split at end`() {
        val node = TextNode("Hello")
        val split = node.splitPlain(5)
        
        assertNotNull(split.pre)
        assertNull(split.post)
        assertEquals("Hello", split.pre?.plainText)
        assertEquals(1, split.size)
    }

    @Test
    fun `WikiDocument split at boundary`() {
        val doc = WikitextParser.parse("Hello [[World|Earth]]!")
        val split = doc.splitPlain(6)
        
        assertNotNull(split.pre)
        assertNotNull(split.post)
        assertEquals("Hello ", split.pre?.plainText)
        assertEquals("Earth!", split.post?.plainText)
        assertEquals(2, split.size)
    }

    @Test
    fun `WikiDocument split inside TextNode`() {
        val doc = WikitextParser.parse("Hello [[World|Earth]]!")
        val split = doc.splitPlain(3)
        assertEquals("Hel", split.pre?.plainText)
        assertEquals("lo Earth!", split.post?.plainText)
    }

    @Test
    fun `WikiDocument split inside non-splittable`() {
        val doc = WikitextParser.parse("Hello [[World|Earth]]!")
        val split = doc.splitPlain(8) // inside "Earth"
        
        // Non-splittable completely goes to post
        assertEquals("Hello ", split.pre?.plainText)
        assertEquals("Earth!", split.post?.plainText)
    }

    @Test
    fun `WikiDocument split with string`() {
        val doc = WikitextParser.parse("Hello [[World|Earth]]!")
        val split = doc.splitPlain("Earth")
        assertEquals("Hello ", split.pre?.plainText)
        assertEquals("Earth!", split.post?.plainText)
    }

    @Test
    fun `WikiList split`() {
        val src = "* Item 1\n* Item 2"
        val doc = WikitextParser.parse(src)
        val list = doc.children.first() as WikiList
        
        // This splits by index on list.children plains.
        // item 1 is "Item 1" (len 6)
        // item 2 is "Item 2" (len 6)
        // Total plain is 12 (ignoring list plain bullets).
        val split = list.splitPlain(6)
        assertNotNull(split.pre)
        assertNotNull(split.post)
        assertEquals("• Item 1", split.pre?.plainText)
        assertEquals("• Item 2", split.post?.plainText)
    }
    
    @Test
    fun `WikiListItem split without sublist`() {
        val src = "* Item 123"
        val doc = WikitextParser.parse(src)
        val list = doc.children.first() as WikiList
        val item1 = list.items.first()
        
        val split = item1.splitPlain(6) // splices "Item 1" and "23"
        assertNotNull(split.pre)
        assertNotNull(split.post)
        assertEquals("Item 1", split.pre?.plainText)
        assertEquals("23", split.post?.plainText)
    }

}
