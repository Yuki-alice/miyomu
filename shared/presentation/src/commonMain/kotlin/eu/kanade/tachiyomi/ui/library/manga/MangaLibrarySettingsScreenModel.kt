package eu.kanade.tachiyomi.ui.library.manga

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.stateIn
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.TriState
import tachiyomi.core.common.preference.getAndSet
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.library.manga.model.MangaLibrarySort
import tachiyomi.domain.library.model.LibraryDisplayMode
import tachiyomi.domain.library.service.LibraryPreferences
import kotlin.time.Duration.Companion.seconds

/**
 * CMP port: category-flag persistence (per-category display/sort) is deferred to when the
 * category system lands; display mode and sort are stored globally in LibraryPreferences,
 * and tracker filtering is surfaced through an injected (initially empty) trackers flow.
 */
class MangaLibrarySettingsScreenModel(
    val libraryPreferences: LibraryPreferences,
    trackersFlow: Flow<List<Int>> = emptyFlow(),
) : ScreenModel {

    val trackersFlow = trackersFlow.stateIn(
        scope = screenModelScope,
        started = SharingStarted.WhileSubscribed(5.seconds.inWholeMilliseconds),
        initialValue = emptyList(),
    )

    fun toggleFilter(preference: (LibraryPreferences) -> Preference<TriState>) {
        preference(libraryPreferences).getAndSet {
            it.next()
        }
    }

    fun toggleTracker(id: Int) {
        toggleFilter { libraryPreferences.filterTrackedManga(id) }
    }

    fun setDisplayMode(mode: LibraryDisplayMode) {
        libraryPreferences.displayMode().set(mode)
    }

    fun setSort(
        category: Category?,
        mode: MangaLibrarySort.Type,
        direction: MangaLibrarySort.Direction,
    ) {
        screenModelScope.launchIO {
            libraryPreferences.mangaSortingMode().set(MangaLibrarySort(mode, direction))
        }
    }
}
