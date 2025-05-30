package takutility.dubdb.service

interface Wikidata {

    fun findIdsByItWiki(itWiki: Collection<String> = listOf()): Map<String, String>
    fun findIdsByImdb(imdb: Collection<String> = listOf()): Map<String, String>
}