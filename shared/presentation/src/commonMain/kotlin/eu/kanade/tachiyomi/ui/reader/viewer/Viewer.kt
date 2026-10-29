package eu.kanade.tachiyomi.ui.reader.viewer

import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ViewerChapters

/**
 * CMP port: the Android View viewer interface is reduced to the surface the ported chrome
 * needs; the actual viewer is Miyomu's Compose implementation in ComicReaderScreen.
 */
interface Viewer {
    fun setChapters(chapters: ViewerChapters)

    fun moveToPage(page: ReaderPage)
}

abstract class PagerViewer : Viewer

class L2RPagerViewer : PagerViewer() {
    override fun setChapters(chapters: ViewerChapters) = Unit
    override fun moveToPage(page: ReaderPage) = Unit
}

class R2LPagerViewer : PagerViewer() {
    override fun setChapters(chapters: ViewerChapters) = Unit
    override fun moveToPage(page: ReaderPage) = Unit
}

class VerticalPagerViewer : PagerViewer() {
    override fun setChapters(chapters: ViewerChapters) = Unit
    override fun moveToPage(page: ReaderPage) = Unit
}

class WebtoonViewer : Viewer {
    override fun setChapters(chapters: ViewerChapters) = Unit
    override fun moveToPage(page: ReaderPage) = Unit
}
