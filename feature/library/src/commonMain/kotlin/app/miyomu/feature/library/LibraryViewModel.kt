package app.miyomu.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.miyomu.core.data.LibraryRepository
import app.miyomu.core.model.ContentSummary
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class LibraryViewModel(
    libraryRepository: LibraryRepository,
) : ViewModel() {
    val favourites: StateFlow<List<ContentSummary>> = libraryRepository
        .observeFavourites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
