package org.projectv.spike.js.ios

class SimpleCookieJar {
    private data class CookieRecord(
        val name: String,
        val value: String,
        val domain: String,
        val path: String,
    )

    private data class Target(val host: String, val path: String)

    private val cookies = mutableListOf<CookieRecord>()

    fun set(url: String, name: String, value: String, domain: String) {
        val target = parseUrl(url) ?: return
        val normalized = domain.trim().lowercase().ifEmpty { target.host }.removePrefix(".")
        cookies.removeAll { it.name == name && it.domain == normalized }
        cookies += CookieRecord(name, value, normalized, "/")
    }

    fun header(url: String): String? {
        val target = parseUrl(url) ?: return null
        val matching = cookies.filter { matches(it, target.host, target.path) }
        return matching.takeIf { it.isNotEmpty() }?.joinToString("; ") { "${it.name}=${it.value}" }
    }

    fun listJson(url: String): String {
        val target = parseUrl(url) ?: return "[]"
        return cookies.filter { matches(it, target.host, target.path) }
            .joinToString(",", "[", "]") { cookie ->
                """{"name":"${escape(cookie.name)}","value":"${escape(cookie.value)}","domain":"${escape(cookie.domain)}"}"""
            }
    }

    fun delete(url: String) {
        val target = parseUrl(url) ?: return
        cookies.removeAll { domainMatches(it.domain, target.host) }
    }

    fun storeFromResponse(url: String, setCookieHeaders: List<String>) {
        val target = parseUrl(url) ?: return
        setCookieHeaders.forEach { header ->
            val parts = header.split(";")
            val pair = parts.firstOrNull()?.split("=", limit = 2) ?: return@forEach
            val name = pair[0].trim()
            val value = pair.getOrElse(1) { "" }.trim()
            if (name.isEmpty()) return@forEach

            var domain = target.host
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
        domainMatches(cookie.domain, host) && path.startsWith(cookie.path)

    private fun domainMatches(cookieDomain: String, host: String): Boolean =
        host == cookieDomain || host.endsWith(".$cookieDomain")

    private fun parseUrl(url: String): Target? {
        val rest = when {
            url.startsWith("http://") -> url.removePrefix("http://")
            url.startsWith("https://") -> url.removePrefix("https://")
            else -> return null
        }
        val slash = rest.indexOf('/')
        val authority = if (slash >= 0) rest.substring(0, slash) else rest
        val path = if (slash >= 0) rest.substring(slash) else "/"
        val host = authority.substringBefore(':').lowercase()
        if (host.isEmpty()) return null
        return Target(host, path.ifEmpty { "/" })
    }

    private fun escape(value: String): String =
        value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
}
