package takutility.dubdb.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import takutility.dubdb.entities.Dubber
import takutility.dubdb.entities.Source
import takutility.dubdb.entities.SourceIds
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

interface WikiApi {

    fun dubbersFromCat(limit: Int): List<Dubber>
}

class WikiApiImpl: WikiApi {
    val client = OkHttpClient()
    val mapper = ObjectMapper()

    override fun dubbersFromCat(limit: Int): List<Dubber> {
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
            ?.let { json ->
                val obj = mapper.readValue(json, JsonNode::class.java)
                obj["query"]["categorymembers"].asSequence()
                    .map { page -> Dubber(
                        name = cleanWikiTitle(page["title"].asText()),
                        ids = SourceIds.of(Source.WIKI to page["title"].asText().replace(" ", "_")),
                        lastUpdate = LocalDate.ofInstant(Instant.parse(page["timestamp"].asText()), ZoneOffset.UTC),
                    ) }
            }
            ?.toList() ?: listOf()
    }
}

private fun cleanWikiTitle(title: String): String {
    val bracket = title.indexOf("(")
    if (bracket < 3) return title
    return title.substring(0, bracket).trim()
}