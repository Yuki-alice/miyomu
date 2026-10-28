package org.projectv.spike.js.ios

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.sizeOf
import kotlinx.cinterop.toKString
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.darwin.inet_addr
import platform.posix.AF_INET
import platform.posix.IPPROTO_TCP
import platform.posix.SOCK_STREAM
import platform.posix.close
import platform.posix.connect
import platform.posix.errno
import platform.posix.recv
import platform.posix.send
import platform.posix.socket
import platform.posix.sockaddr_in
import platform.posix.strerror

@OptIn(ExperimentalForeignApi::class)
suspend fun httpGet(url: String, cookies: SimpleCookieJar): String = withContext(Dispatchers.Default) {
    val target = parseHttpUrl(url)
    val socketFd = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP)
    check(socketFd >= 0) { "socket() failed: ${strerror(errno)?.toKString()}" }
    try {
        memScoped {
            val address = alloc<sockaddr_in>()
            address.sin_family = AF_INET.convert()
            address.sin_port = htons(target.port)
            address.sin_addr.s_addr = inet_addr(target.host)
            val result = connect(
                socketFd,
                address.ptr.reinterpret(),
                sizeOf<sockaddr_in>().convert(),
            )
            check(result == 0) {
                "connect(${target.host}:${target.port}) failed: ${strerror(errno)?.toKString()}"
            }
        }

        val cookieHeader = cookies.header(url)
            ?.replace("\r", "")
            ?.replace("\n", "")
        val request = buildString {
            append("GET ${target.path} HTTP/1.1\r\n")
            append("Host: ${target.host}:${target.port}\r\n")
            if (cookieHeader != null) {
                append("Cookie: $cookieHeader\r\n")
            }
            append("Connection: close\r\n\r\n")
        }
        val requestBytes = request.encodeToByteArray()
        requestBytes.usePinned { pinned ->
            var sent = 0
            while (sent < requestBytes.size) {
                val count = send(
                    socketFd,
                    pinned.addressOf(sent),
                    (requestBytes.size - sent).convert(),
                    0,
                )
                check(count > 0) { "send() failed: ${strerror(errno)?.toKString()}" }
                sent += count.toInt()
            }
        }

        val buffer = ByteArray(8192)
        val response = ArrayList<Byte>()
        while (true) {
            val count = buffer.usePinned { pinned ->
                recv(socketFd, pinned.addressOf(0), buffer.size.convert(), 0)
            }
            if (count <= 0) break
            for (i in 0 until count.toInt()) {
                response.add(buffer[i])
            }
        }

        val text = response.toByteArray().decodeToString()
        val headerEnd = text.indexOf("\r\n\r\n")
        check(headerEnd >= 0) { "Malformed HTTP response for $url" }
        val headerText = text.substring(0, headerEnd)
        val setCookieHeaders = headerText.lines()
            .filter { it.startsWith("Set-Cookie:", ignoreCase = true) }
            .map { it.substringAfter(':').trim() }
        cookies.storeFromResponse(url, setCookieHeaders)
        val statusLine = headerText.lineSequence().firstOrNull().orEmpty()
        check(statusLine.contains(" 200 ")) { "HTTP status for $url: $statusLine" }
        text.substring(headerEnd + 4)
    } finally {
        close(socketFd)
    }
}

private fun htons(value: Int): UShort =
    (((value and 0xFF) shl 8) or ((value shr 8) and 0xFF)).toUShort()

private data class HttpTarget(val host: String, val port: Int, val path: String)

private fun parseHttpUrl(url: String): HttpTarget {
    require(url.startsWith("http://")) { "Only http:// is supported in the iOS spike: $url" }
    val rest = url.removePrefix("http://")
    val slash = rest.indexOf('/')
    val authority = if (slash >= 0) rest.substring(0, slash) else rest
    val path = if (slash >= 0) rest.substring(slash) else "/"
    val host = authority.substringBefore(':')
    val port = authority.substringAfter(':', "80").toInt()
    return HttpTarget(host, port, path)
}
