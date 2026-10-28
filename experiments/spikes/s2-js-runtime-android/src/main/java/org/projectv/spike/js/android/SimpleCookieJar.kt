package org.projectv.spike.js.android

import java.net.URI

class SimpleCookieJar {
    private data class CookieRecord(
        val name: String,
        val value: String,
        val domain: String,
        val path: String,
    )

    private val cookies = mutableListOf<CookieRecord>()

    @Synchronized
    fun set(url: String, name: String, value: String, domain: String) {
        val host = URI.create(url).host ?: return
        val normalized = domain.trim().lowercase().ifEmpty { host.lowercase() }.removePrefix(".")
        cookies.removeAll { it.name == name && it.domain == normalized }
        cookies += CookieRecord(name, value, normalized, "/")
    }

    @Synchronized
    fun header(url: String): String? {
        val uri = URI.create(url)
        val host = uri.host ?: return null
        val path = uri.path.ifEmpty { "/" }
        val matching = cookies.filter { matches(it, host, path) }
        return matching.takeIf { it.isNotEmpty() }?.joinToString("; ") { "${it.name}=${it.value}" }
    }

    @Synchronized
    fun listJson(url: String): String {
        val uri = URI.create(url)
        val host = uri.host ?: return "[]"
        val path = uri.path.ifEmpty { "/" }
        return cookies.filter { matches(it, host, path) }
            .joinToString(",", "[", "]") { cookie ->
                """{"name":"${escape(cookie.name)}","value":"${escape(cookie.value)}","domain":"${escape(cookie.domain)}"}"""
            }
    }

    @Synchronized
    fun delete(url: String) {
        val host = URI.create(url).host ?: return
        cookies.removeAll { domainMatches(it.domain, host.lowercase()) }
    }

    @Synchronized
    fun storeFromResponse(url: String, setCookieHeaders: List<String>) {
        val host = URI.create(url).host ?: return
        setCookieHeaders.forEach { header ->
            val parts = header.split(";")
            val pair = parts.firstOrNull()?.split("=", limit = 2) ?: return@forEach
            val name = pair[0].trim()
            val value = pair.getOrElse(1) { "" }.trim()
            if (name.isEmpty()) return@forEach

            var domain = host.lowercase()
            var path = "/"
            parts.drop(1).forEach { raw ->
                val attribute = raw.trim()
                when {
                    attribute.startsWith("Domain=", ignoreCase = true) -> {
                        domain = attribute.substringAfter('=').trim().lowercase().removePrefix(".")
                    }
                    attribute.startsWith("Path=", ignoreCase = true) -> {
                        path = attribute.substringAfter('=').trim().ifEmpty { "/" }
                    }
                }
            }
            cookies.removeAll { it.name == name && it.domain == domain && it.path == path }
            cookies += CookieRecord(name, value, domain, path)
        }
    }

    private fun matches(cookie: CookieRecord, host: String, path: String): Boolean =
        domainMatches(cookie.domain, host.lowercase()) && path.startsWith(cookie.path)

    private fun domainMatches(cookieDomain: String, host: String): Boolean =
        host == cookieDomain || host.endsWith(".$cookieDomain")

    private fun escape(value: String): String =
        value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
}
