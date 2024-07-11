package takutility.dubdb.wiki

import org.jsoup.nodes.Document

class WikiPage(val title: String) {
    val doc: Document? = WikiPageLoader.load(title)

    fun exists() = doc != null
}