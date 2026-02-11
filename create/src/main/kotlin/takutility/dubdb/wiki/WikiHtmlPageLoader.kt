package takutility.dubdb.wiki

import org.apache.commons.codec.binary.Base32
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import takutility.dubdb.Config
import takutility.dubdb.loadConfig
import java.io.File
import java.security.MessageDigest


interface WikiHtmlPageLoader {

    fun page(title: String) = WikiHtmlPage(title, load(title))

    fun load(title: String): Document?

    fun setAsDefault() = WikiHtmlPageLoader.set(this)

    companion object {
        private var inst: WikiHtmlPageLoader? = null

        fun get(): WikiHtmlPageLoader {
            if (inst == null)
               fromConfig()
            return inst!!
        }

        fun fromConfig(config: Config = loadConfig()) {
            val cachePath = config.wiki?.cache
            if (cachePath != null)
                inst = CachedWikiHtmlPageLoader(cachePath)
            else
                inst = WebWikiHtmlPageLoader
        }

        fun set(newInst: WikiHtmlPageLoader) {
            inst = newInst
        }

        fun load(title: String): Document? = get().load(title)
    }
}

object WebWikiHtmlPageLoader: WikiHtmlPageLoader {

    fun toUrl(title: String) = "https://it.wikipedia.org/wiki/$title"

    override fun load(title: String): Document? {
        if (".php" in title) return null
        return Jsoup.connect(toUrl(title)).get()
    }
}

class CachedWikiHtmlPageLoader(cacheDir: File? = null): WikiHtmlPageLoader {
    private companion object {
        private val hash = MessageDigest.getInstance("MD5")
        private val base32 = Base32()
        private val invalidChars = Regex("[^\\p{L}\\d _'.?-]")
    }

    private val cacheDir: File

    init {
        this.cacheDir = cacheDir ?: File("cache")
    }

    constructor(cacheDir: String) : this(File(cacheDir))

    override fun load(title: String): Document? {
        val file = titleToFile(title)
        if (file.exists())
            return Jsoup.parse(file, null, WebWikiHtmlPageLoader.toUrl(title))
        return WebWikiHtmlPageLoader.load(title)?.apply {
            cacheDir.mkdirs()
            file.writeText(outerHtml())
        }
    }

    private fun titleToFile(title: String): File {
        val cleanTitle = title.replace(invalidChars, "_")
        if (cleanTitle == title)
            return cacheDir.resolve("${title}.html")
        val titleHash = base32.encodeAsString(hash.digest(title.encodeToByteArray())).replace("=","").lowercase()
        return cacheDir.resolve("${cleanTitle}_${titleHash}.html")
    }

}