package app.miyomu.source.repo

import app.miyomu.network.createMiyomuHttpClient
import app.miyomu.network.cookies.FileCookiesStorage
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import kotlinx.io.write
import kotlinx.io.writeString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.URI
import java.security.MessageDigest

/**
 * File-backed [SourceInstallManager].
 *
 * Layout under [baseDir]: `manifest.json` (installed sources + repository URLs) and
 * `sources/<id>/source.js`. Source ids are validated before they reach the filesystem so a
 * malicious index cannot escape the source directory.
 */
class FileSourceInstallManager(
    private val baseDir: Path,
    private val httpClient: HttpClient = defaultSourceClient(baseDir),
    private val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = true
    },
) : SourceInstallManager {

    private val mutex = Mutex()
    private val _installed = MutableStateFlow<List<InstalledSource>>(emptyList())
    private val _repositories = MutableStateFlow<List<String>>(emptyList())
    override val installed: StateFlow<List<InstalledSource>> = _installed.asStateFlow()
    override val repositories: StateFlow<List<String>> = _repositories.asStateFlow()

    private val manifestPath = Path(baseDir, "manifest.json")
    private val sourcesDir = Path(baseDir, "sources")
    private var loaded = false

    override suspend fun ensureLoaded() {
        mutex.withLock { loadLocked() }
    }

    override suspend fun addRepository(url: String) {
        val normalized = url.trim()
        require(normalized.startsWith("http://") || normalized.startsWith("https://")) {
            "Repository URL must start with http:// or https://"
        }
        mutex.withLock {
            loadLocked()
            if (normalized !in _repositories.value) {
                _repositories.value = _repositories.value + normalized
                writeManifestLocked()
            }
        }
    }

    override suspend fun removeRepository(url: String) {
        mutex.withLock {
            loadLocked()
            if (url in _repositories.value) {
                _repositories.value = _repositories.value - url
                writeManifestLocked()
            }
        }
    }

    override suspend fun getById(id: String): InstalledSource? {
        ensureLoaded()
        return _installed.value.firstOrNull { it.id == id }
    }

    override suspend fun fetchCatalog(): SourceCatalog = withContext(Dispatchers.IO) {
        ensureLoaded()
        val installedById = _installed.value.associateBy { it.id }
        val available = mutableListOf<AvailableSource>()
        val errors = mutableListOf<String>()
        for (repository in _repositories.value) {
            runCatching { fetchIndex(repository) }
                .onSuccess { index ->
                    index.sources.forEach { source ->
                        available += AvailableSource(
                            packageInfo = source,
                            repositoryUrl = repository,
                            installedVersion = installedById[source.id]?.version,
                        )
                    }
                }
                .onFailure { error ->
                    if (error is CancellationException) throw error
                    errors += "${repository}: ${error.message ?: error::class.simpleName}"
                }
        }
        SourceCatalog(available = available, errors = errors)
    }

    override suspend fun install(source: AvailableSource): InstalledSource = withContext(Dispatchers.IO) {
        ensureLoaded()
        val pkg = source.packageInfo
        require(sourceIdRegex.matches(pkg.id)) { "Invalid source id: ${pkg.id}" }
        require(pkg.version.isNotBlank()) { "Source ${pkg.id} has no version" }

        val scriptUrl = resolveUrl(source.repositoryUrl, pkg.url)
        val bytes = fetchBytes(scriptUrl)
        val digest = sha256Hex(bytes)
        val expected = pkg.sha256?.takeIf { it.isNotBlank() }
        require(expected == null || digest.equals(expected, ignoreCase = true)) {
            "sha256 mismatch for ${pkg.id}"
        }

        val now = System.currentTimeMillis()
        mutex.withLock {
            loadLocked()
            val directory = Path(sourcesDir, pkg.id)
            SystemFileSystem.createDirectories(directory)
            val scriptPath = Path(directory, "source.js")
            writeBytesAtomically(scriptPath, bytes)

            val existing = _installed.value.firstOrNull { it.id == pkg.id }
            val record = InstalledSource(
                id = pkg.id,
                name = pkg.name,
                version = pkg.version,
                lang = pkg.lang,
                scriptPath = scriptPath.toString(),
                repositoryUrl = source.repositoryUrl,
                sha256 = digest,
                description = pkg.description,
                iconUrl = pkg.iconUrl,
                installedAt = existing?.installedAt ?: now,
                updatedAt = now,
            )
            _installed.value = _installed.value.filterNot { it.id == pkg.id } + record
            writeManifestLocked()
            record
        }
    }

    override suspend fun uninstall(id: String) = withContext(Dispatchers.IO) {
        require(sourceIdRegex.matches(id)) { "Invalid source id: $id" }
        mutex.withLock {
            loadLocked()
            deleteRecursively(Path(sourcesDir, id))
            if (_installed.value.any { it.id == id }) {
                _installed.value = _installed.value.filterNot { it.id == id }
                writeManifestLocked()
            }
        }
    }

    private suspend fun fetchIndex(indexUrl: String): SourceRepositoryIndex =
        json.decodeFromString(fetchText(indexUrl))

    private suspend fun fetchText(url: String): String {
        val response = httpClient.get(url)
        if (!response.status.isSuccess()) error("HTTP ${response.status.value} for $url")
        return response.bodyAsText()
    }

    private suspend fun fetchBytes(url: String): ByteArray {
        val response = httpClient.get(url)
        if (!response.status.isSuccess()) error("HTTP ${response.status.value} for $url")
        return response.bodyAsBytes()
    }

    private fun loadLocked() {
        if (loaded) return
        SystemFileSystem.createDirectories(baseDir)
        val manifest = runCatching {
            if (!SystemFileSystem.exists(manifestPath)) return@runCatching SourceManifest()
            json.decodeFromString<SourceManifest>(readText(manifestPath))
        }.getOrDefault(SourceManifest())
        _installed.value = manifest.sources
        _repositories.value = manifest.repositories
        loaded = true
    }

    private fun writeManifestLocked() {
        val manifest = SourceManifest(
            sources = _installed.value,
            repositories = _repositories.value,
        )
        writeTextAtomically(manifestPath, json.encodeToString(manifest))
    }
}

