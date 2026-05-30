package takutility.dubdb.wiki

import org.jsoup.nodes.Element
import takutility.dubdb.entities.*
import takutility.wikitext.WikiLink

private fun <E : EntityRef> parse(aTag: Element, ctor: (String) -> E): E {
    val srcId = aTag.asWikiSourceId()
    return ctor(aTag.text()).apply { ids += srcId }
}

private fun <E : EntityRef> parse(linkNode: WikiLink, ctor: (String) -> E): E {
    val srcId = linkNode.asWikiSourceId()
    return ctor(linkNode.plainText).apply { ids += srcId }
}

fun Element.asWikiId(): String = absUrl("href")
    .let { Source.WIKI.urlToId(it)
        ?: Source.WIKI_MISSING.urlToId(it)
        ?: it }

fun WikiLink.asWikiSourceId(): SourceId = SourceId(Source.WIKI, this.target)

fun Element.asWikiSourceId(): SourceId = absUrl("href")
    .let { SourceId.fromUrl(Source.WIKI, it)
        ?: SourceId.fromUrl(Source.WIKI_MISSING, it)
        ?: SourceId(Source.UNK, it)
    }

fun WikiLink.asEntity(): EntityRef = parse(this) { name -> EntityRefImpl(name) }

fun Element.asEntity(): EntityRef = parse(this) { name -> EntityRefImpl(name) }

fun Element.asActor() = parse(this) { name -> Actor(name) }

fun Element.asDubber() = parse(this) { name -> Dubber(name) }

fun WikiLink.asMovie(): MovieRef = parse(this) { name -> movieRefOf(name) }

fun Element.asMovie(): MovieRef = parse(this) { name -> movieRefOf(name) }