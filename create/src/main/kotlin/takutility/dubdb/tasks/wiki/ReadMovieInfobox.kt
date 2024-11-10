package takutility.dubdb.tasks.wiki

import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import takutility.dubdb.DubDbContext
import takutility.dubdb.entities.*
import takutility.dubdb.tasks.TaskResult
import takutility.dubdb.util.splitCharacter
import takutility.dubdb.wiki.asWikiSourceId
import java.util.regex.Pattern

private val boxElementSplit = Pattern.compile("^([^:]+): (.*)$")

private val botTitles = sequenceOf(
    "Interpreti e personaggi" to DataSource.MOVIE_ORIG,
    "Doppiatori originali" to DataSource.MOVIE_ORIG_DUB,
    "Doppiatori italiani" to DataSource.MOVIE_DUB,
    "Doppiatori e personaggi" to DataSource.MOVIE_DUB,
)

private fun isActor(dataSource: DataSource) = when(dataSource) {
    DataSource.MOVIE_ORIG, DataSource.MOVIE_ORIG_DUB -> true
    else -> false
}

class ReadMovieInfobox(context: DubDbContext): WikiPageTask(context) {

    fun run(movie: MovieRef): TaskResult {
        val movieId = movie.wiki
        val doc = load(movieId) ?: return TaskResult.empty

        val titles = doc.select("table.sinottico .sinottico_divisione") ?: return TaskResult.empty

        val dubbed = botTitles
            .mapNotNull { e ->
                val isActor = isActor(e.second)
                getList(titles, e.first)?.select("li")
                    ?.flatMap { parseLi(it) }
                    ?.map { row ->
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

                        dubbedEntity
                    }
            }
            .flatten()
            .toList()

        return TaskResult(dubbedEntities = dubbed)
    }

    private fun parseLi(li: Element): Sequence<RowValues> {
        val text = li.text()
        val matcher = boxElementSplit.matcher(text)
        if (!matcher.matches()) return sequenceOf()

        val actorName = matcher.group(1).trim()
        val charaNames = matcher.group(2).trim()
        val links = li.select("a").associateBy({ it.text().trim() }, { it.asWikiSourceId() })

        return splitCharacter(charaNames).asSequence().map { charaName ->
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