package app.miyomu.reader.novel

import app.miyomu.novel.NovelBlock
import app.miyomu.novel.NovelContentSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Drives the novel reader over a [NovelContentSource] (local files or online sources):
 * loads chapter data chapter by chapter, tracks the reading position and reports text and
 * progress keys. Pagination is the UI layer's job.
 */
class NovelReaderController(
    private val contentSource: NovelContentSource,
    private val scope: CoroutineScope,
) {

    data class State(
        val loading: Boolean = true,
        val error: String? = null,
        /** Progress bucket; local books use the library id, online books their own scheme. */
        val itemId: String = "",
        /** Progress key of the current chapter inside [itemId]. */
        val entryId: String = "",
        val bookKey: String = "",
        val chapterCount: Int = 0,
        val chapterIndex: Int = 0,
        val chapterTitle: String? = null,
        /** All chapter titles (null = untitled), for the catalog panel. */
        val chapterTitles: List<String?> = emptyList(),
        val text: String = "",
        /** Content blocks of the current chapter (text paragraphs interleaved with images). */
        val blocks: List<NovelBlock> = emptyList(),
        /** Char offset to restore inside the current chapter; consumed once by the UI. */
        val pendingOffset: Int = 0,
    ) {
        val hasPreviousChapter: Boolean get() = chapterIndex > 0
        val hasNextChapter: Boolean get() = chapterCount > 0 && chapterIndex < chapterCount - 1
    }

    private val mutableState = MutableStateFlow(State())
    val state: StateFlow<State> = mutableState.asStateFlow()

    private var bookKey: String = ""

    fun open(key: String, chapterIndex: Int, charOffset: Int) {
        bookKey = key
        mutableState.value = State(loading = true, bookKey = key)
        scope.launch { loadChapter(key, chapterIndex, charOffset) }
    }

    fun openChapter(index: Int) {
        val key = bookKey
        if (key.isEmpty()) return
        val current = mutableState.value
        if (index < 0 || (current.chapterCount > 0 && index >= current.chapterCount)) return
        // Set synchronously so callers polling [state] never observe a stale chapter as fresh.
        mutableState.value = current.copy(loading = true)
        scope.launch { loadChapter(key, index, 0) }
    }

    fun nextChapter() = openChapter(mutableState.value.chapterIndex + 1)

    fun previousChapter() = openChapter(mutableState.value.chapterIndex - 1)

    /** Mirrors the comic controller's lifecycle hook; nothing to release today. */
    fun close() = Unit

    private suspend fun loadChapter(key: String, index: Int, charOffset: Int) {
        mutableState.value = mutableState.value.copy(loading = true, error = null)
        val data = runCatching { contentSource.load(key, index, charOffset) }
            .getOrElse { failure ->
                return failureState(key, index, failure.message ?: failure.toString())
            }
        val failureMessage = data.error
        if (failureMessage != null) return failureState(key, index, failureMessage)

        mutableState.value = State(
            loading = false,
            error = null,
            itemId = data.itemId,
            entryId = data.entryId,
            bookKey = key,
            chapterCount = data.chapterCount,
            chapterIndex = data.chapterIndex,
            chapterTitle = data.chapterTitle,
            chapterTitles = data.chapterTitles,
            text = data.text,
            blocks = data.blocks,
            pendingOffset = data.pendingOffset,
        )
    }

    private fun failureState(key: String, index: Int, message: String) {
        mutableState.value = mutableState.value.copy(
            loading = false,
            error = message,
            bookKey = key,
            chapterIndex = index,
        )
    }
}
