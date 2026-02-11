package takutility.dubdb.wiki

import org.jsoup.nodes.Document
import java.time.Instant

class WikiHtmlPage(val title: String, val doc: Document?) {

    fun exists() = doc != null
}

interface WikiPage {
    val title: String
    val id: Long?
    val mainSection: WikiSection?
    val sections: Map<String, WikiSection>?
    val langLink: Map<String, String>?
    val sort: String?
    val image: String?
    val wikidata: String?
    val revisionId: Long?
    val lastRead: Instant?

    fun exists(): Boolean
}

interface WikiSection {
    val title: String
    val subsections: Map<String, WikiSection>
    val offset: Int
    val content: String?
}