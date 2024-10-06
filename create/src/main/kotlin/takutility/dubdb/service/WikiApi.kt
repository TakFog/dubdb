package takutility.dubdb.service

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jsonMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.Instant

interface WikiApi {

    fun dubbersFromCat(limit: Int): CategoryMemberResponse
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class WikiApiResponse<T>(val query: Map<String, T>) {
    fun queryValue() = query.values.first()
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class CategoryMember(val pageid: Long, val title: String, val timestamp: Instant)
typealias CategoryMemberResponse = WikiApiResponse<List<CategoryMember>>

class WikiApiImpl: WikiApi {
    private val client = OkHttpClient()
    private val mapper = jsonMapper {
        addModules(kotlinModule(), JavaTimeModule())
    }

    override fun dubbersFromCat(limit: Int): CategoryMemberResponse {
        val url = "https://it.wikipedia.org/w/api.php".toHttpUrlOrNull()!!.newBuilder()
            .addQueryParameter("action", "query")
            .addQueryParameter("format", "json")
            .addQueryParameter("formatversion", "2")
            .addQueryParameter("list", "categorymembers")
            .addQueryParameter("cmtitle", "Categoria:Doppiatori_italiani")
            .addQueryParameter("cmprop", "ids|title|timestamp")
            .addQueryParameter("cmlimit", limit.toString())
            .addQueryParameter("cmsort", "timestamp")
            .addQueryParameter("cmdir", "desc")
            .build()

        val request: Request = Request.Builder().url(url).build()

        return client.newCall(request).execute().use { response -> response.body?.string() }
            ?.let { json -> mapper.readValue<CategoryMemberResponse>(json) }
            ?: CategoryMemberResponse(mapOf("categorymembers" to listOf()))
    }
}
