package app.miyomu.source.repo

import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable

/**
 * A user-added source repository: an index JSON that points at JS source scripts.
 * Miyomu never ships or recommends a repository (ADR-0003/0013).
 */
@Serializable
data class SourceRepositoryIndex(
    val name: String = "",
    val version: Int = 1,
    val sources: List<SourcePackage> = emptyList(),
)

/** One JS source entry in a repository index. Relative [url]s resolve against the index URL. */
@Serializable
data class SourcePackage(
    val id: String,
    val name: String,
    val version: String,
    val url: String,
    val lang: String = "",
    val sha256: String? = null,
    val description: String? = null,
    val iconUrl: String? = null,
)

/** A source package copied into the local source directory. */
@Serializable
data class InstalledSource(
    val id: String,
    val name: String,
    val version: String,
    val lang: String,
    val scriptPath: String,
    val repositoryUrl: String,
    val sha256: String? = null,
    val description: String? = null,
    val iconUrl: String? = null,
    val installedAt: Long,
    val updatedAt: Long,
)

/** A package offered by one of the configured repositories. */
data class AvailableSource(
    val packageInfo: SourcePackage,
    val repositoryUrl: String,
    val installedVersion: String?,
) {
    val isInstalled: Boolean get() = installedVersion != null
    val updateAvailable: Boolean get() = installedVersion != null && installedVersion != packageInfo.version
}

data class SourceCatalog(
    val available: List<AvailableSource> = emptyList(),
    /** One message per repository that could not be fetched; the rest of the catalog still loads. */
    val errors: List<String> = emptyList(),
)

interface SourceInstallManager {
    val installed: StateFlow<List<InstalledSource>>
    val repositories: StateFlow<List<String>>

    suspend fun ensureLoaded()

    suspend fun addRepository(url: String)

    suspend fun removeRepository(url: String)

    suspend fun fetchCatalog(): SourceCatalog

    suspend fun getById(id: String): InstalledSource?

    /** Installs a new package or replaces an installed one (update). */
    suspend fun install(source: AvailableSource): InstalledSource

    suspend fun uninstall(id: String)
}

/** Source ids double as directory names, so keep them path-safe. */
internal val sourceIdRegex = Regex("""^[a-zA-Z0-9][a-zA-Z0-9._-]*$""")

@Serializable
internal data class SourceManifest(
    val sources: List<InstalledSource> = emptyList(),
    val repositories: List<String> = emptyList(),
)
