package takutility.dubdb.service.wikiapi

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jsonMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import mu.KotlinLogging
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import takutility.dubdb.userAgent
import java.io.IOException
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private val logger = KotlinLogging.logger {}

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
        try {
            prop?.let { formBody.add("prop", prop) }
            section?.let { formBody.add("section", section.toString()) }

            return call(formBody.build())
                ?.let { json -> try {
                        mapper.readValue<ParseResponse>(json)
                    } catch (e: Exception) {
                        logger.error(e) { "Error parsing: $json"}
                        throw e
                    }
                }
        } catch (e: Exception) {
            logger.error(e) { "Error parsing for \"$title\" $revid $prop $section" }
            throw e
        }
    }

    private fun call(formBody: FormBody): String? {
        val request: Request = Request.Builder()
            .url(httpUrl)
            .header("User-Agent", userAgent)
            .post(formBody)
            .build()

        var attempts = 0
        val maxAttempts = 5
        while (true) {
            attempts++
            try {
                client.newCall(request).execute().use { response ->
                    if (response.code == 429) {
                        if (attempts >= maxAttempts) {
                            throw IOException("Rate limit exceeded (429) after $maxAttempts attempts")
                        }
                        val retryAfterHeader = response.header("Retry-After")
                        val secondsToWait = parseRetryAfter(retryAfterHeader)
                        logger.warn { "Rate limited (429). Retrying in $secondsToWait seconds (attempt $attempts/$maxAttempts) for $formBody" }
                        Thread.sleep(secondsToWait * 1000)
                    } else {
                        return response.body?.string()
                    }
                }
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                logger.error(e) { "Thread interrupted during API retry delay" }
                throw e
            } catch (e: Exception) {
                if (attempts >= maxAttempts) {
                    logger.error(e) { "Error while parsing response for $formBody after $attempts attempts" }
                    throw e
                }
                logger.warn(e) { "Error while calling API. Retrying in 2 seconds... (attempt $attempts/$maxAttempts)" }
                try {
                    Thread.sleep(2000)
                } catch (ie: InterruptedException) {
                    Thread.currentThread().interrupt()
                    throw ie
                }
            }
        }
    }

    private fun parseRetryAfter(headerValue: String?): Long {
        if (headerValue == null) return 5L
        
        val seconds = headerValue.toLongOrNull()
        if (seconds != null) {
            return if (seconds > 0) seconds else 5L
        }
        
        try {
            val date = ZonedDateTime.parse(headerValue, DateTimeFormatter.RFC_1123_DATE_TIME)
            val delay = ChronoUnit.SECONDS.between(Instant.now(), date.toInstant())
            return if (delay > 0) delay else 5L
        } catch (e: Exception) {
            return 5L
        }
    }
}
