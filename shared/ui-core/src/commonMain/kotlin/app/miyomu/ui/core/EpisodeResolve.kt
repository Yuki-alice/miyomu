package app.miyomu.ui.core

import app.miyomu.model.ResolvedVideo
import app.miyomu.ui.MiyomuGraph

/** Resolved stream urls rotate, so a cached resolve older than this is treated as absent. */
internal const val RESOLVE_CACHE_TTL_MILLIS = 30L * 60 * 1000

/**
 * Resolves one episode url through the source runtime, persisting the result in the resolve cache.
 *
 * This used to be inline in the source detail screen, which meant the player could only advance to
 * the next episode through a callback owned by that screen. Lifting it here lets the player
 * resolve the next episode itself, so playback survives the detail screen being popped.
 */
suspend fun resolveEpisodeStream(
    graph: MiyomuGraph,
    sourceId: String,
    episodeUrl: String,
): ResolvedVideo? = runCatching {
    val now = graph.nowMillis()
    graph.data.videoResolve.get(episodeUrl, RESOLVE_CACHE_TTL_MILLIS, now)
        ?: graph.sourceRuntime.resolveVideo(sourceId, episodeUrl).also { resolved ->
            graph.data.videoResolve.put(episodeUrl, resolved, now)
        }
}.getOrNull()
