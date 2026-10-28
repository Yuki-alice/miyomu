package app.miyomu.model

import kotlinx.serialization.Serializable

@Serializable
data class ComicSummary(
    val id: String,
    val title: String,
    val coverUrl: String? = null,
    val author: String? = null,
    val tags: List<String> = emptyList(),
)

@Serializable
data class ComicDetails(
    val id: String,
    val title: String,
    val coverUrl: String? = null,
    val description: String? = null,
    val tags: Map<String, List<String>> = emptyMap(),
    val chapters: List<Chapter> = emptyList(),
    val updateTime: String? = null,
    val url: String? = null,
)

@Serializable
data class Chapter(
    val id: String,
    val title: String,
    val group: String? = null,
)

@Serializable
data class PageRef(
    val url: String,
    val headers: Map<String, String> = emptyMap(),
)
