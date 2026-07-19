package takutility.dubdb.tasks.wiki

import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import takutility.dubdb.DubDbContext
import takutility.dubdb.entities.*
import takutility.dubdb.tasks.TaskResult
import takutility.dubdb.util.splitCharacter
import takutility.dubdb.wiki.asWikiSourceId
import takutility.wikitext.WikiLink
import takutility.wikitext.WikiListItem
import takutility.wikitext.WikitextParser
import java.util.regex.Pattern

private val boxElementSplit = Pattern.compile("^([^:]+): (.*)$")

private val botHtmlTitles = sequenceOf(
    "Interpreti e personaggi" to DataSource.MOVIE_ORIG,
    "Doppiatori originali" to DataSource.MOVIE_ORIG_DUB,
    "Doppiatori italiani" to DataSource.MOVIE_DUB,
    "Doppiatori e personaggi" to DataSource.MOVIE_DUB,
)

private val botTitles = sequenceOf(
    "attori" to DataSource.MOVIE_ORIG,
    "doppiatori originali" to DataSource.MOVIE_ORIG_DUB,
    "doppiatori italiani" to DataSource.MOVIE_DUB,
)

private fun isActor(dataSource: DataSource) = when(dataSource) {
    DataSource.MOVIE_ORIG, DataSource.MOVIE_ORIG_DUB -> true
    else -> false
}

class ReadMovieInfobox(context: DubDbContext): WikiPageTask(context) {

    fun run(movie: MovieRef): TaskResult {
        val movieId = movie.wiki
        val page = loadPage(movieId) ?: return TaskResult.empty
        val content = page.mainSection?.content ?: return TaskResult.empty

        // Parse wikitext infobox
        val doc = WikitextParser.parse(content)
        val infobox = doc.allTemplates().firstOrNull { 
            val name = it.name.trim()
            name == "Film" || name == "FictionTV" 
        } ?: return TaskResult.empty

        val dubbed = botTitles
            .mapNotNull { e ->
                val isActor = isActor(e.second)
                infobox.named(e.first)?.value?.walk()
                    ?.filterIsInstance<WikiListItem>()
                    ?.flatMap { parseNode(it) }
                    ?.map { row -> buildEntity(isActor, row, movieId, e, movie) }
            }
            .flatten()
            .toList()

        return TaskResult(dubbedEntities = dubbed)
    }

    fun runHtml(movie: MovieRef): TaskResult {
        val movieId = movie.wiki
        val doc = load(movieId) ?: return TaskResult.empty

        val titles = doc.select("table.sinottico .sinottico_divisione") ?: return TaskResult.empty

        val dubbed = botHtmlTitles
            .mapNotNull { e ->
                val isActor = isActor(e.second)
                getList(titles, e.first)?.select("li")
                    ?.flatMap { parseLi(it) }
                    ?.map { row -> buildEntity(isActor, row, movieId, e, movie) }
            }
            .flatten()
            .toList()

        return TaskResult(dubbedEntities = dubbed)
    }

    private fun buildEntity(
        isActor: Boolean,
        row: RowValues,
        movieId: SourceId?,
        e: Pair<String, DataSource>,
        movie: MovieRef
    ): DubbedEntity {
        val actor = if (isActor) Actor(row.artistName) else null
        val dubber = if (isActor) null else Dubber(row.artistName)
        row.artistWiki?.notUnk()?.let { id ->
            val artist = if (isActor) actor!! else dubber!!
            artist.ids += id
        }
        val raw = RawData(
            sourceId = movieId!!,
            dataSource = e.second,
            raw = row.raw,
        )
        val dubbedEntity = DubbedEntity(
            name = row.charaName,
            movie = movie,
            actor = actor,
            dubber = dubber,
            sources = mutableListOf(raw)
        )
        row.charaWiki?.notUnk()?.let { dubbedEntity.ids += it }

        return dubbedEntity
    }

    private fun parseLi(li: Element): Sequence<RowValues> {
        val text = li.text()
        val matcher = boxElementSplit.matcher(text)
        if (!matcher.matches()) return sequenceOf()

        val actorName = matcher.group(1).trim()
        val charaNames = matcher.group(2).trim()
        val links = li.select("a").associateBy({ it.text().trim() }, { it.asWikiSourceId() })

        return splitCharacter(charaNames).map { charaName ->
            val name = charaName.trim()
            val link = links[name] ?: links[charaNames]
            RowValues(
                artistName = actorName,
                artistWiki = links[actorName],
                charaName = name,
                charaWiki = link,
                raw = li.html(),
            )
        }
    }

    private fun parseNode(li: WikiListItem): Sequence<RowValues> {
        val split = li.content.splitPlain(":")
        if (split.size != 2) return sequenceOf()

        val actorName = split.pre!!.plainText.trim()
        val actorLink = split.pre.walk()
            .filterIsInstance<WikiLink>()
            .firstOrNull { it.plainText == actorName }
            ?.let { SourceId.normalized(Source.WIKI, it.target) }

        val charaNames = split.post!!.plainText.substring(1).trim() // skip :
        val links = split.post.walk()
            .filterIsInstance<WikiLink>()
            .associate { it.plainText to SourceId.normalized(Source.WIKI, it.target) }

        return splitCharacter(charaNames).map { charaName ->
            val name = charaName.trim()
            val link = links[name] ?: links[charaNames]
            RowValues(
                artistName = actorName,
                artistWiki = actorLink,
                charaName = name,
                charaWiki = link,
                raw = li.rawText,
            )
        }
    }

    private fun getList(titles: Elements, title: String): Elements? {
        val lists = titles.firstOrNull { e -> title == e.text() }
            ?.nextElementSiblings()
            ?.select("td.sinottico_testo_centrale ul")
        return if (lists.isNullOrEmpty()) null else lists[0].select("li")
    }

    inner class RowValues(
        val artistName: String,
        val artistWiki: SourceId?,
        val charaName: String,
        val charaWiki: SourceId?,
        val raw: String,
    )
}