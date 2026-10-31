package app.miyomu.feature.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.miyomu.core.data.ContentRepository
import app.miyomu.core.model.AppError
import app.miyomu.core.model.ContentSummary
import app.miyomu.core.model.Outcome
import app.miyomu.core.model.SourceOption
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DiscoverUiState(
    val sources: List<SourceOption> = emptyList(),
    val selectedSource: String? = null,
    val query: String = "",
    val items: List<ContentSummary> = emptyList(),
    val loading: Boolean = true,
    val error: AppError? = null,
)

class DiscoverViewModel(
    private val repository: ContentRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(DiscoverUiState())
    val state: StateFlow<DiscoverUiState> = _state.asStateFlow()
    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            val sources = repository.sources()
            _state.update {
                it.copy(
                    sources = sources,
                    selectedSource = it.selectedSource ?: sources.firstOrNull()?.name,
                )
            }
            refresh()
        }
    }

    fun selectSource(name: String) {
        if (_state.value.selectedSource == name) return
        _state.update { it.copy(selectedSource = name, loading = true) }
        viewModelScope.launch {
            repository.selectSource(name)
            refresh()
        }
    }

    fun search(query: String) {
        _state.update { it.copy(query = query) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300)
            refresh()
        }
    }

    fun refresh() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val result = repository.browse(query = _state.value.query)) {
                is Outcome.Success -> _state.update {
                    it.copy(items = result.value, loading = false, error = null)
                }
                is Outcome.Failure -> _state.update {
                    it.copy(loading = false, error = result.error)
                }
            }
        }
    }
}
