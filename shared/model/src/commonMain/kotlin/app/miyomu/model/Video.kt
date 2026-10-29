package app.miyomu.model

import kotlinx.serialization.Serializable

/**
 * One playable stream returned by an anime source resolve (D2) and cached by the data layer.
 * `type` is one of `hls`, `dash`, `mp4`; `quality` is source-provided (e.g. `1080P`).
 */
@Serializable
data class ResolvedStream(
    val url: String,
    val quality: String? = null,
    val type: String = "hls",
)

/**
 * A resolved playback target: stream candidates plus the anti-leech request headers
 * (Referer / User-Agent) that must accompany the media request. Both desktop MPV and Android
 * ExoPlayer receive these through mediamp's `UriMediaData.headers` (ADR-0014).
 */
@Serializable
data class ResolvedVideo(
    val streams: List<ResolvedStream> = emptyList(),
    val headers: Map<String, String> = emptyMap(),
) {
    fun bestStream(): ResolvedStream? = streams.firstOrNull()
}
