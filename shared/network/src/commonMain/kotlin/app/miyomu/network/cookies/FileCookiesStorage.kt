package app.miyomu.network.cookies

import io.ktor.client.plugins.cookies.CookiesStorage
import io.ktor.client.plugins.cookies.fillDefaults
import io.ktor.client.plugins.cookies.matches
import io.ktor.http.Cookie
import io.ktor.http.Url
import io.ktor.util.date.getTimeMillis
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import kotlinx.io.writeString
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Cookie jar persisted as JSON. Calls are serialized; every mutation rewrites the file
 * atomically through a sibling temporary file.
 *
 * Session cookies (no max-age/expires) are persisted on purpose: source logins often rely
 * on them across app restarts.
 */
class FileCookiesStorage(
    private val file: Path,
    private val nowMillis: () -> Long = { getTimeMillis() },
) : CookiesStorage {

    private val mutex = Mutex()
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private val entries = mutableListOf<StoredCookie>()
    private var loaded = false

    override suspend fun get(requestUrl: Url): List<Cookie> = mutex.withLock {
        loadIfNeeded()
        if (purgeExpired()) persist()

        entries.mapNotNull { entry ->
            if (entry.cookie.matchesSafely(requestUrl)) entry.cookie else null
        }
    }

    suspend fun all(): List<Cookie> = mutex.withLock {
        loadIfNeeded()
        if (purgeExpired()) persist()
        entries.map { it.cookie }
    }

    override suspend fun addCookie(requestUrl: Url, cookie: Cookie) = mutex.withLock {
        if (cookie.name.isBlank()) return@withLock
        loadIfNeeded()

        val now = nowMillis()
        val normalized = cookie.fillDefaults(requestUrl)
        val removed = entries.removeAll { it.cookie.name == normalized.name && it.cookie.matchesSafely(requestUrl) }
        if (normalized.isExpiredAt(now, createdAtMillis = now)) {
            if (removed) persist()
            return@withLock
        }

        entries += StoredCookie(normalized, now)
        persist()
    }

    override fun close() = Unit

    private fun loadIfNeeded() {
        if (loaded) return
        loaded = true
        if (!SystemFileSystem.exists(file)) return

        val text = SystemFileSystem.source(file).buffered().use { it.readString() }
        if (text.isBlank()) return

        val restored = runCatching { json.decodeFromString<List<StoredCookie>>(text) }.getOrNull()
        if (restored == null) {
            // A corrupt jar should cost cookies, not break networking for the source.
            runCatching { SystemFileSystem.atomicMove(file, Path(file.toString() + ".corrupt")) }
            return
        }

        entries += restored
        purgeExpired()
    }

    private fun purgeExpired(): Boolean {
        val now = nowMillis()
        return entries.removeAll { it.isExpiredAt(now) }
    }

    private fun persist() {
        file.parent?.let { SystemFileSystem.createDirectories(it) }

        val temp = Path(file.toString() + ".tmp")
        SystemFileSystem.sink(temp).buffered().use { it.writeString(json.encodeToString(entries.toList())) }

        runCatching { SystemFileSystem.atomicMove(temp, file) }
            .onFailure {
                if (SystemFileSystem.exists(file)) SystemFileSystem.delete(file)
                SystemFileSystem.atomicMove(temp, file)
            }
    }

    private fun StoredCookie.isExpiredAt(now: Long): Boolean =
        cookie.isExpiredAt(now, createdAtMillis)

    private fun Cookie.isExpiredAt(now: Long, createdAtMillis: Long): Boolean {
        val expiresAt = maxAge?.let { createdAtMillis + it * 1_000L } ?: expires?.timestamp
        return expiresAt != null && now >= expiresAt
    }

    private fun Cookie.matchesSafely(url: Url): Boolean =
        domain != null && path != null && matches(url)

    @Serializable
    private data class StoredCookie(
        val cookie: Cookie,
        val createdAtMillis: Long,
    )
}
