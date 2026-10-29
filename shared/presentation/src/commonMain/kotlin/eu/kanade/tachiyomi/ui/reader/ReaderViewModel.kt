package eu.kanade.tachiyomi.ui.reader

import eu.kanade.tachiyomi.ui.reader.model.ViewerChapters
import eu.kanade.tachiyomi.ui.reader.viewer.Viewer
import tachiyomi.domain.entries.manga.model.Manga

/**
 * CMP port stub: only the state shape consumed by the ported reader chrome; the full
 * reader view-model logic lives in Miyomu's ComicReaderController integration.
 */
class ReaderViewModel {

    data class State(
        val manga: Manga? = null,
        val viewerChapters: ViewerChapters? = null,
        val bookmarked: Boolean = false,
        val isLoadingAdjacentChapter: Boolean = false,
        val currentPage: Int = -1,
        val viewer: Viewer? = null,
        val menuVisible: Boolean = false,
    ) {
        val currentChapter
            get() = viewerChapters?.currChapter

        val totalPages: Int
            get() = currentChapter?.pages?.size ?: -1
    }
}
