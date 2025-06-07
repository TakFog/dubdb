package takutility.dubdb.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

@Disabled
internal class WikidataTest {
    val wikidata: Wikidata = WikidataImpl()

    @Test
    fun findIdsByItWiki() {
        val downeySrc = "Robert_Downey_Jr."
        val maggiSrc = "Angelo_Maggi"
        val deadpoolSrc = "Deadpool_2"
        val reynoldsSrc = "Ryan_Reynolds"
        val patriarcaSrc = "Gabriele_Patriarca_(doppiatore)"
        val doctorSrc = "The_Good_Doctor_(serie_televisiva)"
        val downeyWD = "Q165219"
        val maggiWD = "Q3617056"
        val deadpoolWD = "Q25431158"
        val reynoldsWD = "Q192682"
        val patriarcaWD = "Q3756660"
        val doctorWD = "Q29908604"

        val result = wikidata.findIdsByItWiki(listOf(downeySrc, maggiSrc, deadpoolSrc, reynoldsSrc, patriarcaSrc, doctorSrc))

        assertEquals(downeyWD, result[downeySrc], downeySrc)
        assertEquals(maggiWD, result[maggiSrc], maggiSrc)
        assertEquals(deadpoolWD, result[deadpoolSrc], deadpoolSrc)
        assertEquals(reynoldsWD, result[reynoldsSrc], reynoldsSrc)
        assertEquals(patriarcaWD, result[patriarcaSrc], patriarcaSrc)
        assertEquals(doctorWD, result[doctorSrc], doctorSrc)
    }

    @Test
    fun findIdsByImdb() {
        val downeySrc = "nm0000375"
        val maggiSrc = "nm0535947"
        val deadpoolSrc = "tt5463162"
        val reynoldsSrc = "nm0005351"
        val patriarcaSrc = "nm0665775"
        val doctorSrc = "tt6470478"
        val randomSrc = "xy52123651"
        val downeyWD = "Q165219"
        val maggiWD = "Q3617056"
        val deadpoolWD = "Q25431158"
        val reynoldsWD = "Q192682"
        val patriarcaWD = "Q3756660"
        val doctorWD = "Q29908604"

        val result = wikidata.findIdsByImdb(listOf(downeySrc, maggiSrc, deadpoolSrc, reynoldsSrc, patriarcaSrc, doctorSrc, randomSrc))

        assertEquals(downeyWD, result[downeySrc], "downey")
        assertEquals(maggiWD, result[maggiSrc], "maggi")
        assertEquals(deadpoolWD, result[deadpoolSrc], "deadpool")
        assertEquals(reynoldsWD, result[reynoldsSrc], "reynolds")
        assertEquals(patriarcaWD, result[patriarcaSrc], "patriarca")
        assertEquals(doctorWD, result[doctorSrc], "doctor")
        assertFalse(randomSrc in result, "random")
    }
}