private fun defaultSourceClient(baseDir: Path): HttpClient {
    SystemFileSystem.createDirectories(baseDir)
    return createMiyomuHttpClient(FileCookiesStorage(Path(baseDir, "cookies.json")))
}

internal fun resolveUrl(baseUrl: String, relativeOrAbsolute: String): String {
    val absolute = if (relativeOrAbsolute.startsWith("http://") || relativeOrAbsolute.startsWith("https://")) {
        relativeOrAbsolute
    } else {
        URI(baseUrl).resolve(relativeOrAbsolute).toString()
    }
    require(absolute.startsWith("http://") || absolute.startsWith("https://")) {
        "Unsupported URL: $absolute"
    }
    return absolute
}

internal fun sha256Hex(bytes: ByteArray): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
    return buildString(digest.size * 2) {
        digest.forEach { byte ->
            val value = byte.toInt() and 0xFF
            append("0123456789abcdef"[value ushr 4])
            append("0123456789abcdef"[value and 0x0F])
        }
    }
}

private fun readText(path: Path): String =
    SystemFileSystem.source(path).buffered().use { it.readString() }

private fun writeTextAtomically(path: Path, text: String) {
    path.parent?.let { SystemFileSystem.createDirectories(it) }
    val temp = Path(path.toString() + ".tmp")
    SystemFileSystem.sink(temp).buffered().use { it.writeString(text) }
    moveReplacing(temp, path)
}

private fun writeBytesAtomically(path: Path, bytes: ByteArray) {
    path.parent?.let { SystemFileSystem.createDirectories(it) }
    val temp = Path(path.toString() + ".tmp")
    SystemFileSystem.sink(temp).buffered().use { it.write(bytes) }
    moveReplacing(temp, path)
}

private fun moveReplacing(temp: Path, target: Path) {
    runCatching { SystemFileSystem.atomicMove(temp, target) }
        .onFailure {
            if (SystemFileSystem.exists(target)) SystemFileSystem.delete(target)
            SystemFileSystem.atomicMove(temp, target)
        }
}

private fun deleteRecursively(path: Path) {
    if (!SystemFileSystem.exists(path)) return
    if (SystemFileSystem.metadataOrNull(path)?.isDirectory == true) {
        SystemFileSystem.list(path).toList().forEach(::deleteRecursively)
    }
    SystemFileSystem.delete(path, mustExist = false)
}
