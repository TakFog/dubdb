package takutility.dubdb.tasks.wiki

import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode
import takutility.dubdb.DubDbContext
import takutility.dubdb.entities.*
import takutility.dubdb.tasks.TaskResult
import takutility.dubdb.wiki.asEntity
import takutility.dubdb.wiki.asMovie

private val SPLITS = listOf(" in ", " ne ")
private val CHAR_SPLITS = setOf(",", "e")

class ReadDubberSection(context: DubDbContext): WikiPageTask(context) {

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

                val rowEntities = getEntities(li, split)

                for (i in split.index + 1 until li.childNodeSize()) {
                    val node = li.childNode(i) as? Element ?: continue
                    node.select("a").forEach { link: Element ->
                        rowEntities.forEach { entity ->
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
                    return Split(splitIndex, sv, text == sv)
                }
            }
        }
        return null //split not found
    }

    private fun getEntities(li: Element, split: Split): List<EntityRef> {
        var entities: List<EntityRef>? = null
        if (split.index == 1) {
            // use the first tag as entity
            entities = listOf(li.child(0).asEntity())
        }

        if (split.fullTag && (0 until split.index).map(li::childNode)
                .all { (it is Element && it.tagName() == "a") || (it is TextNode && it.text().trim() in CHAR_SPLITS) })
        {
            // all elements before split are links or connectors
            entities = (0 until split.index)
                .mapNotNull { i -> (li.childNode(i) as? Element)?.asEntity() }
        }

        if (entities != null) {
            // entities from tags
            return entities.flatMap { ent ->
                if ("/" in ent.name!!) {
                    ent.name!!.splitToSequence("/")
                        .map { EntityRefImpl(it.trim(), ent.ids.toMutable()) }
                } else {
                    sequenceOf(ent)
                }
            }
        }

        val text = li.text()
        val end = text.indexOf(split.token)
        val charName = text.substring(0, end).trim { it <= ' ' }
        if (charName.length >= 200) return listOf() // too long name
        return listOf(EntityRefImpl(charName))
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