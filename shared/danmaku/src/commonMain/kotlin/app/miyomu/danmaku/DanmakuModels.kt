package app.miyomu.danmaku

import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable

/** One timed comment. `mode` mirrors dandanplay: 1-3 scroll, 4 bottom, 5 top. */
@Serializable
data class DanmakuComment(
    val timeMs: Long,
    val text: String,
    val mode: Int = 1,
    val color: Int = 0xFFFFFF,
)

/** Sorted comments for one episode plus lookup helpers. */
data class DanmakuTrack(
    val episodeId: String,
    val comments: List<DanmakuComment>,
) {
    fun commentsBetween(fromMs: Long, toMs: Long): List<DanmakuComment> =
        comments.filter { it.timeMs in fromMs until toMs }
}

/** User-configured dandanplay credentials; empty means danmaku is off. */
data class DanmakuCredentials(
    val appId: String,
    val appSecret: String,
) {
    val isConfigured: Boolean get() = appId.isNotBlank() && appSecret.isNotBlank()
}

/** Live danmaku state for the player overlay. */
data class DanmakuState(
    val enabled: Boolean,
    val track: DanmakuTrack?,
    val fontSizeSp: Float = 14f,
    val opacity: Float = 0.9f,
)

interface DanmakuRepository {
    val credentials: StateFlow<DanmakuCredentials>

    suspend fun setCredentials(credentials: DanmakuCredentials)

    /** Matches the anime title and returns candidate episodes (id + title). */
    suspend fun searchEpisodes(animeTitle: String): List<DanmakuEpisode>

    /** Downloads the comment track for one dandanplay episode id. */
    suspend fun loadTrack(episodeId: String): DanmakuTrack
}

data class DanmakuEpisode(
    val episodeId: String,
    val animeTitle: String,
    val episodeTitle: String,
)
