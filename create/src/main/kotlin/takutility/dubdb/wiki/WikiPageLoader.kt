package takutility.dubdb.wiki

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jsonMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import org.apache.commons.codec.binary.Base32
import takutility.dubdb.Config
import takutility.dubdb.loadConfig
import takutility.dubdb.service.wikiapi.WikiApi
import java.io.File
import java.security.MessageDigest

interface WikiPageLoader {
    fun page(title: String): WikiPage

    companion object {

        fun fromConfig(wikiApi: WikiApi, config: Config = loadConfig()): WikiPageLoader {
            val cachePath = config.wiki.cache
            return if (cachePath != null)
                CachedWikiPageLoader(wikiApi, cachePath)
            else
                WebWikiPageLoader(wikiApi)
        }

    }
}

class WebWikiPageLoader(private val api: WikiApi): WikiPageLoader {

    override fun page(title: String) = WikiApiPage(api, title)
}

class CachedWikiPageLoader(private val api: WikiApi, private val cacheDir: File): WikiPageLoader {

    private companion object {
        private val hash = MessageDigest.getInstance("MD5")
        private val base32 = Base32()
        private val invalidChars = Regex("[^\\p{L}\\d_'.-]")
    }

    private val mapper = jsonMapper {
        addModules(kotlinModule(), JavaTimeModule())
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        serializationInclusion(JsonInclude.Include.NON_NULL)
        addMixIn(WikiSection::class.java, SimpleWikiApiSection::class.java)
    }
    
    constructor(api: WikiApi, cacheDir: String) : this(api, File(cacheDir))
    
    override fun page(title: String): WikiPage
        = loadCache(title) ?: WikiApiPage(api, title, this::saveCache)

    /**
     * Loads the cache for a given page title
     */
    private fun loadCache(title: String): WikiApiPage? {
        val file = titleToFile(title)
        if (!file.exists()) {
            return null
        }

        return try {
            val page = mapper.readValue<SimpleWikiPage>(file)
            WikiApiPage(api, page, this::saveCache)
        } catch (e: Exception) {
            // If cache is corrupted, return empty cache
            null
        }
    }

    /**
     * Saves the cache for a given page
     */
    private fun saveCache(page: WikiApiPage) {
        val file = titleToFile(page.title)
        cacheDir.mkdirs()
        mapper.writerWithDefaultPrettyPrinter().writeValue(file, page.toSimple())
    }

    /**
     * Converts a page title to a cache file path
     */
    private fun titleToFile(title: String): File {
        val noSpaceTitle = title.replace(" ", "_")
        val cleanTitle = noSpaceTitle.replace(invalidChars, "_")
        if (cleanTitle == noSpaceTitle) {
            return cacheDir.resolve("${noSpaceTitle}.json")
        }
        val titleHash = base32.encodeAsString(hash.digest(noSpaceTitle.encodeToByteArray())).replace("=", "").lowercase()
        return cacheDir.resolve("${cleanTitle}_${titleHash}.json")
    }
}