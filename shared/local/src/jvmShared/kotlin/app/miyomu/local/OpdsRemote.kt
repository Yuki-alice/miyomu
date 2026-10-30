package app.miyomu.local

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.client.request.basicAuth
import io.ktor.utils.io.readAvailable
import java.io.File
import java.io.FileOutputStream
import java.net.URI

/** One book offered by a remote OPDS acquisition feed (P1-6 外部 feed 接入). */
data class OpdsRemoteBook(
    val title: String,
    val downloadUrl: String,
    val mime: String,
)

/**
 * Minimal OPDS feed client: fetches a catalog URL and lists its acquisition entries, then
 * downloads books into the library root.
 *
 * The entry parser is a pragmatic subset (title + first acquisition link per entry, relative
 * hrefs resolved against the feed URL), plus rel="next" pagination (bounded) and optional
 * HTTP Basic credentials — enough for well-formed OPDS 1.2/1.1 feeds; facets and token auth
 * are a dedicated slice.
 */
suspend fun fetchOpdsFeed(
    client: HttpClient,
    url: String,
    credentials: OpdsCredentials? = null,
    maxPages: Int = 5,
): List<OpdsRemoteBook> {
    val entries = mutableListOf<OpdsRemoteBook>()
    var current: String? = url
    var pages = 0
    while (current != null && pages < maxPages) {
        val pageUrl = current
        val body = getBody(client, pageUrl, credentials)
        val base = URI(pageUrl)
        entries += parseEntries(body, base)
        // OPDS pagination: follow rel="next" until the last page or the page budget.
        current = NEXT_LINK_REGEX.find(body)?.groupValues?.get(1)?.let { href ->
            runCatching { base.resolve(href).toString() }.getOrNull()
        }
        pages += 1
    }
    return entries
}

internal fun parseEntries(body: String, base: URI): List<OpdsRemoteBook> {
    return ENTRY_REGEX.findAll(body).mapNotNull { entryMatch ->
        val block = entryMatch.value
        val title = TITLE_REGEX.find(block)?.groupValues?.get(1)?.let(::unescape)?.trim()
            .takeIf { !it.isNullOrBlank() } ?: return@mapNotNull null
        val link = LINK_REGEX.findAll(block)
            .map { attrs ->
                Triple(
                    attribute(attrs.value, "href"),
                    attribute(attrs.value, "rel"),
                    attribute(attrs.value, "type"),
                )
            }
            .firstOrNull { (href, rel, _) ->
                !href.isNullOrEmpty() && (rel?.contains("acquisition") == true || !href.endsWith(".xml"))
            } ?: return@mapNotNull null
        val resolved = runCatching { base.resolve(link.first).toString() }.getOrNull()
            ?: return@mapNotNull null
        OpdsRemoteBook(
            title = title,
            downloadUrl = resolved,
            mime = link.third?.ifBlank { null } ?: "application/octet-stream",
        )
    }.toList()
}

/** Streams [book] into [libraryRoot] (conflict-avoiding name) and returns the stored file. */
suspend fun downloadOpdsBook(
    client: HttpClient,
    book: OpdsRemoteBook,
    libraryRoot: File,
    credentials: OpdsCredentials? = null,
): File {
    val response: HttpResponse = if (credentials != null) {
        client.get(book.downloadUrl) { basicAuth(credentials.username, credentials.password) }
    } else {
        client.get(book.downloadUrl)
    }
    if (!response.status.isSuccess()) {
        throw IllegalStateException("下载失败：HTTP ${response.status.value}")
    }
    val pathUrl = book.downloadUrl.substringBefore('?')
    val extension = pathUrl.substringAfterLast('.', "").let { if (it.isEmpty() || it.length > 5) "" else ".$it" }
    val target = uniqueTargetFor(File(libraryRoot, sanitize(book.title) + extension))
    FileOutputStream(target).use { out ->
        val channel = response.bodyAsChannel()
        val buffer = ByteArray(64 * 1024)
        while (!channel.isClosedForRead) {
            val read = channel.readAvailable(buffer)
            if (read == -1) break
            out.write(buffer, 0, read)
        }
    }
    return target
}

private suspend fun getBody(client: HttpClient, url: String, credentials: OpdsCredentials?): String {
    val response: HttpResponse = if (credentials != null) {
        client.get(url) { basicAuth(credentials.username, credentials.password) }
    } else {
        client.get(url)
    }
    if (!response.status.isSuccess()) {
        throw IllegalStateException("OPDS feed 请求失败：HTTP ${response.status.value}")
    }
    return response.bodyAsText()
}

private fun attribute(linkTag: String, name: String): String? =
    Regex("""$name="([^"]*)"""").find(linkTag)?.groupValues?.get(1)

internal fun sanitize(title: String): String = title.replace(Regex("[\\\\/:*?\"<>|]"), "_").ifBlank { "book" }

internal fun uniqueTargetFor(target: File): File {
    if (!target.exists()) return target
    var copy = 1
    while (true) {
        val candidate = File(
            target.parentFile,
            "${target.nameWithoutExtension} ($copy)." + target.extension,
        )
        if (!candidate.exists()) return candidate
        copy++
    }
}

internal fun unescape(raw: String): String = raw
    .replace("&lt;", "<")
    .replace("&gt;", ">")
    .replace("&quot;", "\"")
    .replace("&apos;", "'")
    .replace("&amp;", "&")

/** Optional HTTP Basic credentials for feeds behind access control (P1-6 外部 feed 认证). */
data class OpdsCredentials(val username: String, val password: String)

private val ENTRY_REGEX = Regex("<entry[\\s\\S]*?</entry>")
private val NEXT_LINK_REGEX = Regex("""<link[^>]*rel="next"[^>]*href="([^"]+)"""")
private val TITLE_REGEX = Regex("<title>([\\s\\S]*?)</title>")
private val LINK_REGEX = Regex("<link\\b[^>]*>")
