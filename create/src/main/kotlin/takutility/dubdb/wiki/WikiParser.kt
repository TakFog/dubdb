package takutility.dubdb.wiki

import org.jsoup.nodes.Element
import takutility.dubdb.entities.*

private fun <E : EntityRef> parse(aTag: Element, ctor: (String) -> E): E {
    val srcId = aTag.asWikiSourceId()
    return ctor(aTag.text()).apply { ids += srcId }
}

fun Element.asWikiId(): String = absUrl("href")
    .let { Source.WIKI.urlToId(it)
        ?: Source.WIKI_MISSING.urlToId(it)
        ?: it }

fun Element.asWikiSourceId(): SourceId = absUrl("href")
    .let { SourceId.fromUrl(Source.WIKI, it)
        ?: SourceId.fromUrl(Source.WIKI_MISSING, it)
        ?: SourceId(Source.UNK, it)
    }

fun Element.asEntity(): EntityRef = parse(this) { name -> EntityRefImpl(name) }

fun Element.asActor() = parse(this) { name -> Actor(name) }

fun Element.asDubber() = parse(this) { name -> Dubber(name) }

fun Element.asMovie(): MovieRef = parse(this) { name -> movieRefOf(name) }