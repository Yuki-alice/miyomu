package eu.kanade.tachiyomi.ui.entries.manga

import androidx.compose.runtime.Immutable
import androidx.compose.ui.util.fastAny
import cafe.adriel.voyager.core.model.ScreenModel
import eu.kanade.core.util.insertSeparators
import eu.kanade.domain.entries.manga.model.isLocal
import eu.kanade.tachiyomi.data.download.manga.model.MangaDownload
import eu.kanade.tachiyomi.source.MangaSource
import kotlinx.collections.immutable.ImmutableList
import tachiyomi.core.common.preference.CheckboxState
import tachiyomi.core.common.preference.TriState
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.entries.applyFilter
import tachiyomi.domain.entries.manga.model.Manga
import tachiyomi.domain.items.chapter.model.Chapter
import tachiyomi.domain.items.chapter.service.calculateChapterGap
import tachiyomi.domain.items.chapter.service.getChapterSort
import kotlin.math.floor

/**
 * CMP port: only the state/dialog/chapter-list types from Aniyomi's 1243-line screen model
 * are kept here verbatim; the action surface is implemented by Miyomu's adapted screen model.
 */
open class MangaScreenModel : ScreenModel {

    sealed interface Dialog {
        data class ChangeCategory(
            val manga: Manga,
            val initialSelection: ImmutableList<CheckboxState<Category>>,
        ) : Dialog

        data class DeleteChapters(val chapters: List<Chapter>) : Dialog

        data class DuplicateManga(val manga: Manga, val duplicate: Manga) : Dialog

        data class Migrate(val newManga: Manga, val oldManga: Manga) : Dialog

        data class SetMangaFetchInterval(val manga: Manga) : Dialog

        data object SettingsSheet : Dialog

        data object TrackSheet : Dialog

        data object FullCover : Dialog
    }

    sealed interface State {
        @Immutable
        data object Loading : State

        @Immutable
        data class Success(
            val manga: Manga,
            val source: MangaSource,
            val isFromSource: Boolean,
            val chapters: List<ChapterList.Item>,
            val availableScanlators: Set<String>,
            val excludedScanlators: Set<String>,
            val trackingCount: Int = 0,
            val hasLoggedInTrackers: Boolean = false,
            val isRefreshingData: Boolean = false,
            val dialog: Dialog? = null,
            val hasPromptedToAddBefore: Boolean = false,
        ) : State {
            val processedChapters by lazy {
                chapters.applyFilters(manga).toList()
            }

            val isAnySelected by lazy {
                chapters.fastAny { it.selected }
            }

            val chapterListItems by lazy {
                processedChapters.insertSeparators { before, after ->
                    val (lowerChapter, higherChapter) = if (manga.sortDescending()) {
                        after to before
                    } else {
                        before to after
                    }
                    if (higherChapter == null) return@insertSeparators null

                    if (lowerChapter == null) {
                        floor(higherChapter.chapter.chapterNumber)
                            .toInt()
                            .minus(1)
                            .coerceAtLeast(0)
                    } else {
                        calculateChapterGap(higherChapter.chapter, lowerChapter.chapter)
                    }
                        .takeIf { it > 0 }
                        ?.let { missingCount ->
                            ChapterList.MissingCount(
                                id = "${lowerChapter?.id}-${higherChapter.id}",
                                count = missingCount,
                            )
                        }
                }
            }

            val scanlatorFilterActive: Boolean
                get() = excludedScanlators.intersect(availableScanlators).isNotEmpty()

            val filterActive: Boolean
                get() = scanlatorFilterActive || manga.chaptersFiltered()

            val trackingAvailable: Boolean
                get() = trackingCount > 0

            /**
             * Applies the view filters to the list of chapters obtained from the database.
             * @return an observable of the list of chapters filtered and sorted.
             */
            private fun List<ChapterList.Item>.applyFilters(manga: Manga): Sequence<ChapterList.Item> {
                val isLocalManga = manga.isLocal()
                val unreadFilter = manga.unreadFilter
                val downloadedFilter = manga.downloadedFilter
                val bookmarkedFilter = manga.bookmarkedFilter
                return asSequence()
                    .filter { (chapter) -> applyFilter(unreadFilter) { !chapter.read } }
                    .filter { (chapter) -> applyFilter(bookmarkedFilter) { chapter.bookmark } }
                    .filter { applyFilter(downloadedFilter) { it.isDownloaded || isLocalManga } }
                    .sortedWith { (chapter1), (chapter2) ->
                        getChapterSort(manga).invoke(
                            chapter1,
                            chapter2,
                        )
                    }
            }
        }
    }
}

@Immutable
sealed class ChapterList {
    @Immutable
    data class MissingCount(
        val id: String,
        val count: Int,
    ) : ChapterList()

    @Immutable
    data class Item(
        val chapter: Chapter,
        val downloadState: MangaDownload.State,
        val downloadProgress: Int,
        val selected: Boolean = false,
    ) : ChapterList() {
        val id = chapter.id
        val isDownloaded = downloadState == MangaDownload.State.DOWNLOADED
    }
}
