package takutility.dubdb.service.wikiapi

data class Info(val pageid: Long, val title: String, val lastrevid: Long, val missing: Boolean? = null)

data class InfoPagesResponse(val pages: List<Info>)
typealias InfoResponse = WikiApiResponse<InfoPagesResponse>

