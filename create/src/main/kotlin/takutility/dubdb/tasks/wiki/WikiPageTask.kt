package takutility.dubdb.tasks.wiki

import org.jsoup.nodes.Document
import takutility.dubdb.DubDbContext
import takutility.dubdb.entities.Source
import takutility.dubdb.entities.SourceId
import takutility.dubdb.wiki.WikiPage

open class WikiPageTask(val context: DubDbContext) {

    protected fun loadPage(wikiSource: SourceId?): WikiPage? {
        if (wikiSource == null || wikiSource.source != Source.WIKI)
            return null
        return context.wikiPageLoader.page(wikiSource.id)
    }

    protected fun load(wikiSource: SourceId?): Document? {
        if (wikiSource == null || wikiSource.source != Source.WIKI)
            return null
        return context.wikiPageLoader.load(wikiSource.id)
    }

    protected fun load(page: String?): Document? {
        if (page == null)
            return null
        return context.wikiPageLoader.load(page)
    }
}