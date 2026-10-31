package app.miyomu.core.source

import org.skepsun.kototoro.core.source.SourceRef
import org.skepsun.kototoro.core.source.SourceRuntime
import org.skepsun.kototoro.core.source.SourceUnavailableException

/**
 * Minimal registry for the first data-flow slice. A later P1 step replaces it with
 * the persisted source list and extension-aware registry.
 */
class SourceRegistry(private val runtimes: List<SourceRuntime>) {
    private var cachedSources: List<SourceRef>? = null

    suspend fun sources(): List<SourceRef> {
        cachedSources?.let { return it }
        val loaded = runtimes.flatMap { it.getSources() }.distinctBy { it.name }
        cachedSources = loaded
        return loaded
    }

    suspend fun primary(): SourceRuntime = runtimes.firstOrNull()
        ?: throw SourceUnavailableException("none")

    suspend fun runtime(sourceName: String): SourceRuntime {
        val sources = sources()
        if (sources.none { it.name == sourceName }) throw SourceUnavailableException(sourceName)
        return runtimes.firstOrNull { runtime ->
            runtime.getSources().any { it.name == sourceName }
        } ?: throw SourceUnavailableException(sourceName)
    }
}
