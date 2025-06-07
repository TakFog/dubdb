package takutility.dubdb.service

import com.bordercloud.sparql.SparqlClient
import java.net.URI

interface Wikidata {

    fun findIdsByItWiki(itWiki: Collection<String> = listOf()): Map<String, String>
    fun findIdsByImdb(imdb: Collection<String> = listOf()): Map<String, String>
}

class WikidataImpl : Wikidata {
    private val sc: SparqlClient = SparqlClient(false)

    init {
        sc.endpointRead = URI("https://query.wikidata.org/sparql")
    }

    override fun findIdsByItWiki(itWiki: Collection<String>): Map<String, String> {
        val list = itWiki.joinToString(separator = " ") { "<https://it.wikipedia.org/wiki/$it>" }
        return findIdsByQuery("VALUES ?input { $list } ?input schema:about ?item .") {
            it.toString().replace("https://it.wikipedia.org/wiki/", "")
        }
    }

    override fun findIdsByImdb(imdb: Collection<String>): Map<String, String> {
        val list = imdb.joinToString(separator = " ") {"\"$it\""}
        return findIdsByQuery("VALUES ?input { $list } ?item wdt:P345 ?input .")
    }

    private fun findIdsByQuery(subquery: String, cleanupInput: (Any) -> String = { it.toString() }): Map<String, String> {
        val query = "SELECT ?input ?item WHERE { $subquery }"
        val sr = sc.query(query)

        return sr.model.rows.associate {
            val wdid = it["item"].toString().replace("http://www.wikidata.org/entity/", "")
            cleanupInput(it["input"]!!) to wdid
        }
    }
}