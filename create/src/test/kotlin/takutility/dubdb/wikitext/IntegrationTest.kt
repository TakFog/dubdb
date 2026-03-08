package takutility.dubdb.wikitext

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import takutility.wikitext.*

//  Complex / integration scenarios
class IntegrationTest {

    @Test
    fun `infobox template parses all fields`() {
        val src = """
            {{Infobox person
            | name       = Jane Doe
            | birth_date = {{Birth date|1990|4|15}}
            | birth_place = [[New York City]], [[United States]]
            | website    = [https://example.com Jane's site]
            }}
        """.trimIndent()
        val doc = WikitextParser.parse(src)
        val infobox = doc.topLevelTemplates().first()
        Assertions.assertEquals("Infobox person", infobox.name)

        // named arg 'name'
        Assertions.assertEquals("Jane Doe", infobox.named("name")!!.value.plainText.trim())

        // birth_date contains a nested template
        val bdArg = infobox.named("birth_date")!!
        val bdTemplate = bdArg.value.walk().filterIsInstance<WikiTemplate>().first()
        Assertions.assertEquals("Birth date", bdTemplate.name)
        Assertions.assertEquals("1990", bdTemplate.positional(1)!!.value.plainText.trim())

        // birth_place contains two wiki links
        val bpArg = infobox.named("birth_place")!!
        val links = bpArg.value.walk().filterIsInstance<WikiLink>().toList()
        Assertions.assertEquals(2, links.size)
        Assertions.assertEquals("New York City", links[0].target)

        // website contains an external link
        val wsArg = infobox.named("website")!!
        val extLink = wsArg.value.walk().filterIsInstance<ExternalLink>().first()
        Assertions.assertEquals("https://example.com", extLink.url)
    }

    @Test
    fun `nested lists with template content`() {
        val src = "* {{T|a}}\n** [[Page|B]]\n* plain"
        val list = parseSingle(src).assertIs<WikiList>()
        Assertions.assertEquals(2, list.items.size)
        // first item has template content
        val firstTemplates = list.items[0].content.walk()
            .filterIsInstance<WikiTemplate>().toList()
        Assertions.assertEquals(1, firstTemplates.size)
        // first item has nested sub-list containing a link
        val subLinks = list.items[0].subList!!.items[0].content.walk()
            .filterIsInstance<WikiLink>().toList()
        Assertions.assertEquals(1, subLinks.size)
        Assertions.assertEquals("Page", subLinks[0].target)
    }

    @Test
    fun `table with links and templates in cells`() {
        val src = """
            {|
            |-
            | [[Rome]] || {{flag|Italy}}
            |-
            | [https://example.com Ex] || plain
            |}
        """.trimIndent()
        val table = parseSingle(src).assertIs<WikiTable>()
        val allLinks = table.walk().filterIsInstance<WikiLink>().toList()
        val allTmpl  = table.walk().filterIsInstance<WikiTemplate>().toList()
        val allExt   = table.walk().filterIsInstance<ExternalLink>().toList()
        Assertions.assertEquals(1, allLinks.size)
        Assertions.assertEquals(1, allTmpl.size)
        Assertions.assertEquals(1, allExt.size)
    }

    @Test
    fun `document walk finds every node type`() {
        val src = "Text {{T|[[L]]}} [https://x.com X]\n* item\n{|\n|-\n|cell\n|}"
        val doc = WikitextParser.parse(src)
        val types = doc.walk().map { it::class.simpleName }.toSet()
        for (expected in listOf(
            "WikiDocument", "TextNode", "WikiTemplate",
            "TemplateArgument", "WikiLink", "ExternalLink",
            "WikiList", "WikiListItem", "WikiTable", "TableRow", "TableCell"
        )) Assertions.assertTrue(expected in types, "Missing node type: $expected")
    }

    @Test
    fun `template with equals sign inside nested template does not split name`() {
        // The `=` inside {{inner|k=v}} must not be the named-arg `=` for outer
        val t = parseSingle("{{Outer|{{inner|k=v}}}}").assertIs<WikiTemplate>()
        Assertions.assertEquals(1, t.arguments.size)
        Assertions.assertTrue(t.arguments.first().isPositional)
    }

    @Test
    fun `deeply nested templates all appear in allTemplates`() {
        val src = "{{A|{{B|{{C|{{D}}}}}}}}"
        val doc = WikitextParser.parse(src)
        val names = doc.allTemplates().map { it.name }
        Assertions.assertEquals(listOf("A", "B", "C", "D"), names)
    }
}