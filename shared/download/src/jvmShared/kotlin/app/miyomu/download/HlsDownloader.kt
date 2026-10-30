package app.miyomu.download

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.utils.io.readAvailable
import java.io.File
import java.io.FileOutputStream

/**
 * Minimal HLS downloader (P1-2): fetches the media playlist, optionally descending through a
 * master playlist (first variant), then appends every media segment in order into one
 * MPEG-TS file. The result plays in ExoPlayer/MPV without remuxing; a remux-to-MP4 pass is a
 * dedicated slice.
 */
suspend fun downloadHls(
    client: HttpClient,
    playlistUrl: String,
    target: File,
    onProgress: suspend (bytesDone: Long) -> Unit,
): Long {
    var playlist = getText(client, playlistUrl)
    var playlistBase = playlistUrl

    if (MASTER_STREAM_INF in playlist) {
        val variant = playlist.lineSequence()
            .firstOrNull { it.isNotBlank() && !it.startsWith("#") }
            ?: throw IllegalStateException("HLS 主播放列表没有可用的分变率")
        playlistBase = resolveAgainst(playlistBase, variant.trim())
        playlist = getText(client, playlistBase)
    }

    val segments = playlist.lineSequence()
        .filter { it.isNotBlank() && !it.startsWith("#") }
        .map { resolveAgainst(playlistBase, it.trim()) }
        .toList()
    if (segments.isEmpty()) throw IllegalStateException("HLS 播放列表没有媒体分段")

    target.parentFile?.mkdirs()
    var done = 0L
    FileOutputStream(target).use { out ->
        for (segment in segments) {
            val response: HttpResponse = client.get(segment)
            if (!response.status.isSuccess()) {
                throw IllegalStateException("HLS 分段下载失败：HTTP ${response.status.value}")
            }
            val channel = response.bodyAsChannel()
            val buffer = ByteArray(64 * 1024)
            while (!channel.isClosedForRead) {
                val read = channel.readAvailable(buffer)
                if (read == -1) break
                out.write(buffer, 0, read)
                done += read
                onProgress(done)
            }
        }
    }
    return done
}

private suspend fun getText(client: HttpClient, url: String): String {
    val response: HttpResponse = client.get(url)
    if (!response.status.isSuccess()) {
        throw IllegalStateException("HLS 播放列表请求失败：HTTP ${response.status.value}")
    }
    return response.bodyAsText()
}

private fun resolveAgainst(base: String, href: String): String =
    runCatching { java.net.URI(base).resolve(href).toString() }.getOrDefault(href)

private const val MASTER_STREAM_INF = "#EXT-X-STREAM-INF"
