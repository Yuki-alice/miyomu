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
 * Minimal DASH downloader (P1-2): parses a single-Period MPD for the first adaptation set's
 * representation and concatenates the init segment + media segments into one fragmented MP4
 * file — fMP4 segments carry their own moof/mdat boxes, so the concat is a valid fragmented
 * MP4 that ExoPlayer/MPV play natively (no remuxing library needed).
 *
 * Supported MPD shapes: SegmentTemplate with `$Number$` or `$Time$`-free media templates and
 * SegmentList with SegmentURL entries, single Period / first AdaptationSet / first
 * Representation. richer MPD variants (multiple Periods, SegmentTimeline, content
 * steering) are a dedicated slice.
 */
suspend fun downloadDash(
    client: HttpClient,
    mpdUrl: String,
    target: File,
    onProgress: suspend (bytesDone: Long) -> Unit,
): Long {
    val mpd = getText(client, mpdUrl)
    val mpdBase = mpdUrl.substringBeforeLast('/')

    val baseUrl = findTagAttribute(mpd, "BaseURL", "href")?.let { resolveAgainst(mpdBase, it.trim()) } ?: mpdBase
    val initialization = findTagAttribute(mpd, "Initialization", "sourceURL")?.let { resolveAgainst(baseUrl, it.trim()) }
    val segmentList = LIST_URL_REGEX.findAll(mpd)
        .map { resolveAgainst(baseUrl, it.groupValues[1].trim()) }
        .toList()

    val segments = buildList {
        initialization?.let(::add)
        addAll(segmentList)
        if (isEmpty()) {
            // SegmentTemplate with $Number$: expand across the media presentation duration.
            val template = TEMPLATE_ATTRIBUTE.find(mpd)?.value ?: throw IllegalStateException(
                "DASH MPD 无法解析出分段（缺少 SegmentList/SegmentTemplate）",
            )
            val mediaTemplate = attribute(template, "media") ?: throw IllegalStateException("DASH SegmentTemplate 缺少 media 属性")
            val start = attribute(template, "startNumber")?.toIntOrNull() ?: 1
            val duration = attribute(template, "duration")?.toLongOrNull() ?: 0L
            val timescale = attribute(template, "timescale")?.toLongOrNull() ?: 1L
            val segmentSeconds = if (timescale > 0) duration.toDouble() / timescale else 0.0
            val total = parseIsoDuration(MEDIA_DURATION_REGEX.find(mpd)?.groupValues?.get(1).orEmpty())
            val count = when {
                segmentSeconds > 0 && total > 0 -> (total / segmentSeconds).toInt().coerceAtLeast(1)
                else -> throw IllegalStateException("DASH MPD 无法推断分段数量（缺 duration/MediaPresentationDuration）")
            }
            for (number in start until start + count) {
                val mediaName = mediaTemplate
                    .replace(NUMBER_PLACEHOLDER, number.toString())
                    .replace(REPRESENTATION_PLACEHOLDER, "")
                add(resolveAgainst(baseUrl, mediaName))
            }
        }
    }

    target.parentFile?.mkdirs()
    var done = 0L
    FileOutputStream(target).use { out ->
        for (segment in segments) {
            val response: HttpResponse = client.get(segment)
            if (!response.status.isSuccess()) {
                throw IllegalStateException("DASH 分段下载失败：HTTP ${response.status.value}")
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
        throw IllegalStateException("请求失败：HTTP ${response.status.value}")
    }
    return response.bodyAsText()
}

private fun resolveAgainst(base: String, href: String): String =
    runCatching { java.net.URI(base).resolve(href).toString() }.getOrDefault(href)

/** Finds the value of [attrName] on the first `<tag ... attrName="value" ...>` element in [xml]. */
private fun findTagAttribute(xml: String, tagName: String, attrName: String): String? {
    val tagPattern = Regex("<$tagName\\b[^>]*>")
    val tagMatch = tagPattern.find(xml) ?: return null
    return Regex("""$attrName="([^"]*)"""").find(tagMatch.value)?.groupValues?.get(1)
}

private fun attribute(tag: String, name: String): String? =
    Regex("""$name="([^"]*)"""").find(tag)?.groupValues?.get(1)

private fun String.findTag(tag: String): String? =
    Regex("<$tag\\b[^>]*>").find(this)?.value

private val LIST_URL_REGEX = Regex("<SegmentURL[^>]*media=\"([^\"]+)\"")
private val TEMPLATE_ATTRIBUTE = Regex("<SegmentTemplate\\b[^>]*>")
private val TAG_ATTRIBUTE = Regex("<\\w+\\b[^>]*>")
private val MEDIA_DURATION_REGEX = Regex("mediaPresentationDuration=\"([^\"]+)\"")
private val NUMBER_PLACEHOLDER = "\$Number\$"
private val REPRESENTATION_PLACEHOLDER = "\$RepresentationID\$"

/** Parses ISO-8601 durations like PT1H2M10.5S into seconds. */
internal fun parseIsoDuration(raw: String): Double {
    val match = Regex(
        "PT(?:(\\d+)H)?(?:(\\d+)M)?(?:([\\d.]+)S)?",
    ).find(raw.trim()) ?: return 0.0
    val hours = match.groupValues[1].toDoubleOrNull() ?: 0.0
    val minutes = match.groupValues[2].toDoubleOrNull() ?: 0.0
    val seconds = match.groupValues[3].toDoubleOrNull() ?: 0.0
    return hours * 3600 + minutes * 60 + seconds
}
