package app.miyomu.track

import kotlinx.serialization.Serializable

/** Signed-in tracking account (P1-9). */
data class TrackUser(
    val id: String,
    val name: String,
)

/** A series returned by a tracker search; binding is manual per library item. */
data class TrackSearchResult(
    val id: Long,
    val title: String,
)

/** Which tracker a binding points at. AniList and Bangumi only; MAL is a non-goal. */
enum class TrackTarget { ANILIST, BANGUMI }

/** A library item bound to a tracker entry (stored in preferences, bindings are manual). */
@Serializable
data class TrackBinding(
    val itemId: String,
    val target: TrackTarget,
    val remoteId: Long,
    val remoteTitle: String,
    /** Last progress pushed to the tracker (chapter/episode number). */
    val progress: Int = 0,
)

class TrackException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
