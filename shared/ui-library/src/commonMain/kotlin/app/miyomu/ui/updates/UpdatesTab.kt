@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package app.miyomu.ui.updates

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import app.miyomu.ui.MiyomuGraph
import eu.kanade.presentation.updates.manga.MangaUpdateScreen
import eu.kanade.presentation.updates.manga.MangaUpdatesItem
import eu.kanade.presentation.updates.manga.MangaUpdatesState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import tachiyomi.domain.entries.manga.model.MangaCover
import tachiyomi.domain.updates.manga.model.MangaUpdatesWithRelations

/**
 * Updates tab backed by the ported Aniyomi `MangaUpdateScreen` (ADR-0011 pattern): rows are
 * composed from the `library_updates` feed joined with the library and chapter marks.
 */
@Composable
fun UpdatesTab(
    graph: MiyomuGraph,
    onOpen: (itemId: String) -> Unit,) {



        val scope = rememberCoroutineScope()
        val model = remember(graph) { MiyomuUpdatesScreenModel(graph, scope) }
        val state by model.state.collectAsState()
        val checkState by graph.libraryUpdater.state.collectAsState()

        MangaUpdateScreen(
            state = MangaUpdatesState(isLoading = state.isLoading, items = state.items),
            snackbarHostState = remember { SnackbarHostState() },
            lastUpdated = graph.preferenceStore.getLong("updates_last_fetched", 0L).get(),
            onClickCover = { item -> onOpen(item.itemId) },
            onSelectAll = model::selectAll,
            onInvertSelection = model::invertSelection,
            onUpdateLibrary = {
                if (checkState.running) false else {
                    graph.libraryUpdater.checkNow()
                    true
                }
            },
            onMultiBookmarkClicked = model::onMultiBookmarkClicked,
            onMultiMarkAsReadClicked = model::onMultiMarkAsReadClicked,
            onMultiDeleteClicked = model::onMultiDeleteClicked,
            onUpdateSelected = model::onUpdateSelected,
            onOpenChapter = { item -> onOpen(item.itemId) },
        )
    }


/** Miyomu binding for the ported updates screen: composition, selection and mark actions. */
class MiyomuUpdatesScreenModel(
    private val graph: MiyomuGraph,
    private val scope: kotlinx.coroutines.CoroutineScope,
) {
    data class State(
        val isLoading: Boolean = true,
        val items: List<MangaUpdatesItem> = emptyList(),
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private val idIndex = mutableMapOf<Long, Pair<String, String>>()

    init {
        refresh()
    }

    fun refresh() {
        scope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val rows = graph.data.libraryUpdates.observeUpdates().first()
            val library = graph.data.library.observeLibrary().first().associateBy { it.id }
            val items = rows.mapNotNull { row ->
                val item = library[row.itemId] ?: return@mapNotNull null
                val marks = runCatching { graph.data.chapters.chapters(row.itemId) }
                    .getOrDefault(emptyList())
                    .associateBy { it.key }[row.key]
                val progress = runCatching { graph.data.progress.latestFor(row.itemId, row.key) }.getOrNull()
                val historyId = "${row.itemId}:${row.key}".hashCode().toLong()
                synchronized(idIndex) { idIndex[historyId] = row.itemId to row.key }
                MangaUpdatesItem(
                    update = MangaUpdatesWithRelations(
                        mangaId = row.itemId.hashCode().toLong() and 0xFFFFFFFFL,
                        mangaTitle = item.title,
                        chapterId = row.key.hashCode().toLong() and 0xFFFFFFFFL,
                        chapterName = row.name,
                        scanlator = null,
                        read = marks?.read ?: false,
                        bookmark = marks?.bookmark ?: false,
                        lastPageRead = (progress?.position ?: 0.0).toLong(),
                        sourceId = item.sourceId.hashCode().toLong(),
                        dateFetch = row.fetchedAt,
                        coverData = MangaCover(
                            mangaId = row.itemId.hashCode().toLong() and 0xFFFFFFFFL,
                            sourceId = 0L,
                            isMangaFavorite = true,
                            url = item.coverUrl,
                            lastModified = 0L,
                        ),
                    ),
                    itemId = row.itemId,
                    entryKey = row.key,
                )
            }
            _state.value = State(isLoading = false, items = items)
        }
    }

    fun selectAll(selected: Boolean) {
        _state.value = _state.value.copy(items = _state.value.items.map { it.copy(selected = selected) })
    }

    fun invertSelection() {
        _state.value = _state.value.copy(items = _state.value.items.map { it.copy(selected = !it.selected) })
    }

    fun onUpdateSelected(item: MangaUpdatesItem, selected: Boolean, defaultValue: Boolean, longPress: Boolean) {
        _state.value = _state.value.copy(
            items = _state.value.items.map {
                if (it.update.chapterId == item.update.chapterId) it.copy(selected = selected) else it
            },
        )
    }

    fun onMultiBookmarkClicked(selected: List<MangaUpdatesItem>, bookmarked: Boolean) {
        applyMarks(selected) { itemId, keys -> graph.data.chapters.setBookmarked(itemId, keys, bookmarked) }
    }

    fun onMultiMarkAsReadClicked(selected: List<MangaUpdatesItem>, read: Boolean) {
        applyMarks(selected) { itemId, keys -> graph.data.chapters.setRead(itemId, keys, read) }
    }

    fun onMultiDeleteClicked(selected: List<MangaUpdatesItem>) {
        scope.launch {
            selected.forEach { graph.data.libraryUpdates.deleteUpdate(it.itemId, it.entryKey) }
            refresh()
        }
    }

    private fun applyMarks(
        selected: List<MangaUpdatesItem>,
        action: suspend (String, List<String>) -> Unit,
    ) {
        scope.launch {
            selected.groupBy { it.itemId }.forEach { (itemId, rows) ->
                action(itemId, rows.map { it.entryKey })
            }
            refresh()
        }
    }
}

