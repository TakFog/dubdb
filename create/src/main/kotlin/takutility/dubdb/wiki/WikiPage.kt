package takutility.dubdb.wiki

import com.fasterxml.jackson.annotation.JsonAlias
import org.jsoup.nodes.Document
import java.time.Instant

class WikiHtmlPage(val title: String, val doc: Document?) {

    fun exists() = doc != null
}

class WikiPage(
    val title: String,
    var content: String? = null,
    @field:JsonAlias("pageid") var id: Long? = null,
    @field:JsonAlias("touched") var lastEdit: Instant? = null,
) {
    fun exists() = id != null || content != null
}