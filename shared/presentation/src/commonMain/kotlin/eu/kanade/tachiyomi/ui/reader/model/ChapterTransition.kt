package eu.kanade.tachiyomi.ui.reader.model

/**
 * CMP port: Aniyomi keeps this in `ui.reader.model` and pairs it with a SQLDelight `history` row
 * to resolve the previous/next chapter. Miyomu has no history table for the reader yet, so the
 * transition only carries the two [ReaderChapter]s and the screen derives everything from them.
 */
sealed class ChapterTransition {

    abstract val from: ReaderChapter
    abstract val to: ReaderChapter?

    data class Prev(
        override val from: ReaderChapter,
        override val to: ReaderChapter?,
    ) : ChapterTransition()

    data class Next(
        override val from: ReaderChapter,
        override val to: ReaderChapter?,
    ) : ChapterTransition()
}
