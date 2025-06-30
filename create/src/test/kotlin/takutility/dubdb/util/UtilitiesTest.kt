package takutility.dubdb.util

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream


internal class UtilitiesTest {


    companion object {
        @JvmStatic
        fun splitWithBracketsParams(): Stream<Arguments> {
            return Stream.of(
                Arguments.of("Methuen Orphanage Girl (voice, uncredited)", listOf("Methuen Orphanage Girl (voice, uncredited)")),
                Arguments.of("Ultron (voice), Jarvis", listOf("Ultron (voice)", "Jarvis")),
                Arguments.of("Jarvis, Ultron (voice)", listOf("Jarvis", "Ultron (voice)")),
                Arguments.of("Jarvis, Ultron", listOf("Jarvis", "Ultron")),
                Arguments.of("Methuen, Orphanage Girl (voice, uncredited)", listOf("Methuen", "Orphanage Girl (voice, uncredited)")),
                Arguments.of("(hard, case)", listOf("(hard, case)")),
                Arguments.of(", start comma", listOf("start comma")),
                Arguments.of("Orphanage Girl (voice, uncredited), Methuen", listOf("Orphanage Girl (voice, uncredited)", "Methuen")),
                Arguments.of(" start with space", listOf(" start with space")),
            )
        }
    }

    @ParameterizedTest
    @MethodSource("splitWithBracketsParams")
    fun splitWithBrackets(input: String, expected: List<String>) {
        assertEquals(expected, input.splitWithBrackets().toList())
    }
}