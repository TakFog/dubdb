package takutility.dubdb.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import takutility.dubdb.entities.Source
import takutility.dubdb.entities.SourceIds

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

    @Test
    fun findIds() {
        val downeyWD = "Q165219"
        val maggiWD = "Q3617056"
        val deadpoolWD = "Q25431158"
        val reynoldsWD = "Q192682"
        val patriarcaWD = "Q3756660"
        val doctorWD = "Q29908604"
        val downeyIds = ids("Robert_Downey_Jr.", "Robert_Downey_Jr.", "nm0000375")
        val maggiIds = ids("Angelo_Maggi", "Angelo_Maggi", "nm0535947", "doppiaggio/voci/vociamag")
        val deadpoolIds = ids("Deadpool_2", "Deadpool_2", "tt5463162", "doppiaggio/film1/deadpool2")
        val reynoldsIds = ids("Ryan_Reynolds", "Ryan_Reynolds", "nm0005351")
        val patriarcaIds = ids("Gabriele_Patriarca_(doppiatore)", null, "nm0665775", "doppiaggio/voci/vocigpat")
        val doctorIds = ids("The_Good_Doctor_(serie_televisiva)", "The_Good_Doctor_(American_TV_series)", "tt6470478", "doppiaggio/telefilm/thegooddoctor")

        val result = wikidata.findIds(listOf(downeyWD, maggiWD, deadpoolWD, reynoldsWD, patriarcaWD, doctorWD))

        assertEquals(downeyIds, result[downeyWD], "downey")
        assertEquals(maggiIds, result[maggiWD], "maggi")
        assertEquals(deadpoolIds, result[deadpoolWD], "deadpool")
        assertEquals(reynoldsIds, result[reynoldsWD], "reynolds")
        assertEquals(patriarcaIds, result[patriarcaWD], "patriarca")
        assertEquals(doctorIds, result[doctorWD], "doctor")
    }

    private fun ids(itWiki: String? = null, enWiki: String? = null, imdb: String? = null, mondoDoppiatori: String? = null): SourceIds {
        val ids = SourceIds()
        itWiki?.let { ids[Source.WIKI] = it }
        enWiki?.let { ids[Source.WIKI_EN] = it }
        imdb?.let { ids[Source.IMDB] = it }
        mondoDoppiatori?.let { ids[Source.MONDO_DOPPIATORI] = it }
        return ids
    }
}