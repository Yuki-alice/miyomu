package app.miyomu.feature.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.miyomu.core.data.ContentRepository
import app.miyomu.core.data.LibraryRepository
import app.miyomu.core.model.ChapterId
import app.miyomu.core.model.ContentId
import app.miyomu.core.model.Outcome
import app.miyomu.core.model.PageSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class ReaderUiState(
    val pages: List<PageSummary> = emptyList(),
    val initialPage: Int = 0,
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
                    val start = progress
                        ?.takeIf { it.chapterId == chapter }
                        ?.page
                        ?.coerceIn(0, (result.value.size - 1).coerceAtLeast(0))
                        ?: 0
                    _state.value = ReaderUiState(
                        pages = result.value,
                        initialPage = start,
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
