package eu.kanade.tachiyomi.ui.reader.model

import eu.kanade.tachiyomi.ui.reader.loader.PageLoader
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * CMP port: holds the domain Chapter directly (Aniyomi's DB Chapter layer is replaced by
 * Miyomu's Room-backed models).
 */
data class ReaderChapter(val chapter: tachiyomi.domain.items.chapter.model.Chapter) {

    val stateFlow = MutableStateFlow<State>(State.Wait)
    var state: State
        get() = stateFlow.value
        set(value) {
            stateFlow.value = value
        }

    val pages: List<ReaderPage>?
        get() = (state as? State.Loaded)?.pages

    var pageLoader: PageLoader? = null

    var requestedPage: Int = 0

    private var references = 0

    fun ref() {
        references++
    }

    fun unref() {
        references--
        if (references == 0) {
            pageLoader?.recycle()
            pageLoader = null
            state = State.Wait
        }
    }

    sealed interface State {
        data object Wait : State

        data object Loading : State

        data class Error(val error: Throwable) : State

        data class Loaded(val pages: List<ReaderPage>) : State
    }
}
