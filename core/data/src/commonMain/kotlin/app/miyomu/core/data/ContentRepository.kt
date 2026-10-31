package app.miyomu.core.data

import app.miyomu.core.model.ContentDetails
import app.miyomu.core.model.ContentId
import app.miyomu.core.model.ContentSummary
import app.miyomu.core.model.ChapterId
import app.miyomu.core.model.Outcome
import app.miyomu.core.model.PageSummary
import app.miyomu.core.model.SourceOption

interface ContentRepository {
    suspend fun sources(): List<SourceOption>
    suspend fun selectSource(sourceName: String)
    suspend fun browse(offset: Int = 0, limit: Int = 20, query: String? = null): Outcome<List<ContentSummary>>
    suspend fun details(contentId: ContentId): Outcome<ContentDetails>
    suspend fun pages(contentId: ContentId, chapterId: ChapterId): Outcome<List<PageSummary>>
}
