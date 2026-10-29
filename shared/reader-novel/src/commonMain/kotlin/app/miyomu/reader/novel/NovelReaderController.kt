package app.miyomu.reader.novel

import app.miyomu.local.LocalBook
import app.miyomu.novel.LocalNovelRepository
import app.miyomu.novel.NovelParseException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Drives the novel reader: parses the book once through [LocalNovelRepository] (which caches),
 * then serves chapter text chapter by chapter. Pagination and char-offset tracking are the
 * UI layer's job; this class only moves between chapters and reports text.
 */
class NovelReaderController(
    private val novels: LocalNovelRepository,
    private val scope: CoroutineScope,
) {

    data class State(
        val loading: Boolean = true,
        val error: String? = null,
        val book: LocalBook? = null,
        val chapterCount: Int = 0,
        val chapterIndex: Int = 0,
        val chapterTitle: String? = null,
        /** All chapter titles (null = untitled), for the catalog panel. */
        val chapterTitles: List<String?> = emptyList(),
        val text: String = "",
        /** Char offset to restore inside the current chapter; consumed once by the UI. */
        val pendingOffset: Int = 0,
    ) {
        val hasPreviousChapter: Boolean get() = chapterIndex > 0
        val hasNextChapter: Boolean get() = chapterCount > 0 && chapterIndex < chapterCount - 1
    }

    private val mutableState = MutableStateFlow(State())
    val state: StateFlow<State> = mutableState.asStateFlow()

    private var book: LocalBook? = null

    fun open(book: LocalBook, chapterIndex: Int, charOffset: Int) {
        this.book = book
        mutableState.value = State(loading = true)
        scope.launch {
            loadChapter(book, chapterIndex.coerceAtLeast(0), charOffset.coerceAtLeast(0))
        }
    }

    fun openChapter(index: Int) {
        val currentBook = book ?: return
        val current = mutableState.value
        if (index < 0 || (current.chapterCount > 0 && index >= current.chapterCount)) return
        // Set synchronously so callers polling [state] never observe a stale chapter as fresh.
        mutableState.value = current.copy(loading = true)
        scope.launch { loadChapter(currentBook, index, 0) }
    }

    fun nextChapter() = openChapter(mutableState.value.chapterIndex + 1)

    fun previousChapter() = openChapter(mutableState.value.chapterIndex - 1)

    /** Mirrors the comic controller's lifecycle hook; nothing to release today. */
    fun close() = Unit

    private suspend fun loadChapter(book: LocalBook, index: Int, charOffset: Int) {
        mutableState.value = mutableState.value.copy(loading = true, error = null)
        runCatching {
            val parsed = novels.parse(book)
            val chapters = parsed.chapters
            val safeIndex = index.coerceIn(0, chapters.lastIndex.coerceAtLeast(0))
            val chapter = chapters.getOrNull(safeIndex)
            State(
                loading = false,
                book = book,
                chapterCount = chapters.size,
                chapterIndex = safeIndex,
                chapterTitle = chapter?.title,
                chapterTitles = chapters.map { it.title },
                text = chapter?.text().orEmpty(),
                pendingOffset = if (safeIndex == index) charOffset else 0,
            )
        }.onSuccess { mutableState.value = it }
            .onFailure { failure ->
                val message = when (failure) {
                    is NovelParseException -> failure.message ?: failure.toString()
                    else -> failure.message ?: failure.toString()
                }
                mutableState.value = mutableState.value.copy(loading = false, error = message)
            }
    }
}
