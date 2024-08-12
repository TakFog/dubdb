package takutility.dubdb.tasks.wiki

import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode
import takutility.dubdb.entities.*
import takutility.dubdb.tasks.TaskResult
import takutility.dubdb.wiki.WikiPageLoader
import takutility.dubdb.wiki.asEntity
import takutility.dubdb.wiki.asMovie

private val SPLITS = listOf(" in ", " ne ")

class ReadDubberSection(loader: WikiPageLoader): WikiPageTask(loader) {

    fun run(dubber: DubberRef): TaskResult {
        val pageId = dubber.wiki ?: return TaskResult.empty
        val title = load(pageId.id)?.selectFirst("#Doppiaggio")
            ?: return TaskResult.empty

        val entities = mutableListOf<DubbedEntity>()

        /* structure:
         *     <div><h2>Doppiaggio</h2></div>
         *     <div><h3>Film</h3></div>
         *     <div><ul>...</ul></div>
         *     <div><h3>Film d'animazione</h3></div>
         *     <div><ul>...</ul></div>
         *     ...
         *     <div><h2>Note</h2></div>
         */
        var element = title.parent()!!.nextElementSibling()
        // take title sibling up to the next h2 element
        while (element != null && "h2" != element.tagName()) {
            if ("ul" != element.tagName()) {
                // ignore all non ul tags
                element = element.nextElementSibling()
                continue
            }
            element.select("li").forEach { li: Element ->
                val split = findSplit(li) ?: return@forEach //no split, ignore

                val entity: EntityRef = if (split.index == 1) {
                    li.child(0).asEntity() // use the first tag as entity name
//                } else if (split.fullTag) { TODO
                } else {
                    val text = li.text()
                    val end = text.indexOf(split.token)
                    val charName = text.substring(0, end).trim { it <= ' ' }
                    if (charName.length >= 200) return@forEach // too long name
                    EntityRefImpl(charName)
                }

                for (i in split.index + 1 until li.childNodeSize()) {
                    val node = li.childNode(i) as? Element ?: continue
                    node.select("a").forEach { link: Element ->
                        entities.add(
                            DubbedEntity(
                                dubber = dubber,
                                movie = link.asMovie(),
                                name = entity.name!!,
                                ids = entity.ids,
                                sources = mutableListOf(RawData(pageId, DataSource.DUBBER, li.html()))
                            )
                        )
                    }
                }
            }
            element = element.nextElementSibling()
        }

        return TaskResult(dubbedEntities = entities)
    }

    /**
     * Look for the child text node containing a split token
     */
    private fun findSplit(li: Element): Split? {
        for (splitIndex in 0 until li.childNodeSize()) {
            val node = li.childNode(splitIndex)
            if (node !is TextNode) continue
            val text = node.text()
            for (sv in SPLITS) {
                if (text.contains(sv)) {
                    return Split(splitIndex, sv,
                        (splitIndex != 1 || text != sv))
                }
            }
        }
        return null //split not found
    }
}

private data class Split(
    /**
     * Index of the li child containing the split token
     */
    val index: Int,
    /**
     * Split token found
     */
    val token: String,
    /**
     * false if the split token appears in a longer text element
     */
    val fullTag: Boolean
)