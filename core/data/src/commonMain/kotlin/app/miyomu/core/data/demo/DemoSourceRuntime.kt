package app.miyomu.core.data.demo

import org.skepsun.kototoro.core.source.SourceChapter
import org.skepsun.kototoro.core.source.SourceContent
import org.skepsun.kototoro.core.source.SourceDescriptor
import org.skepsun.kototoro.core.source.SourceDetailsFetchMode
import org.skepsun.kototoro.core.source.SourceFilter
import org.skepsun.kototoro.core.source.SourceFilterCapabilities
import org.skepsun.kototoro.core.source.SourceFilterOptions
import org.skepsun.kototoro.core.source.SourcePage
import org.skepsun.kototoro.core.source.SourcePagingMode
import org.skepsun.kototoro.core.source.SourceRef
import org.skepsun.kototoro.core.source.SourceRuntime
import org.skepsun.kototoro.core.source.SourceTag

/**
 * P0 fixture source: no network, deterministic data. It proves the SourceRuntime →
 * Repository → UI path before a real parser is installed.
 */
class DemoSourceRuntime : SourceRuntime {
    private val source = SourceRef(name = "demo", locale = "en", contentType = "MANGA")
    private val tags = setOf(
        SourceTag("Fantasy", "fantasy", source),
        SourceTag("Mystery", "mystery", source),
    )

    private val contents = listOf(
        content(1, "Sky Archive", "Mira Okonkwo", 8.6f, "A cartographer's apprentice chases a library that drifts above the mountains and only opens at dawn, where every book is something that once fell from the sky."),
        content(2, "Northbound Owl", "Signe Aalto", 8.1f, "A night courier follows a letter that refuses to reach its destination."),
        content(3, "Static Bloom", "Kira Moreau", 7.8f, "A city grows flowers from radio interference, and one botanist can hear them."),
        content(4, "Moth & Meteor", "Juno Adeyemi", 8.9f, "Two astronomers discover that the meteor they are tracking is looking back."),
        content(5, "Cloud Kitchen", "Priya Sandoval", 7.4f, "A floating restaurant serves dishes made from weather."),
        content(6, "Neon Monks", "Asha Varga", 8.3f, "A monastery at the edge of a neon city keeps the last analog bells."),
        content(7, "Harbor of Bells", "Lukas Brandt", 7.9f, "Every ship that leaves the harbor takes one bell with it."),
        content(8, "Hollow Star", "Teo Lindqvist", 8.7f, "A star goes missing, and only the night shift at the observatory notices."),
    )

    private val chapters = contents.associate { content ->
        content.id to (1..5).map { number ->
            SourceChapter(
                id = content.id * 1000 + number,
                title = "Chapter $number",
                number = number.toFloat(),
                volume = 1,
                url = "https://example.com/${content.id}/$number",
                scanlator = "Demo",
                uploadDate = 1_760_000_000_000 + number * 86_400_000L,
                branch = "main",
                source = source,
            )
        }
    }

    override suspend fun getSources(): List<SourceRef> = listOf(source)

    override suspend fun describe(sourceName: String): SourceDescriptor = SourceDescriptor(
        source = source,
        sortOrders = setOf("POPULAR", "LATEST"),
        defaultSortOrder = "POPULAR",
        filterCapabilities = SourceFilterCapabilities(
            isMultipleTagsSupported = false,
            isTagsExclusionSupported = false,
            isSearchSupported = true,
            isSearchWithFiltersSupported = false,
            isYearSupported = false,
            isYearRangeSupported = false,
            isOriginalLocaleSupported = false,
            isAuthorSearchSupported = false,
        ),
        pagingMode = SourcePagingMode.OFFSET,
        homepage = "https://example.com",
    )

    override suspend fun getFilterOptions(sourceName: String): SourceFilterOptions = SourceFilterOptions(
        availableTags = tags,
        tagGroups = emptyList(),
        availableStates = emptySet(),
        availableContentRating = emptySet(),
        availableContentTypes = emptySet(),
        availableDemographics = emptySet(),
        availableLocales = emptySet(),
    )

    override suspend fun getList(
        sourceName: String,
        offset: Int,
        order: String?,
        filter: SourceFilter?,
    ): List<SourceContent> {
        val query = filter?.query?.trim().orEmpty()
        val filtered = if (query.isBlank()) {
            contents
        } else {
            contents.filter { it.title.contains(query, ignoreCase = true) }
        }
        return filtered.drop(offset).take(20)
    }

    override suspend fun getDetails(content: SourceContent, fetchMode: SourceDetailsFetchMode): SourceContent =
        content.copy(chapters = chapters[content.id].orEmpty())

    override suspend fun getPages(chapter: SourceChapter, nextChapterUrl: String?): List<SourcePage> =
        (1..3).map { index ->
            SourcePage(
                id = chapter.id * 10 + index,
                url = "https://example.com/page/${chapter.id}/$index.jpg",
                preview = null,
                source = source,
                headers = mapOf("Referer" to "https://example.com/"),
            )
        }

    override suspend fun getPageUrl(page: SourcePage): String = page.url

    override suspend fun getRelated(content: SourceContent): List<SourceContent> =
        contents.filter { it.id != content.id }.take(3)

    private fun content(
        id: Long,
        title: String,
        author: String,
        rating: Float,
        description: String,
    ) = SourceContent(
        id = id,
        title = title,
        altTitles = emptySet(),
        url = "https://example.com/$id",
        publicUrl = "https://example.com/$id",
        rating = rating,
        contentRating = "SAFE",
        coverUrl = null,
        tags = tags,
        state = "ONGOING",
        authors = setOf(author),
        source = source,
        description = description,
    )
}
