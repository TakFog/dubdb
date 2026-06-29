package takutility.dubdb.service.wikiapi

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jsonMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import takutility.dubdb.userAgent


interface WikiApi {

    fun dubbersFromCat(limit: Int): CategoryMemberResponse
    fun info(title: String): Info?
    fun parse(title: String? = null, revid: Long? = null, prop: String? = null, section: Int? = null): ParseResponse?
}

data class WikiApiResponse<T>(val query: T)
typealias WikiApiMapResponse<T> = WikiApiResponse<Map<String, T>>
fun <T> WikiApiMapResponse<T>.queryValue() = query.values.first()

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

        return call(formBody)
            ?.let { json -> mapper.readValue<CategoryMemberResponse>(json) }
            ?: CategoryMemberResponse(mapOf("categorymembers" to listOf()))
    }

    override fun info(title: String): Info? {
        val formBody = FormBody.Builder()
            .add("action", "query")
            .add("format", "json")
            .add("formatversion", "2")
            .add("prop", "info")
            .add("titles", title)
            .build()

        return call(formBody)
            ?.let { json -> mapper.readValue<InfoResponse>(json).query.pages.firstOrNull() }
            ?.takeIf { it.missing != true }
    }

    override fun parse(title: String?, revid: Long?, prop: String?, section: Int?): ParseResponse? {
        val formBody = FormBody.Builder()
            .add("action", "parse")
            .add("format", "json")
            .add("formatversion", "2")
        if (revid != null) {
            formBody.add("oldid", revid.toString())
        } else if (title != null) {
            formBody.add("page", title)
        } else {
            throw IllegalArgumentException("At least title or revid required")
        }
        prop?.let { formBody.add("prop", prop) }
        section?.let { formBody.add("section", section.toString()) }

        return call(formBody.build())
            ?.let { json -> mapper.readValue<ParseResponse>(json) }
    }

    private fun call(formBody: FormBody): String? {
        val request: Request = Request.Builder()
            .url(httpUrl)
            .header("User-Agent", userAgent)
            .post(formBody)
            .build()

        return client.newCall(request).execute().use { response -> response.body?.string() }
    }
}
