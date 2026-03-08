package takutility.dubdb.wikitext

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import takutility.wikitext.WikiLink
import takutility.wikitext.WikiTable
import takutility.wikitext.WikiTemplate

class WikiTableTest {

    private fun simpleTable() = """
        {| class="wikitable"
        |-
        | Cell A || Cell B
        |-
        | Cell C || Cell D
        |}
    """.trimIndent()

    @Test
    fun `table is parsed as WikiTable node`() {
        parseSingle(simpleTable()).assertIs<WikiTable>()
    }

    @Test
    fun `table attributes are extracted`() {
        val table = parseSingle(simpleTable()).assertIs<WikiTable>()
        Assertions.assertEquals("class=\"wikitable\"", table.attributes)
    }

    @Test
    fun `table has correct number of rows`() {
        val table = parseSingle(simpleTable()).assertIs<WikiTable>()
        Assertions.assertEquals(2, table.rows.size)
    }

    @Test
    fun `each row has correct number of cells`() {
        val table = parseSingle(simpleTable()).assertIs<WikiTable>()
        Assertions.assertEquals(2, table.rows[0].cells.size)
        Assertions.assertEquals(2, table.rows[1].cells.size)
    }

    @Test
    fun `data cells are not headers`() {
        val table = parseSingle(simpleTable()).assertIs<WikiTable>()
        for (row in table.rows) for (cell in row.cells) Assertions.assertFalse(cell.isHeader)
    }

    @Test
    fun `header cells are marked as headers`() {
        val src = """
            {|
            |-
            ! Name !! Age
            |}
        """.trimIndent()
        val table = parseSingle(src).assertIs<WikiTable>()
        for (cell in table.rows.first().cells) Assertions.assertTrue(cell.isHeader)
    }

    @Test
    fun `table caption is parsed`() {
        val src = """
            {|
            |+ My Caption
            |-
            | Cell
            |}
        """.trimIndent()
        val table = parseSingle(src).assertIs<WikiTable>()
        Assertions.assertNotNull(table.caption)
        Assertions.assertEquals("My Caption", table.caption!!.plainText.trim())
    }

    @Test
    fun `caption without attributes`() {
        val src = "{|\n|+ Simple caption\n|-\n| x\n|}"
        val table = parseSingle(src).assertIs<WikiTable>()
        Assertions.assertEquals("", table.caption!!.attributesRaw)
        Assertions.assertEquals("Simple caption", table.caption!!.plainText.trim())
    }

    @Test
    fun `cell content is recursively parsed — link in cell`() {
        val src = "{|\n|-\n| [[Rome|Roma]]\n|}"
        val table = parseSingle(src).assertIs<WikiTable>()
        val links = table.walk().filterIsInstance<WikiLink>().toList()
        Assertions.assertEquals(1, links.size)
        Assertions.assertEquals("Rome", links.first().target)
    }

    @Test
    fun `cell content is recursively parsed — template in cell`() {
        val src = "{|\n|-\n| {{flag|Italy}}\n|}"
        val table = parseSingle(src).assertIs<WikiTable>()
        val templates = table.walk().filterIsInstance<WikiTemplate>().toList()
        Assertions.assertEquals(1, templates.size)
        Assertions.assertEquals("flag", templates.first().name)
    }

    @Test
    fun `table rawText reconstructs source`() {
        val src = "{|\n|-\n| Hello\n|}"
        val table = parseSingle(src).assertIs<WikiTable>()
        table.assertRaw(src)
    }

    @Test
    fun `table plainText contains cell text`() {
        val src = "{|\n|-\n| Alpha\n| Beta\n|}"
        val table = parseSingle(src).assertIs<WikiTable>()
        val plain = table.plainText
        Assertions.assertTrue(plain.contains("Alpha"))
        Assertions.assertTrue(plain.contains("Beta"))
    }

    @Test
    fun `table no caption has null caption`() {
        val table = parseSingle(simpleTable()).assertIs<WikiTable>()
        Assertions.assertNull(table.caption)
    }
}