package takutility.dubdb.wikitext

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import takutility.wikitext.*

class WikiListTest {

    // ── Flat unordered ────────────────────────────────────────────────────

    @Test
    fun `flat unordered list has correct type and item count`() {
        val list = parseSingle("* Alpha\n* Beta\n* Gamma").assertIs<WikiList>()
        Assertions.assertEquals(ListType.Unordered, list.listType)
        Assertions.assertEquals(3, list.items.size)
    }

    @Test
    fun `unordered items have star marker and depth 1`() {
        val list = parseSingle("* A\n* B").assertIs<WikiList>()
        for (item in list.items) {
            Assertions.assertEquals('*', item.marker)
            Assertions.assertEquals(1, item.depth)
            Assertions.assertTrue(item.isUnordered)
            Assertions.assertFalse(item.isOrdered)
        }
    }

    @Test
    fun `unordered item content is parsed`() {
        val list = parseSingle("* [[Rome|Roma]]").assertIs<WikiList>()
        val link = list.items.first().content.children.filterIsInstance<WikiLink>()
        Assertions.assertEquals(1, link.size)
        Assertions.assertEquals("Rome", link.first().target)
    }

    // ── Flat ordered ──────────────────────────────────────────────────────

    @Test
    fun `flat ordered list has correct type`() {
        val list = parseSingle("# One\n# Two\n# Three").assertIs<WikiList>()
        Assertions.assertEquals(ListType.Ordered, list.listType)
        Assertions.assertEquals(3, list.items.size)
    }

    @Test
    fun `ordered items have hash marker`() {
        val list = parseSingle("# A").assertIs<WikiList>()
        Assertions.assertEquals('#', list.items.first().marker)
        Assertions.assertTrue(list.items.first().isOrdered)
    }

    // ── Definition list ───────────────────────────────────────────────────

    @Test
    fun `definition list has correct type`() {
        val list = parseSingle("; Term\n: Description").assertIs<WikiList>()
        Assertions.assertEquals(ListType.Definition, list.listType)
        Assertions.assertEquals(2, list.items.size)
    }

    @Test
    fun `definition term marker is semicolon`() {
        val list = parseSingle("; Word").assertIs<WikiList>()
        val item = list.items.first()
        Assertions.assertEquals(';', item.marker)
        Assertions.assertTrue(item.isTerm)
    }

    @Test
    fun `definition description marker is colon`() {
        val list = parseSingle(": Description").assertIs<WikiList>()
        val item = list.items.first()
        Assertions.assertEquals(':', item.marker)
        Assertions.assertTrue(item.isDescription)
    }

    // ── Mixed ─────────────────────────────────────────────────────────────

    @Test
    fun `mixed marker list has Mixed type`() {
        val list = parseSingle("* Bullet\n# Number").assertIs<WikiList>()
        Assertions.assertEquals(ListType.Mixed, list.listType)
    }

    // ── Nesting ───────────────────────────────────────────────────────────

    @Test
    fun `nested unordered list becomes subList`() {
        val src = "* Parent\n** Child 1\n** Child 2\n* Next parent"
        val list = parseSingle(src).assertIs<WikiList>()
        Assertions.assertEquals(2, list.items.size, "two top-level items")
        val parent = list.items.first()
        Assertions.assertNotNull(parent.subList)
        Assertions.assertEquals(2, parent.subList!!.items.size)
    }

    @Test
    fun `nested items have correct depth`() {
        val src = "* depth1\n** depth2\n*** depth3"
        val list = parseSingle(src).assertIs<WikiList>()
        val depth1Item = list.items.first()
        Assertions.assertEquals(1, depth1Item.depth)
        val depth2Item = depth1Item.subList!!.items.first()
        Assertions.assertEquals(2, depth2Item.depth)
        val depth3Item = depth2Item.subList!!.items.first()
        Assertions.assertEquals(3, depth3Item.depth)
    }

    @Test
    fun `ordered sub-list under unordered parent`() {
        val src = "* Bullet\n## Sub-ordered"
        val list = parseSingle(src).assertIs<WikiList>()
        val sub = list.items.first().subList!!
        Assertions.assertEquals('#', sub.items.first().marker)
    }

    @Test
    fun `depth skip is handled gracefully`() {
        // Going straight from depth 1 to depth 3 without depth 2
        val src = "* top\n*** deep"
        val list = parseSingle(src).assertIs<WikiList>()
        Assertions.assertEquals(1, list.items.size)
        val sub = list.items.first().subList!!
        Assertions.assertEquals(1, sub.items.size)
        Assertions.assertEquals(3, sub.items.first().depth)
    }

    // ── Item content ──────────────────────────────────────────────────────

    @Test
    fun `item content may contain a template`() {
        val list = parseSingle("* {{em|important}} text").assertIs<WikiList>()
        val templates = list.items.first().content.walk()
            .filterIsInstance<WikiTemplate>().toList()
        Assertions.assertEquals(1, templates.size)
        Assertions.assertEquals("em", templates.first().name)
    }

    @Test
    fun `item content may contain an external link`() {
        val list = parseSingle("* See [https://example.com here]").assertIs<WikiList>()
        val links = list.items.first().content.walk()
            .filterIsInstance<ExternalLink>().toList()
        Assertions.assertEquals(1, links.size)
    }

    // ── rawText and plainText ─────────────────────────────────────────────

    @Test
    fun `list rawText reconstructs source lines`() {
        val src = "* A\n* B\n* C"
        val list = parseSingle(src).assertIs<WikiList>()
        list.assertRaw(src)
        Assertions.assertEquals(src, list.toString())
    }

    @Test
    fun `list item rawText is its source line`() {
        val list = parseSingle("* Hello").assertIs<WikiList>()
        Assertions.assertEquals("* Hello", list.items.first().rawText)
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