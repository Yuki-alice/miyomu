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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class ReaderUiState(
    val pages: List<PageSummary> = emptyList(),
    val scene: PagedReaderScene? = null,
    val initialSlot: Int = 0,
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

    fun load(contentId: String, chapterId: String) {
        val key = "$contentId/$chapterId"
        if (loadedKey == key) return
        loadedKey = key
        _state.value = ReaderUiState(loading = true)
        viewModelScope.launch {
            val content = ContentId(contentId.toLongOrNull() ?: -1L)
            val chapter = ChapterId(chapterId.toLongOrNull() ?: -1L)
            when (val result = contentRepository.pages(content, chapter)) {
                is Outcome.Success -> {
                    val progress = libraryRepository.observeProgress(content).first()
                    val startPage = progress
                        ?.takeIf { it.chapterId == chapter }
                        ?.page
                        ?.coerceIn(0, (result.value.size - 1).coerceAtLeast(0))
                        ?: 0
                    val scene = PagedReaderScene(
                        viewportWidth = 1000,
                        viewportHeight = 1600,
                        config = PagedSpreadConfig(
                            isDoublePage = false,
                            readingDirection = SceneReadingDirection.LEFT_TO_RIGHT,
                        ),
                        initialSpecs = result.value.mapIndexed { index, page ->
                            PagedPageSpec(
                                pageId = PageId(page.id),
                                geometryHint = PageGeometryHint.Estimated(0.7f),
                                chapterId = chapter.value,
                                chapterPageIndex = index,
                            )
                        },
                    )
                    val initialSlot = scene
                        .slotIndexOf(PageId(result.value[startPage].id))
                        .coerceAtLeast(0)
                    _state.value = ReaderUiState(
                        pages = result.value,
                        scene = scene,
                        initialSlot = initialSlot,
                        loading = false,
                    )
                }
                is Outcome.Failure -> _state.value = ReaderUiState(loading = false, failed = true)
            }
        }
    }

    fun saveProgress(contentId: String, chapterId: String, page: Int, percent: Float) {
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
