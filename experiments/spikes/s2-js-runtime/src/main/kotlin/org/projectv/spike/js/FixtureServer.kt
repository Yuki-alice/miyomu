package org.projectv.spike.js

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.net.URLDecoder
import java.util.concurrent.Executors

class FixtureServer(
    port: Int = 0,
    private val mediaRoot: java.io.File = java.io.File("spikes/s1-player/fixtures"),
) {
    private val server: HttpServer = HttpServer.create(InetSocketAddress("127.0.0.1", port), 0)
    private val executor = Executors.newCachedThreadPool { runnable ->
        Thread(runnable, "fixture-server").apply { isDaemon = true }
    }

    val baseUrl: String = "http://127.0.0.1:${server.address.port}"

    init {
        server.executor = executor
        server.createContext("/") { exchange -> handle(exchange) }
        server.start()
    }

    private fun handle(exchange: HttpExchange) {
        val path = exchange.requestURI.path
        val query = parseQuery(exchange.requestURI.rawQuery)
        if (path.startsWith("/media/")) {
            serveMedia(exchange, path.removePrefix("/media/"))
            return
        }
        val (status, body, contentType) = when {
            path == "/api/search" -> {
                val keyword = query["q"].orEmpty()
                val page = query["page"]?.toIntOrNull() ?: 1
                Triple(
                    200,
                    """{"items":[{"id":"demo","title":"Demo Comic","author":"Fixture Author","cover":"","tags":["Test","$keyword"]}],"totalPages":1,"page":$page}""",
                    JSON,
                )
            }
            path == "/api/comics/demo" -> Triple(
                200,
                """{"title":"Demo Comic","author":"Fixture Author","cover":"","description":"Fixture description","updatedOn":"2026-10-28","chapters":{"1":"Chapter 1","2":"Chapter 2"}}""",
                JSON,
            )
            path == "/api/comics/demo/chapters/1" -> Triple(
                200,
                """{"images":["/images/1.jpg","/images/2.jpg"]}""",
                JSON,
            )
            path == "/api/slow" -> {
                Thread.sleep(query["ms"]?.toLongOrNull() ?: 300L)
                Triple(200, """{"ok":true}""", JSON)
            }
            path == "/api/cookie-check" -> {
                val cookie = exchange.requestHeaders.getFirst("Cookie").orEmpty()
                Triple(200, """{"cookie":"${jsonEscape(cookie)}"}""", JSON)
            }
            path == "/html/search" -> {
                val keyword = query["q"].orEmpty()
                Triple(
                    200,
                    """<html><body><div class="results">
                        <a class="comic" data-id="demo" href="/html/comic/demo">
                            <span class="title">Demo Comic</span>
                            <span class="author">Fixture Author</span>
                        </a>
                        <a class="comic" data-id="other" href="/html/comic/other">
                            <span class="title">Other Comic</span>
                            <span class="author">Second Author</span>
                        </a>
                        <span class="keyword">${htmlEscape(keyword)}</span>
                    </div></body></html>""",
                    HTML,
                )
            }
            path == "/html/comic/demo" -> Triple(
                200,
                """<html><body><div id="comic">
                    <h1 class="title">Demo Comic</h1>
                    <span class="author">Fixture Author</span>
                    <ul class="chapters">
                        <li><a data-id="1" href="/html/chapter/1">Chapter 1</a></li>
                        <li><a data-id="2" href="/html/chapter/2">Chapter 2</a></li>
                    </ul>
                </div></body></html>""",
                HTML,
            )
            path == "/html/chapter/1" -> Triple(
                200,
                """<html><body><div class="pages">
                    <img data-src="/images/1.jpg" src="placeholder" />
                    <img data-src="/images/2.jpg" src="placeholder" />
                </div></body></html>""",
                HTML,
            )
            path == "/html/demo" -> Triple(
                200,
                """<html><body><a class="comic" href="/demo">Demo</a></body></html>""",
                HTML,
            )
            else -> Triple(404, """{"error":"not found"}""", JSON)
        }
        val bytes = body.toByteArray()
        exchange.responseHeaders.add("Content-Type", contentType)
        exchange.sendResponseHeaders(status, bytes.size.toLong())
        exchange.responseBody.use { it.write(bytes) }
    }

    private fun serveMedia(exchange: HttpExchange, relativePath: String) {
        val root = mediaRoot.canonicalFile
        val file = java.io.File(root, relativePath).canonicalFile
        if (!file.path.startsWith(root.path) || !file.isFile) {
            exchange.sendResponseHeaders(404, 0)
            exchange.responseBody.close()
            return
        }
        val contentType = when (file.extension.lowercase()) {
            "m3u8" -> "application/vnd.apple.mpegurl"
            "ts" -> "video/mp2t"
            "mp4" -> "video/mp4"
            "mkv" -> "video/x-matroska"
            "srt" -> "application/x-subrip"
            "ass" -> "text/x-ssa"
            else -> "application/octet-stream"
        }
        val fileLength = file.length()
        val rangeHeader = exchange.requestHeaders.getFirst("Range")
        var start = 0L
        var end = fileLength - 1
        var status = 200
        if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
            val range = rangeHeader.removePrefix("bytes=").split("-", limit = 2)
            start = range.getOrNull(0)?.toLongOrNull() ?: 0L
            end = range.getOrNull(1)?.toLongOrNull() ?: (fileLength - 1L)
            if (start in 0..end && end < fileLength) {
                status = 206
                exchange.responseHeaders.add("Content-Range", "bytes $start-$end/$fileLength")
            } else {
                exchange.sendResponseHeaders(416, 0)
                exchange.responseBody.close()
                return
            }
        }
        exchange.responseHeaders.add("Content-Type", contentType)
        exchange.responseHeaders.add("Accept-Ranges", "bytes")
        exchange.responseHeaders.add("Cache-Control", "no-store")
        val length = end - start + 1
        exchange.sendResponseHeaders(status, length)
        exchange.responseBody.use { output ->
            java.io.RandomAccessFile(file, "r").use { input ->
                input.seek(start)
                val buffer = ByteArray(64 * 1024)
                var remaining = length
                while (remaining > 0) {
                    val chunk = minOf(buffer.size.toLong(), remaining).toInt()
                    val read = input.read(buffer, 0, chunk)
                    if (read <= 0) break
                    output.write(buffer, 0, read)
                    remaining -= read
                }
            }
        }
    }

    private fun parseQuery(raw: String?): Map<String, String> {
        if (raw.isNullOrBlank()) return emptyMap()
        return raw.split("&").mapNotNull { pair ->
            val parts = pair.split("=", limit = 2)
            if (parts[0].isEmpty()) {
                null
            } else {
                parts[0] to URLDecoder.decode(parts.getOrElse(1) { "" }, "UTF-8")
            }
        }.toMap()
    }

    fun close() {
        server.stop(0)
        executor.shutdownNow()
    }

    private fun jsonEscape(value: String): String =
        value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")

    private fun htmlEscape(value: String): String =
        value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

    private companion object {
        const val JSON = "application/json; charset=utf-8"
        const val HTML = "text/html; charset=utf-8"
    }
}
