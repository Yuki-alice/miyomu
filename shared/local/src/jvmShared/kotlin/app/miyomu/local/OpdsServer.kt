package app.miyomu.local

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.cio.CIO
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.http.content.ByteArrayContent
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import java.io.File

/**
 * P1-6 OPDS 网络服务化: serves the `.opds-export` directory as an OPDS 1.2 catalog over the
 * local network — `/catalog.xml` is the acquisition feed (regenerated per request so fresh
 * exports show up without a restart) and `/books/<file>` streams the book copies.
 *
 * Path traversal is blocked: the requested name must resolve inside the books directory.
 * The handle must be [stopped][OpdsServerHandle.stop] when the service is no longer needed.
 */
class OpdsServerHandle internal constructor(
    private val server: EmbeddedServer<*, *>,
    val port: Int,
) {
    fun stop() {
        server.stop(gracePeriodMillis = 500, timeoutMillis = 1_000)
    }
}

suspend fun startOpdsServer(
    port: Int,
    exportRoot: File,
    title: String,
    booksProvider: () -> List<LocalBook>,
    nowMillis: () -> Long,
): OpdsServerHandle {
    val booksDir = File(exportRoot, "books").canonicalFile
    val server = embeddedServer(CIO, port = port) {
        routing {
            get("/") { call.respondRedirect("/catalog.xml") }
            get("/catalog.xml") {
                call.respondText(
                    OpdsCatalog.buildAcquisitionFeed(title, booksProvider(), nowMillis()),
                    ContentType.Application.Xml,
                )
            }
            get("/books/{name}") {
                val name = call.parameters["name"].orEmpty()
                val target = File(booksDir, name)
                val allowed = target.canonicalFile.path.startsWith(booksDir.path) && target.isFile
                if (!allowed) {
                    call.respond(HttpStatusCode.NotFound)
                    return@get
                }
                call.respond(ByteArrayContent(target.readBytes(), ContentType.parse(mimeForFileName(target.name))))
            }
        }
    }
    server.start(wait = false)
    val boundPort = runCatching {
        server.engine.resolvedConnectors().first().port
    }.getOrDefault(port)
    return OpdsServerHandle(server, boundPort)
}

/** First site-local IPv4 address, for showing the LAN URL in the UI. */
fun lanAddress(): String? = runCatching {
    java.net.NetworkInterface.getNetworkInterfaces().asSequence()
        .flatMap { it.inetAddresses.asSequence() }
        .firstOrNull { it.isSiteLocalAddress }
        ?.hostAddress
}.getOrNull()

/** Serves MIME by file extension; unknown extensions stream as octet-stream. */
fun mimeForFileName(name: String): String = when (File(name).extension.lowercase()) {
    "zip", "cbz" -> "application/zip"
    "7z", "cb7" -> "application/x-7z-compressed"
    "pdf" -> "application/pdf"
    "epub" -> "application/epub+zip"
    "txt", "md", "markdown" -> "text/plain"
    "html", "htm" -> "text/html"
    "mobi" -> "application/x-mobipocket-ebook"
    else -> "application/octet-stream"
}
