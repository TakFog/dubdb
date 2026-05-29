package takutility.dubdb.wikitext

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import takutility.wikitext.*

class WikiListTest {

    // ── Flat unordered ────────────────────────────────────────────────────

    @Test
    fun `flat unordered list has correct type and item count`() {
        val list = parseSingle("* Alpha\n* Beta\n* Gamma").assertIs<WikiList>()
        assertEquals(ListType.Unordered, list.listType)
        assertEquals(3, list.items.size)
    }

    @Test
    fun `unordered item trim spaces`() {
        val list = parseSingle("* Item 123 ").assertIs<WikiList>()
        val item1 = list.items.first()
        assertEquals("* Item 123 ", item1.rawText)
        assertEquals("Item 123", item1.plainText)
        assertEquals("Item 123", item1.content.plainText)
    }

    @Test
    fun `unordered items have star marker and depth 1`() {
        val list = parseSingle("* A\n* B").assertIs<WikiList>()
        for (item in list.items) {
            assertEquals('*', item.marker)
            assertEquals(1, item.depth)
            Assertions.assertTrue(item.isUnordered)
            Assertions.assertFalse(item.isOrdered)
        }
    }

    @Test
    fun `unordered item content is parsed`() {
        val list = parseSingle("* [[Rome|Roma]]").assertIs<WikiList>()
        val link = list.items.first().content.children.filterIsInstance<WikiLink>()
        assertEquals(1, link.size)
        assertEquals("Rome", link.first().target)
    }

    // ── Flat ordered ──────────────────────────────────────────────────────

    @Test
    fun `flat ordered list has correct type`() {
        val list = parseSingle("# One\n# Two\n# Three").assertIs<WikiList>()
        assertEquals(ListType.Ordered, list.listType)
        assertEquals(3, list.items.size)
    }

    @Test
    fun `ordered items have hash marker`() {
        val list = parseSingle("# A").assertIs<WikiList>()
        assertEquals('#', list.items.first().marker)
        Assertions.assertTrue(list.items.first().isOrdered)
    }

    // ── Definition list ───────────────────────────────────────────────────

    @Test
    fun `definition list has correct type`() {
        val list = parseSingle("; Term\n: Description").assertIs<WikiList>()
        assertEquals(ListType.Definition, list.listType)
        assertEquals(2, list.items.size)
    }

    @Test
    fun `definition term marker is semicolon`() {
        val list = parseSingle("; Word").assertIs<WikiList>()
        val item = list.items.first()
        assertEquals(';', item.marker)
        Assertions.assertTrue(item.isTerm)
    }

    @Test
    fun `definition description marker is colon`() {
        val list = parseSingle(": Description").assertIs<WikiList>()
        val item = list.items.first()
        assertEquals(':', item.marker)
        Assertions.assertTrue(item.isDescription)
    }

    // ── Mixed ─────────────────────────────────────────────────────────────

    @Test
    fun `mixed marker list has Mixed type`() {
        val list = parseSingle("* Bullet\n# Number").assertIs<WikiList>()
        assertEquals(ListType.Mixed, list.listType)
    }

    // ── Nesting ───────────────────────────────────────────────────────────

    @Test
    fun `nested unordered list becomes subList`() {
        val src = "* Parent\n** Child 1\n** Child 2\n* Next parent"
        val list = parseSingle(src).assertIs<WikiList>()
        assertEquals(2, list.items.size, "two top-level items")
        val parent = list.items.first()
        Assertions.assertNotNull(parent.subList)
        assertEquals(2, parent.subList!!.items.size)
    }

    @Test
    fun `nested items have correct depth`() {
        val src = "* depth1\n** depth2\n*** depth3"
        val list = parseSingle(src).assertIs<WikiList>()
        val depth1Item = list.items.first()
        assertEquals(1, depth1Item.depth)
        val depth2Item = depth1Item.subList!!.items.first()
        assertEquals(2, depth2Item.depth)
        val depth3Item = depth2Item.subList!!.items.first()
        assertEquals(3, depth3Item.depth)
    }

    @Test
    fun `ordered sub-list under unordered parent`() {
        val src = "* Bullet\n## Sub-ordered"
        val list = parseSingle(src).assertIs<WikiList>()
        val sub = list.items.first().subList!!
        assertEquals('#', sub.items.first().marker)
    }

    @Test
    fun `depth skip is handled gracefully`() {
        // Going straight from depth 1 to depth 3 without depth 2
        val src = "* top\n*** deep"
        val list = parseSingle(src).assertIs<WikiList>()
        assertEquals(1, list.items.size)
        val sub = list.items.first().subList!!
        assertEquals(1, sub.items.size)
        assertEquals(3, sub.items.first().depth)
    }

    // ── Item content ──────────────────────────────────────────────────────

    @Test
    fun `item content may contain a template`() {
        val list = parseSingle("* {{em|important}} text").assertIs<WikiList>()
        val templates = list.items.first().content.walk()
            .filterIsInstance<WikiTemplate>().toList()
        assertEquals(1, templates.size)
        assertEquals("em", templates.first().name)
    }

    @Test
    fun `item content may contain an external link`() {
        val list = parseSingle("* See [https://example.com here]").assertIs<WikiList>()
        val links = list.items.first().content.walk()
            .filterIsInstance<ExternalLink>().toList()
        assertEquals(1, links.size)
    }

    // ── rawText and plainText ─────────────────────────────────────────────

    @Test
    fun `list rawText reconstructs source lines`() {
        val src = "* A\n* B\n* C"
        val list = parseSingle(src).assertIs<WikiList>()
        list.assertRaw(src)
        assertEquals(src, list.toString())
    }

    @Test
    fun `list item rawText is its source line`() {
        val list = parseSingle("* Hello").assertIs<WikiList>()
        assertEquals("* Hello", list.items.first().rawText)
    }

    @Test
    fun `unordered plainText uses bullet prefix`() {
        val list = parseSingle("* Alpha\n* Beta").assertIs<WikiList>()
        val plain = list.plainText
        Assertions.assertTrue(plain.contains("• Alpha"), "expected bullet before 'Alpha'")
        Assertions.assertTrue(plain.contains("• Beta"), "expected bullet before 'Beta'")
    }

    @Test
    fun `ordered plainText uses numeric prefix`() {
        val list = parseSingle("# First\n# Second").assertIs<WikiList>()
        val plain = list.plainText
        Assertions.assertTrue(plain.contains("1. First"))
        Assertions.assertTrue(plain.contains("2. Second"))
    }

    @Test
    fun `definition plainText uses colon after term`() {
        val list = parseSingle("; Word").assertIs<WikiList>()
        Assertions.assertTrue(list.plainText.contains("Word:"))
    }

    // ── Not triggered mid-sentence ────────────────────────────────────────

    @Test
    fun `star mid-sentence is NOT parsed as list`() {
        val doc = WikitextParser.parse("2 * 3 = 6")
        Assertions.assertTrue(
            doc.walk().filterIsInstance<WikiList>().none(),
            "Mid-sentence * must not trigger list parsing"
        )
    }

    @Test
    fun `hash mid-sentence is NOT parsed as list`() {
        val doc = WikitextParser.parse("item #2 in a row")
        Assertions.assertTrue(doc.walk().filterIsInstance<WikiList>().none())
    }
}