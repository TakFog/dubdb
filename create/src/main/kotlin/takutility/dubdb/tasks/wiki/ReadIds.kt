package takutility.dubdb.tasks.wiki

import org.jsoup.nodes.Document
import takutility.dubdb.entities.Source
import takutility.dubdb.entities.SourceId
import takutility.dubdb.entities.SourceIds
import takutility.dubdb.tasks.TaskResult
import takutility.dubdb.wiki.WikiPageLoader

private val EXTERNAL = listOf(Source.IMDB, Source.MONDO_DOPPIATORI)


class ReadIds(loader: WikiPageLoader) : WikiPageTask(loader) {

    fun run(wikiSource: SourceId?): TaskResult {
        val doc = load(wikiSource) ?: return TaskResult.empty
        
        val ids = SourceIds.mutable()

        loadWikidata(doc, ids)
        loadWikiEn(doc, ids)
        loadExternal(doc, ids)

        return TaskResult(sourceIds = ids.toImmutable())
    }

    private fun loadWikidata(doc: Document, ids: SourceIds) {
        val wikidata = doc.selectFirst("#t-wikibase a") ?: return
        SourceId.fromUrl(Source.WIKIDATA, wikidata.absUrl("href"))
            ?.let(ids::add)
    }

    private fun loadWikiEn(doc: Document, ids: SourceIds) {
        val illink = doc.selectFirst(".interwiki-en a") ?: return
        SourceId.fromUrl(Source.WIKI_EN, illink.absUrl("href"))
            ?.let(ids::add)
    }

    private fun loadExternal(doc: Document, ids: SourceIds) {
        val title = doc.select("#Collegamenti_esterni")
        if (title.isEmpty()) return

        val uls = title[0].parent()!!.nextElementSiblings().select("ul")
        if (uls.isEmpty()) return

        for (element in uls[0].select("a.external")) {
            val url = element.absUrl("href")
            for (source in EXTERNAL) {
                SourceId.fromUrl(source, url)?.let {
                    ids.add(it)
                    if (ids.containsAllSrc(EXTERNAL)) return
                }
            }
        }
    }

}