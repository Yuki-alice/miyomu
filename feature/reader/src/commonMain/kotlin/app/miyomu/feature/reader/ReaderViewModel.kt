package app.miyomu.feature.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.miyomu.core.data.ContentRepository
import app.miyomu.core.data.LibraryRepository
import app.miyomu.core.model.ChapterId
import app.miyomu.core.model.ContentId
import app.miyomu.core.model.Outcome
import app.miyomu.core.model.PageSummary
import org.skepsun.kototoro.reader.core.PageGeometryHint
import org.skepsun.kototoro.reader.core.PageId
import org.skepsun.kototoro.reader.core.PagedPageSpec
import org.skepsun.kototoro.reader.core.PagedReaderScene
import org.skepsun.kototoro.reader.core.PagedSpreadConfig
import org.skepsun.kototoro.reader.core.SceneReadingDirection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class ReaderUiState(
    val pages: List<PageSummary> = emptyList(),
    val scene: PagedReaderScene? = null,
    val initialSlot: Int = 0,
    val currentPage: Int = 0,
    val mode: ReaderMode = ReaderMode.SINGLE,
    val chapterId: Long = 0L,
    val loading: Boolean = true,
    val failed: Boolean = false,
)

class ReaderViewModel(
    private val contentRepository: ContentRepository,
    private val libraryRepository: LibraryRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(ReaderUiState())
    val state: StateFlow<ReaderUiState> = _state.asStateFlow()

    private var loadedKey: String? = null

    private fun buildScene(
        pages: List<PageSummary>,
        chapterId: Long,
        mode: ReaderMode,
    ): PagedReaderScene = PagedReaderScene(
        viewportWidth = 1000,
        viewportHeight = 1600,
        config = PagedSpreadConfig(
            isDoublePage = mode != ReaderMode.SINGLE,
            readingDirection = if (mode == ReaderMode.RTL) {
                SceneReadingDirection.RIGHT_TO_LEFT
            } else {
                SceneReadingDirection.LEFT_TO_RIGHT
            },
        ),
        initialSpecs = pages.mapIndexed { index, page ->
            PagedPageSpec(
                pageId = PageId(page.id),
                geometryHint = PageGeometryHint.Estimated(0.7f),
                chapterId = chapterId,
                chapterPageIndex = index,
            )
        },
    )

    fun load(sourceName: String, contentId: String, chapterId: String) {
        val key = "$sourceName/$contentId/$chapterId"
        if (loadedKey == key) return
        loadedKey = key
        _state.value = ReaderUiState(loading = true)
        viewModelScope.launch {
            val content = ContentId(contentId.toLongOrNull() ?: -1L)
            val chapter = ChapterId(chapterId.toLongOrNull() ?: -1L)
            when (val result = contentRepository.pages(sourceName, content, chapter)) {
                is Outcome.Success -> {
                    val progress = libraryRepository.observeProgress(content).first()
                    val startPage = progress
                        ?.takeIf { it.chapterId == chapter }
                        ?.page
                        ?.coerceIn(0, (result.value.size - 1).coerceAtLeast(0))
                        ?: 0
                    val scene = buildScene(
                        pages = result.value,
                        chapterId = chapter.value,
                        mode = ReaderMode.SINGLE,
                    )
                    val initialSlot = scene
                        .slotIndexOf(PageId(result.value[startPage].id))
                        .coerceAtLeast(0)
                    _state.value = ReaderUiState(
                        pages = result.value,
                        scene = scene,
                        initialSlot = initialSlot,
                        currentPage = startPage,
                        chapterId = chapter.value,
                        loading = false,
                    )
                }
                is Outcome.Failure -> _state.value = ReaderUiState(loading = false, failed = true)
            }
        }
    }

    fun setMode(mode: ReaderMode) {
        val state = _state.value
        if (state.pages.isEmpty() || state.mode == mode) return
        val scene = buildScene(state.pages, state.chapterId, mode)
        val page = state.pages.getOrNull(state.currentPage) ?: state.pages.first()
        val slot = scene.slotIndexOf(PageId(page.id)).coerceAtLeast(0)
        _state.value = state.copy(
            scene = scene,
            mode = mode,
            initialSlot = slot,
        )
    }

    fun saveProgress(contentId: String, chapterId: String, page: Int, percent: Float) {
        _state.update { it.copy(currentPage = page) }
        viewModelScope.launch {
            libraryRepository.saveProgress(
                contentId = ContentId(contentId.toLongOrNull() ?: -1L),
                chapterId = ChapterId(chapterId.toLongOrNull() ?: -1L),
                chapterTitle = "Chapter",
                page = page,
                scroll = 0f,
                percent = percent,
            )
        }
    }
}
