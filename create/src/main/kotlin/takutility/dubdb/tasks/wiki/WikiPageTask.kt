package takutility.dubdb.tasks.wiki

import org.jsoup.nodes.Document
import takutility.dubdb.entities.Source
import takutility.dubdb.entities.SourceId
import takutility.dubdb.wiki.WikiPageLoader

open class WikiPageTask(val loader: WikiPageLoader) {

    protected fun load(wikiSource: SourceId?): Document? {
        if (wikiSource == null || wikiSource.source != Source.WIKI)
            return null

        return loader.load(wikiSource.id)
    }
}