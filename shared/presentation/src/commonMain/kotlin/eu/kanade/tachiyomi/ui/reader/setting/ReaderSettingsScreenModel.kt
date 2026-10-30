package eu.kanade.tachiyomi.ui.reader.setting

import eu.kanade.tachiyomi.ui.reader.ReaderViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * CMP port: verbatim except the Injekt default — ReaderPreferences is injected explicitly.
 * The scope is injected instead of a Voyager `screenModelScope`; the caller owns the lifetime.
 */
class ReaderSettingsScreenModel(
    readerState: StateFlow<ReaderViewModel.State>,
    val hasDisplayCutout: Boolean,
    val onChangeReadingMode: (ReadingMode) -> Unit,
    val onChangeOrientation: (ReaderOrientation) -> Unit,
    val preferences: ReaderPreferences,
    scope: CoroutineScope,
) {

    val viewerFlow = readerState
        .map { it.viewer }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.Lazily, null)

    val mangaFlow = readerState
        .map { it.manga }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.Lazily, null)
}
