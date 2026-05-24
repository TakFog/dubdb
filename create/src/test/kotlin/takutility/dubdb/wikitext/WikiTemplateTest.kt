package takutility.dubdb.wikitext

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertInstanceOf
import takutility.wikitext.WikiLink
import takutility.wikitext.WikiList
import takutility.wikitext.WikiTemplate

class WikiTemplateTest {

    // ── Name ──────────────────────────────────────────────────────────────

    @Test
    fun `template name is trimmed`() {
        val t = parseSingle("{{ My Template }}").assertIs<WikiTemplate>()
        assertEquals("My Template", t.name)
    }

    @Test
    fun `empty template no args`() {
        val t = parseSingle("{{Stub}}").assertIs<WikiTemplate>()
        assertEquals("Stub", t.name)
        Assertions.assertTrue(t.arguments.isEmpty())
    }

    // ── Positional arguments ──────────────────────────────────────────────

    @Test
    fun `single positional argument`() {
        val t = parseSingle("{{lang|fr}}").assertIs<WikiTemplate>()
        assertEquals(1, t.arguments.size)
        val arg = t.positional(1)!!
        Assertions.assertTrue(arg.isPositional)
        Assertions.assertFalse(arg.isNamed)
        Assertions.assertNull(arg.index.let { null }.also { assertEquals(1, arg.index) })
        assertEquals("fr", arg.value.plainText.trim())
    }

    @Test
    fun `multiple positional arguments numbered from 1`() {
        val t = parseSingle("{{IPA|en|/hɛ.lo/}}").assertIs<WikiTemplate>()
        assertEquals(2, t.arguments.size)
        assertEquals("en", t.positional(1)!!.value.plainText.trim())
        assertEquals("/hɛ.lo/", t.positional(2)!!.value.plainText.trim())
        Assertions.assertNull(t.positional(3))
    }

    @Test
    fun `positionalArgs list preserves order`() {
        val t = parseSingle("{{T|a|b|c}}").assertIs<WikiTemplate>()
        val values = t.positionalArgs.map { it.value.plainText.trim() }
        assertEquals(listOf("a", "b", "c"), values)
    }

    // ── Named arguments ───────────────────────────────────────────────────

    @Test
    fun `single named argument`() {
        val t = parseSingle("{{Cite|author=Smith}}").assertIs<WikiTemplate>()
        assertEquals(1, t.arguments.size)
        val arg = t.named("author")!!
        Assertions.assertTrue(arg.isNamed)
        Assertions.assertFalse(arg.isPositional)
        assertEquals("Smith", arg.value.plainText.trim())
    }

    @Test
    fun `named argument key is trimmed in lookup`() {
        val t = parseSingle("{{T| key = value }}").assertIs<WikiTemplate>()
        Assertions.assertNotNull(t.named("key"), "lookup by 'key' should succeed")
        Assertions.assertNotNull(t.named(" key "), "lookup with extra spaces should succeed")
    }

    @Test
    fun `namedArgs map contains all named arguments`() {
        val t = parseSingle("{{T|a=1|b=2|c=3}}").assertIs<WikiTemplate>()
        val map = t.namedArgs
        assertEquals(3, map.size)
        assertEquals("1", map["a"]!!.value.plainText.trim())
        assertEquals("2", map["b"]!!.value.plainText.trim())
        assertEquals("3", map["c"]!!.value.plainText.trim())
    }

    @Test
    fun `mixed positional and named arguments`() {
        val t = parseSingle("{{Cite|Smith|year=2020|title=Book}}").assertIs<WikiTemplate>()
        assertEquals(3, t.arguments.size)
        Assertions.assertNotNull(t.positional(1))
        Assertions.assertNotNull(t.named("year"))
        Assertions.assertNotNull(t.named("title"))
        Assertions.assertNull(t.named("author"))
    }

    // ── Nested content in argument values ────────────────────────────────

    @Test
    fun `nested template inside positional arg is a WikiTemplate node`() {
        val t = parseSingle("{{Outer|{{Inner|x}}}}").assertIs<WikiTemplate>()
        val arg = t.positional(1)!!
        val inner = arg.value.children.filterIsInstance<WikiTemplate>()
        assertEquals(1, inner.size)
        assertEquals("Inner", inner.first().name)
    }

    @Test
    fun `wikilink inside named arg value is a WikiLink node`() {
        val t = parseSingle("{{T|birthplace=[[Rome]]}}").assertIs<WikiTemplate>()
        val arg = t.named("birthplace")!!
        val links = arg.value.walk().filterIsInstance<WikiLink>().toList()
        assertEquals(1, links.size)
        assertEquals("Rome", links.first().target)
    }

    @Test
    fun `pipe inside nested template does not split outer arg`() {
        val t = parseSingle("{{Outer|{{Inner|a|b}}|second}}").assertIs<WikiTemplate>()
        // Outer should have 2 args: the nested template and "second"
        assertEquals(2, t.arguments.size)
        val first = t.positional(1)!!
        val inner = first.value.children.filterIsInstance<WikiTemplate>()
        assertEquals(1, inner.size)
        assertEquals("Inner", inner.first().name)
        assertEquals(2, inner.first().arguments.size)
    }

    @Test
    fun `pipe inside wikilink does not split outer template arg`() {
        val t = parseSingle("{{T|[[Page|Label]]|second}}").assertIs<WikiTemplate>()
        assertEquals(2, t.arguments.size)
        val linkNode = t.positional(1)!!.value.children.filterIsInstance<WikiLink>()
        assertEquals(1, linkNode.size)
    }

    // ── rawText and plainText ─────────────────────────────────────────────

    @Test
    fun `template rawText includes braces`() {
        val src = "{{Foo|bar}}"
        val t = parseSingle(src).assertIs<WikiTemplate>()
        t.assertRaw(src)
        assertEquals(src, t.toString())
    }

    @Test
    fun `template plainText is bracketed name`() {
        val t = parseSingle("{{Citation needed}}").assertIs<WikiTemplate>()
        t.assertPlain("[Citation needed]")
    }

    @Test
    fun `TemplateArgument rawText is the raw pipe segment`() {
        val t = parseSingle("{{T|key = value}}").assertIs<WikiTemplate>()
        val arg = t.named("key")!!
        assertEquals("key = value", arg.rawText)
        assertEquals(arg.rawText, arg.toString())
    }

    // ── children ──────────────────────────────────────────────────────────

    @Test
    fun `template children are its arguments`() {
        val t = parseSingle("{{T|a|b=c}}").assertIs<WikiTemplate>()
        assertEquals(t.arguments, t.children)
    }

    @Test
    fun `TemplateArgument children include both name and value nodes`() {
        val t = parseSingle("{{T|[[K]]=[[V]]}}").assertIs<WikiTemplate>()
        val arg = t.arguments.first()
        val links = arg.children.filterIsInstance<WikiLink>()
        assertEquals(2, links.size)
    }

    @Test
    fun `list as template argument`() {
        val t = parseSingle("{{T|arglist=* a\n* b\n* c}}").assertIs<WikiTemplate>()
        val listArg = t.named("arglist")
        assertNotNull(listArg)
        assertInstanceOf<WikiList>(listArg!!.value.children[0])
        assertEquals(3, listArg.value.children[0].children.size)
    }

    @Test
    fun `list with spaces as template argument`() {
        val t = parseSingle("{{T|arglist = * a\n* b\n* c}}").assertIs<WikiTemplate>()
        val listArg = t.named("arglist")
        assertNotNull(listArg)
        assertInstanceOf<WikiList>(listArg!!.value.children[0])
        assertEquals(3, listArg.value.children[0].children.size)
    }
}