package takutility.dubdb.service

import com.bordercloud.sparql.SparqlClient
import takutility.dubdb.entities.Source
import takutility.dubdb.entities.SourceIds
import java.net.URI

interface Wikidata {

    fun findIdsByItWiki(itWiki: Set<String>): Map<String, String>
    fun findIdsByItWiki(itWiki: Collection<String>): Map<String, String> = findIdsByItWiki(itWiki.toSet())
    fun findIdsByImdb(imdb: Set<String>): Map<String, String>
    fun findIdsByImdb(imdb: Collection<String>): Map<String, String> = findIdsByImdb(imdb.toSet())
    fun findIds(wdids: Set<String>): Map<String, SourceIds>
    fun findIds(wdids: Collection<String>): Map<String, SourceIds> = findIds(wdids.toSet())
}

class WikidataImpl : Wikidata {
    private val sc: SparqlClient = SparqlClient(false)

    init {
        sc.endpointRead = URI("https://query.wikidata.org/sparql")
    }

    override fun findIdsByItWiki(itWiki: Set<String>): Map<String, String> {
        val list = itWiki.joinToString(separator = " ") { "<https://it.wikipedia.org/wiki/$it>" }
        return findIdsByQuery("VALUES ?input { $list } ?input schema:about ?item .") {
            it.toString().replace("https://it.wikipedia.org/wiki/", "")
        }
    }

    override fun findIdsByImdb(imdb: Set<String>): Map<String, String> {
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

    override fun findIds(wdids: Set<String>): Map<String, SourceIds> {
        val query = """SELECT ?item ?itWiki ?enWiki ?imdb ?mondoDoppiatori WHERE {
              VALUES ?item {${wdids.joinToString("") { "\n                wd:$it" }}
              }
              OPTIONAL {
                ?itArticle schema:about ?item ;
                           schema:isPartOf <https://it.wikipedia.org/> ;
                           schema:name ?itWiki .
              }
              OPTIONAL {
                ?enArticle schema:about ?item ;
                           schema:isPartOf <https://en.wikipedia.org/> ;
                           schema:name ?enWiki .
              }
              OPTIONAL { ?item wdt:P345 ?imdb. }
              OPTIONAL { ?item wdt:P5099 ?mondoDoppiatori. }
            }           
        """.trimIndent()
        val sr = sc.query(query)

        return sr.model.rows.associate { row ->
            val wdid = row["item"].toString().replace("http://www.wikidata.org/entity/", "")
            val ids = SourceIds()
            row["itWiki"]?.let { ids[Source.WIKI] = it.toString().replace(" ", "_") }
            row["enWiki"]?.let { ids[Source.WIKI_EN] = it.toString().replace(" ", "_") }
            row["imdb"]?.let { ids[Source.IMDB] = it.toString() }
            row["mondoDoppiatori"]?.let { ids[Source.MONDO_DOPPIATORI] = "doppiaggio/$it" }
            wdid to ids
        }
    }
}