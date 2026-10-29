package app.miyomu.reader.comic

import app.miyomu.local.LocalBook
import app.miyomu.local.LocalChapter
import app.miyomu.local.ReaderPageSource
import app.miyomu.local.LocalPage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Owns the reading session and loads chapters/pages through [ReaderPageSource].
 * The controller is platform-neutral; Compose only observes [state].
 */
class ComicReaderController(
    private val repository: ReaderPageSource,
    private val scope: CoroutineScope,
) {
    private val mutableState = MutableStateFlow(ComicReaderUiState())
    val state: StateFlow<ComicReaderUiState> = mutableState.asStateFlow()

    private var loadJob: Job? = null

    fun open(book: LocalBook, chapterIndex: Int = 0, pageIndex: Int = 0) {
        loadJob?.cancel()
        loadJob = scope.launch {
            mutableState.update {
                it.copy(
                    book = book,
                    chapters = emptyList(),
                    pages = emptyList(),
                    chapterIndex = 0,
                    pageIndex = 0,
                    loading = true,
                    error = null,
                )
            }

            runCatching {
                val chapters = repository.chapters(book)
                if (chapters.isEmpty()) error("没有可阅读的章节")
                val safeChapter = chapterIndex.coerceIn(chapters.indices)
                val pages = repository.pages(chapters[safeChapter])
                if (pages.isEmpty()) error("本章没有可阅读的页面")
                LoadedChapter(
                    chapters = chapters,
                    chapterIndex = safeChapter,
                    pages = pages,
                    pageIndex = pageIndex.coerceIn(pages.indices),
                )
            }.onSuccess { loaded ->
                mutableState.update {
                    it.copy(
                        chapters = loaded.chapters,
                        chapterIndex = loaded.chapterIndex,
                        pages = loaded.pages,
                        pageIndex = loaded.pageIndex,
                        loading = false,
                        error = null,
                    )
                }
            }.onFailure { failure ->
                mutableState.update {
                    it.copy(
                        loading = false,
                        error = failure.message ?: "无法打开本地文件",
                    )
                }
            }
        }
    }

    fun openChapter(index: Int) {
        val current = mutableState.value
        if (current.book == null || current.chapters.isEmpty()) return
        val target = index.coerceIn(current.chapters.indices)
        if (target == current.chapterIndex && current.pages.isNotEmpty()) return

        loadJob?.cancel()
        loadJob = scope.launch {
            mutableState.update { it.copy(loading = true, error = null) }
            runCatching {
                val pages = repository.pages(current.chapters[target])
                if (pages.isEmpty()) error("本章没有可阅读的页面")
                pages
            }.onSuccess { pages ->
                mutableState.update {
                    it.copy(
                        chapterIndex = target,
                        pages = pages,
                        pageIndex = 0,
                        loading = false,
                        error = null,
                    )
                }
            }.onFailure { failure ->
                mutableState.update {
                    it.copy(
                        loading = false,
                        error = failure.message ?: "无法加载章节",
                    )
                }
            }
        }
    }

    fun nextChapter() {
        val current = mutableState.value
        if (current.hasNextChapter) openChapter(current.chapterIndex + 1)
    }

    fun previousChapter() {
        val current = mutableState.value
        if (current.hasPreviousChapter) openChapter(current.chapterIndex - 1)
    }

    fun setPage(index: Int) {
        mutableState.update { state ->
            if (state.pages.isEmpty()) state
            else state.copy(pageIndex = index.coerceIn(state.pages.indices))
        }
    }

    fun setMode(mode: ComicReadingMode) {
        mutableState.update { it.copy(mode = mode) }
    }

    fun setDualPage(enabled: Boolean) {
        mutableState.update { it.copy(dualPage = enabled) }
    }

    fun setFirstPageAlone(alone: Boolean) {
        mutableState.update { it.copy(firstPageAlone = alone) }
    }

    fun retry() {
        val current = mutableState.value
        val book = current.book ?: return
        open(book, current.chapterIndex, current.pageIndex)
    }

    fun close() {
        loadJob?.cancel()
    }

    private data class LoadedChapter(
        val chapters: List<LocalChapter>,
        val chapterIndex: Int,
        val pages: List<LocalPage>,
        val pageIndex: Int,
    )
}
