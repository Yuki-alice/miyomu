package app.miyomu.feature.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.miyomu.core.data.ContentRepository
import app.miyomu.core.model.AppError
import app.miyomu.core.model.ContentFilter
import app.miyomu.core.model.ContentFilterOptions
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
    val filter: ContentFilter = ContentFilter(),
    val filterOptions: ContentFilterOptions = ContentFilterOptions(),
    val showFilter: Boolean = false,
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
            val selected = sources.firstOrNull()?.name
            _state.update { it.copy(sources = sources, selectedSource = selected) }
            if (selected != null) loadFilterOptions(selected)
            refresh()
        }
    }

    fun selectSource(name: String) {
        if (_state.value.selectedSource == name) return
        _state.update { it.copy(selectedSource = name, loading = true, filter = ContentFilter()) }
        viewModelScope.launch {
            loadFilterOptions(name)
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

    fun openFilter() = _state.update { it.copy(showFilter = true) }
    fun closeFilter() = _state.update { it.copy(showFilter = false) }

    fun applyFilter(filter: ContentFilter) {
        _state.update { it.copy(filter = filter, showFilter = false) }
        refresh()
    }

    fun clearFilter() {
        _state.update { it.copy(filter = ContentFilter()) }
        refresh()
    }

    fun refresh() {
        val source = _state.value.selectedSource ?: return
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (
                val result = repository.browse(
                    sourceName = source,
                    query = _state.value.query,
                    filter = _state.value.filter,
                )
            ) {
                is Outcome.Success -> _state.update {
                    it.copy(items = result.value, loading = false, error = null)
                }
                is Outcome.Failure -> _state.update {
                    it.copy(loading = false, error = result.error)
                }
            }
        }
    }

    private suspend fun loadFilterOptions(source: String) {
        when (val result = repository.filterOptions(source)) {
            is Outcome.Success -> _state.update { it.copy(filterOptions = result.value) }
            is Outcome.Failure -> _state.update { it.copy(filterOptions = ContentFilterOptions()) }
        }
    }
}
