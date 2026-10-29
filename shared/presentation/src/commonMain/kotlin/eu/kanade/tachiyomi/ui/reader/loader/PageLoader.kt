package eu.kanade.tachiyomi.ui.reader.loader

/**
 * CMP port stub of Aniyomi's PageLoader: the concrete loaders (Directory/Archive/Http) are
 * implemented by Miyomu's LocalComicRepository integration; only the lifecycle surface
 * needed by ReaderChapter is declared here.
 */
abstract class PageLoader {

    /**
     * Called when the chapter is no longer being displayed; subclasses release resources.
     */
    open fun recycle() {
    }
}
