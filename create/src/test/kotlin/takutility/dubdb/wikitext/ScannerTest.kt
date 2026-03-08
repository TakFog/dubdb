package takutility.dubdb.wikitext

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import takutility.wikitext.findFirstTopLevelEquals
import takutility.wikitext.findMatchingClose
import takutility.wikitext.splitOnTopLevelPipes

class ScannerTest {

    // ── findMatchingClose ──────────────────────────────────────────────────

    @Test
    fun `findMatchingClose returns end of simple pair`() {
        // "{{foo}}" → open at 0, close end at 7
        Assertions.assertEquals(7, findMatchingClose("{{foo}}", 0, "{{", "}}"))
    }

    @Test
    fun `findMatchingClose handles nesting`() {
        // "{{a{{b}}c}}" depth 2
        Assertions.assertEquals(11, findMatchingClose("{{a{{b}}c}}", 0, "{{", "}}"))
    }

    @Test
    fun `findMatchingClose handles link nesting`() {
        Assertions.assertEquals(42, findMatchingClose("[https://example.com See [[Article|here]]]", 0, "[", "]"))
    }

    @Test
    fun `findMatchingClose handles nested link`() {
        Assertions.assertEquals(41, findMatchingClose("[https://example.com See [[Article|here]]]", 25, "[[", "]]"))
    }

    @Test
    fun `findMatchingClose returns -1 when unmatched`() {
        Assertions.assertEquals(-1, findMatchingClose("{{unclosed", 0, "{{", "}}"))
    }

    @Test
    fun `findMatchingClose works for wikilinks`() {
        Assertions.assertEquals(13, findMatchingClose("[[Some page]]", 0, "[[", "]]"))
    }

    @Test
    fun `findMatchingClose works mid-string`() {
        val text = "prefix{{inner}}suffix"
        Assertions.assertEquals(15, findMatchingClose(text, 6, "{{", "}}"))
    }

    // ── splitOnTopLevelPipes ───────────────────────────────────────────────

    @Test
    fun `splitOnTopLevelPipes splits simple string`() {
        val parts = splitOnTopLevelPipes("Name|arg1|arg2")
        Assertions.assertEquals(listOf("Name", "arg1", "arg2"), parts)
    }

    @Test
    fun `splitOnTopLevelPipes does not split inside nested template`() {
        val parts = splitOnTopLevelPipes("Name|{{inner|x|y}}|last")
        Assertions.assertEquals(3, parts.size)
        Assertions.assertEquals("{{inner|x|y}}", parts[1])
    }

    @Test
    fun `splitOnTopLevelPipes does not split inside wikilink`() {
        val parts = splitOnTopLevelPipes("Name|[[Page|display]]|after")
        Assertions.assertEquals(3, parts.size)
        Assertions.assertEquals("[[Page|display]]", parts[1])
    }

    @Test
    fun `splitOnTopLevelPipes does not split inside external link`() {
        val parts = splitOnTopLevelPipes("Name|[https://example.com Label|extra]|after")
        Assertions.assertEquals(3, parts.size)
    }

    @Test
    fun `splitOnTopLevelPipes handles no pipes`() {
        val parts = splitOnTopLevelPipes("NoPipes")
        Assertions.assertEquals(listOf("NoPipes"), parts)
    }

    @Test
    fun `splitOnTopLevelPipes handles leading pipe`() {
        val parts = splitOnTopLevelPipes("|a|b")
        Assertions.assertEquals(listOf("", "a", "b"), parts)
    }

    // ── findFirstTopLevelEquals ────────────────────────────────────────────

    @Test
    fun `findFirstTopLevelEquals finds simple equals`() {
        Assertions.assertEquals(3, findFirstTopLevelEquals("key=value"))
    }

    @Test
    fun `findFirstTopLevelEquals skips equals inside template`() {
        val text = "key={{tmpl|x=y}}|rest"
        // first top-level '=' is at index 3 (after "key")
        Assertions.assertEquals(3, findFirstTopLevelEquals(text))
    }

    @Test
    fun `findFirstTopLevelEquals skips equals inside wikilink`() {
        val text = "[[Page|a=b]]"
        Assertions.assertEquals(-1, findFirstTopLevelEquals(text))
    }

    @Test
    fun `findFirstTopLevelEquals returns -1 when none`() {
        Assertions.assertEquals(-1, findFirstTopLevelEquals("noequals"))
    }
}