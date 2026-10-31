package app.miyomu.core.data

import app.miyomu.core.model.ContentDetails
import app.miyomu.core.model.ContentFilter
import app.miyomu.core.model.ContentFilterOptions
import app.miyomu.core.model.ContentId
import app.miyomu.core.model.ContentSummary
import app.miyomu.core.model.Outcome
import app.miyomu.core.model.PageSummary
import app.miyomu.core.model.SourceOption

interface ContentRepository {
    suspend fun sources(): List<SourceOption>
    suspend fun filterOptions(sourceName: String): Outcome<ContentFilterOptions>
    suspend fun browse(
        sourceName: String,
        offset: Int = 0,
        limit: Int = 20,
        query: String? = null,
        filter: ContentFilter = ContentFilter(),
    ): Outcome<List<ContentSummary>>

    suspend fun details(sourceName: String, contentId: ContentId): Outcome<ContentDetails>
    suspend fun pages(sourceName: String, contentId: ContentId, chapterId: app.miyomu.core.model.ChapterId): Outcome<List<PageSummary>>
}
