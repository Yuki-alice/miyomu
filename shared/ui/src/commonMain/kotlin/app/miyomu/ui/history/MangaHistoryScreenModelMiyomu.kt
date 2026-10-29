package app.miyomu.ui.history

import app.miyomu.model.ReadingProgress
import app.miyomu.ui.MiyomuGraph
import eu.kanade.presentation.history.manga.MangaHistoryDialog
import eu.kanade.presentation.history.manga.MangaHistoryUiModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import tachiyomi.domain.entries.manga.model.MangaCover
import tachiyomi.domain.history.manga.model.MangaHistoryWithRelations
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Miyomu binding for the ported history screen: composes [MangaHistoryWithRelations] rows from
 * the progress table, the local library and the chapter marks table (the upstream model reads a
 * dedicated history query). Also owns the delete dialog state, like the upstream screen model.
 */
class MangaHistoryScreenModelMiyomu(
    private val graph: MiyomuGraph,
    private val scope: kotlinx.coroutines.CoroutineScope,
) {
    data class State(
        val list: List<MangaHistoryUiModel>? = null,
        val dialog: MangaHistoryDialog? = null,
        val query: String? = null,
    )

    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()

    private val query = MutableStateFlow<String?>(null)
    private val historySource = MutableStateFlow<List<MangaHistoryWithRelations>?>(null)
    private val refreshMutex = Mutex()
    private var reloadRequest = 0

    /** Long history ids -> raw (itemId, entryId) for resume/delete. */
    private val idIndex = mutableMapOf<Long, Pair<String, String>>()

    init {
        scope.launch {
            @OptIn(FlowPreview::class)
            combine(
                query.debounce(200),
                historySource,
            ) { q, history -> q to history }
                .collect { (q, history) -> publish(q, history) }
        }
        refresh()
    }

    /** Rebuilds the composed history; call after progress changes or on tab entry. */
    fun refresh() {
        reloadRequest++
        val token = reloadRequest
        scope.launch {
            refreshMutex.withLock {
                if (token != reloadRequest) return@withLock
                historySource.value = composeHistory()
            }
        }
    }

    fun search(query: String?) {
        this.query.value = query?.takeIf { it.isNotBlank() }
    }

    fun onDialogChange(dialog: MangaHistoryDialog?) {
        _state.value = _state.value.copy(dialog = dialog)
    }

    fun resolveIds(history: MangaHistoryWithRelations): Pair<String, String>? = idIndex[history.id]

    /** Upstream "remove from history": drops the progress row; reset also clears chapter marks. */
    fun deleteHistory(history: MangaHistoryWithRelations, removeEverything: Boolean) {
        val ids = idIndex[history.id] ?: return
        scope.launch {
            runCatching {
                graph.data.progress.clearEntry(ids.first, ids.second)
                if (removeEverything) {
                    graph.data.chapters.setRead(ids.first, listOf(ids.second), read = false)
                    graph.data.chapters.setBookmarked(ids.first, listOf(ids.second), bookmarked = false)
                }
            }
            refresh()
        }
    }

    private suspend fun publish(queryValue: String?, history: List<MangaHistoryWithRelations>?) {
        val filtered = history
            ?.filter { queryValue == null || it.title.contains(queryValue, ignoreCase = true) }
            ?.sortedByDescending { it.readAt ?: 0L }
            ?.groupBy {
                LocalDate.from(Instant.ofEpochMilli(it.readAt ?: 0L).atZone(ZoneId.systemDefault()))
            }
            ?.flatMap { (date, entries) ->
                buildList {
                    add(MangaHistoryUiModel.Header(date))
                    entries.forEach { add(MangaHistoryUiModel.Item(it)) }
                }
            }
        _state.value = _state.value.copy(list = filtered)
    }

    /**
     * Progress rows joined with titles/covers. Local books resolve from the library scan; online
     * entries appear once added to the library (covers/favorite flag follow Aniyomi's history).
     */
    private suspend fun composeHistory(): List<MangaHistoryWithRelations> {
        val progressRows: List<ReadingProgress> = graph.data.progress.observeAll().first()
        val library = graph.data.library.observeLibrary().first().associateBy { it.id }
        val books = graph.library.books.value.associateBy { "local:${it.id}" }
        val recordsByItem = mutableMapOf<String, Map<String, app.miyomu.data.ChapterRecord>>()
        progressRows.map { it.itemId }.distinct().forEach { itemId ->
            recordsByItem[itemId] = runCatching { graph.data.chapters.chapters(itemId) }
                .getOrDefault(emptyList())
                .associateBy { it.key }
        }

        val index = mutableMapOf<Long, Pair<String, String>>()
        val result = progressRows.mapNotNull { row ->
            val localBook = books[row.itemId]
            val libraryItem = library[row.itemId]
            val title = localBook?.title ?: libraryItem?.title ?: return@mapNotNull null
            val record = recordsByItem[row.itemId]?.get(row.entryId)
            val historyId = "${row.itemId}:${row.entryId}".hashCode().toLong()
            index[historyId] = row.itemId to row.entryId
            MangaHistoryWithRelations(
                id = historyId,
                chapterId = row.entryId.hashCode().toLong() and 0xFFFFFFFFL,
                mangaId = row.itemId.hashCode().toLong() and 0xFFFFFFFFL,
                title = title,
                chapterNumber = record?.chapterNumber ?: 1.0,
                readAt = row.updatedAt,
                readDuration = row.duration?.toLong() ?: 0L,
                coverData = MangaCover(
                    mangaId = row.itemId.hashCode().toLong() and 0xFFFFFFFFL,
                    sourceId = 0L,
                    isMangaFavorite = libraryItem != null,
                    url = localBook?.let { null } ?: libraryItem?.coverUrl,
                    lastModified = 0L,
                ),
            )
        }
        synchronized(idIndex) { idIndex.clear(); idIndex.putAll(index) }
        return result
    }
}
