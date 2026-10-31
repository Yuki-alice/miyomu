package app.miyomu.feature.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.miyomu.core.data.ContentRepository
import app.miyomu.core.data.LibraryRepository
import app.miyomu.core.model.ContentDetails
import app.miyomu.core.model.ContentId
import app.miyomu.core.model.Outcome
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DetailsUiState(
    val details: ContentDetails? = null,
    val loading: Boolean = true,
    val failed: Boolean = false,
    val isFavourite: Boolean = false,
)

class DetailsViewModel(
    private val contentRepository: ContentRepository,
    private val libraryRepository: LibraryRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(DetailsUiState())
    val state: StateFlow<DetailsUiState> = _state.asStateFlow()

    private var contentId: ContentId? = null

    init {
        viewModelScope.launch {
            libraryRepository.observeFavourites().collect { favourites ->
                val current = contentId ?: return@collect
                _state.update { it.copy(isFavourite = favourites.any { row -> row.id == current }) }
            }
        }
    }

    fun load(contentId: String) {
        val id = ContentId(contentId.toLongOrNull() ?: -1L)
        if (this.contentId == id && _state.value.details != null) return
        this.contentId = id
        _state.value = _state.value.copy(loading = true, failed = false)
        viewModelScope.launch {
            when (val result = contentRepository.details(id)) {
                is Outcome.Success -> {
                    val favourite = libraryRepository.observeFavourites().first()
                        .any { row -> row.id == id }
                    _state.value = DetailsUiState(
                        details = result.value,
                        loading = false,
                        failed = false,
                        isFavourite = favourite,
                    )
                }
                is Outcome.Failure -> _state.value = DetailsUiState(loading = false, failed = true)
            }
        }
    }

    fun toggleFavourite() {
        val summary = _state.value.details?.summary ?: return
        viewModelScope.launch {
            libraryRepository.setFavourite(summary, !_state.value.isFavourite)
        }
    }
}
