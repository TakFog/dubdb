package takutility.dubdb.service.wikiapi

import com.fasterxml.jackson.annotation.JsonProperty

data class ParseResponse(val parse: Parse?, val error: Any? = null)

data class Parse (
    val title: String,
    val pageid: Long,
    val revid: Long,
    val wikitext: String? = null,
    val langlinks: List<Langlink>? = null,
    val tocdata: Tocdata? = null,
    val properties: Properties? = null
)

data class Langlink (
    val lang: String? = null,
    val url: String? = null,
    val title: String? = null
)

data class Properties (
    val defaultsort: String? = null,

    @get:JsonProperty("page_image_free")@field:JsonProperty("page_image_free")
    val pageImageFree: String? = null,

    @get:JsonProperty("wikibase_item")@field:JsonProperty("wikibase_item")
    val wikibaseItem: String? = null
)

data class Tocdata (
    val sections: List<Section>? = null,
    val extensionData: List<Any?>? = null
)

data class Section (
    val tocLevel: Long? = null,
    val hLevel: Long? = null,
    val line: String? = null,
    val number: String? = null,
    val index: String? = null,
    val codepointOffset: Long? = null,
)
