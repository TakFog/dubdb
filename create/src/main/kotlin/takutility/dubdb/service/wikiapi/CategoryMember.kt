package takutility.dubdb.service.wikiapi

import java.time.Instant

data class CategoryMember(val pageid: Long, val title: String, val timestamp: Instant)
typealias CategoryMemberResponse = WikiApiMapResponse<List<CategoryMember>>