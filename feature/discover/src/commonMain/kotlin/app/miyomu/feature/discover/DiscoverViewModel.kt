package app.miyomu.feature.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.miyomu.core.data.ContentRepository
import app.miyomu.core.model.AppError
import app.miyomu.core.model.ContentSummary
import app.miyomu.core.model.Outcome
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DiscoverUiState(
    val items: List<ContentSummary> = emptyList(),
    val loading: Boolean = true,
    val error: AppError? = null,
)

class DiscoverViewModel(
    private val repository: ContentRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(DiscoverUiState())
    val state: StateFlow<DiscoverUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            when (val result = repository.browse()) {
                is Outcome.Success -> _state.value = DiscoverUiState(items = result.value, loading = false)
                is Outcome.Failure -> _state.value = DiscoverUiState(loading = false, error = result.error)
            }
        }
    }
}
