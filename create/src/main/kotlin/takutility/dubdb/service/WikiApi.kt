package takutility.dubdb.service

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jsonMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import takutility.dubdb.wiki.WikiPage
import java.time.Instant

const val userAgent = "DubDbBot/0.1"

interface WikiApi {

    fun dubbersFromCat(limit: Int): CategoryMemberResponse
    fun info(title: String): WikiPage
}


data class WikiApiResponse<T>(val query: T)
typealias WikiApiMapResponse<T> = WikiApiResponse<Map<String, T>>
fun <T> WikiApiMapResponse<T>.queryValue() = query.values.first()

data class WikiPagesResponse(val pages: List<WikiPage>)


data class CategoryMember(val pageid: Long, val title: String, val timestamp: Instant)
typealias CategoryMemberResponse = WikiApiMapResponse<List<CategoryMember>>
typealias InfoResponse = WikiApiResponse<WikiPagesResponse>

class WikiApiImpl: WikiApi {
    private val client = OkHttpClient()
    private val mapper = jsonMapper {
        addModules(kotlinModule(), JavaTimeModule())
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    }
    private val httpUrl = "https://it.wikipedia.org/w/api.php".toHttpUrlOrNull()!!

    override fun dubbersFromCat(limit: Int): CategoryMemberResponse {
        val formBody = FormBody.Builder()
            .add("action", "query")
            .add("format", "json")
            .add("formatversion", "2")
            .add("list", "categorymembers")
            .add("cmtitle", "Categoria:Doppiatori italiani del XXI secolo")
            .add("cmprop", "ids|title|timestamp")
            .add("cmnamespace", "0")
            .add("cmlimit", limit.toString())
            .add("cmsort", "timestamp")
            .add("cmdir", "desc")
            .build()

        val request: Request = Request.Builder()
            .url(httpUrl)
            .header("User-Agent", userAgent)
            .post(formBody)
            .build()

        return client.newCall(request).execute().use { response -> response.body?.string() }
            ?.let { json -> mapper.readValue<CategoryMemberResponse>(json) }
            ?: CategoryMemberResponse(mapOf("categorymembers" to listOf()))
    }

    override fun info(title: String): WikiPage {
        val formBody = FormBody.Builder()
            .add("action", "query")
            .add("format", "json")
            .add("formatversion", "2")
            .add("prop", "info")
            .add("titles", title.replace("&", "&amp;"))
            .build()

        val request: Request = Request.Builder()
            .url(httpUrl)
            .header("User-Agent", userAgent)
            .post(formBody)
            .build()

        return client.newCall(request).execute().use { response -> response.body?.string() }
            ?.let { json -> mapper.readValue<InfoResponse>(json).query.pages.firstOrNull() }
            ?: WikiPage(title)
    }
}
