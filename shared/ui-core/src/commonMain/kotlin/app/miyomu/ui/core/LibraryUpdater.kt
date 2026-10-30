package app.miyomu.ui.core

import app.miyomu.contract.ChapterRecord
import app.miyomu.contract.LibraryUpdate
import app.miyomu.model.LibraryItem
import app.miyomu.model.LibraryStatus
import app.miyomu.model.MediaType
import app.miyomu.ui.MiyomuGraph
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.launch
import app.miyomu.model.Category

/**
 * Miyomu's library update check (Aniyomi's LibraryUpdateChecker): walks the online entries in
 * the library, re-fetches their details through the source runtime, replaces the stored chapter
 * list (marks survive the replace) and records newly discovered chapters into the updates feed.
 */
class LibraryUpdater(
    private val graph: MiyomuGraph,
    private val scope: kotlinx.coroutines.CoroutineScope,
) {

    data class State(
        val running: Boolean = false,
        val checked: Int = 0,
        val total: Int = 0,
        val newChapters: Int = 0,
        val errors: Map<String, String> = emptyMap(),
        val lastFinishedAt: Long? = null,
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()
    private val mutex = Mutex()

    fun checkNow() {
        scope.launch {
            checkNowAndWait()
        }
    }

    /** Synchronous variant used by tests; suspends until the sweep finishes. */
    suspend fun checkNowAndWait() {
        run {
            mutex.withLock {
                if (_state.value.running) return@withLock
                _state.value = State(running = true)
                runCatching { run() }
                    .onSuccess { result ->
                        _state.value = _state.value.copy(
                            running = false,
                            lastFinishedAt = graph.nowMillis(),
                            errors = result,
                        )
                        graph.preferenceStore.getLong("updates_last_fetched", 0L).set(graph.nowMillis())
                    }
                    .onFailure { error ->
                        _state.value = _state.value.copy(
                            running = false,
                            lastFinishedAt = graph.nowMillis(),
                            errors = mapOf("check" to (error.message ?: error::class.simpleName.orEmpty())),
                        )
                    }
            }
        }
    }

    /** Returns per-entry failure messages; successful entries record new chapters as a side effect. */
    private suspend fun run(): Map<String, String> {
        val now = graph.nowMillis()
        val online = graph.data.library.observeLibrary().first()
            .filter { it.mediaType == MediaType.COMIC && it.sourceId != "local" }
        _state.value = _state.value.copy(total = online.size, checked = 0, newChapters = 0)

        val errors = mutableMapOf<String, String>()
        var newChapters = 0
        for (item in online) {
            try {
                val oldKeys = graph.data.chapters.chapters(item.id).map { it.key }.toSet()
                val details = graph.sourceRuntime.details(item.sourceId, item.sourceKey)
                val chapters = details.chapters
                graph.data.chapters.replaceChapters(
                    itemId = item.id,
                    chapters = chapters.mapIndexed { index, chapter ->
                        ChapterRecord(
                            key = chapter.id,
                            name = chapter.title,
                            url = chapter.id,
                            sourceOrder = index.toLong(),
                        )
                    },
                    fetchedAt = now,
                )
                val discovered = chapters.filter { it.id !in oldKeys }
                graph.data.libraryUpdates.recordUpdates(
                    item.id,
                    discovered.map { chapter ->
                        app.miyomu.contract.LibraryUpdate(
                            itemId = item.id,
                            key = chapter.id,
                            name = chapter.title,
                            chapterNumber = null,
                            fetchedAt = now,
                        )
                    },
                )
                newChapters += discovered.size
                // Refresh the library entry's update timestamp.
                graph.data.library.upsert(item.copy(updatedAt = now, status = LibraryStatus.ONGOING))
            } catch (error: Exception) {
                if (error is kotlinx.coroutines.CancellationException) throw error
                errors[item.title] = error.message ?: error::class.simpleName.orEmpty()
            }
            _state.value = _state.value.copy(checked = _state.value.checked + 1, newChapters = newChapters)
        }
        return errors
    }
}
