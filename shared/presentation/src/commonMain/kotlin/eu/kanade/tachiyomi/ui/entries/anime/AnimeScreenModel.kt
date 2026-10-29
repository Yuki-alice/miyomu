package eu.kanade.tachiyomi.ui.entries.anime

import androidx.compose.runtime.Immutable
import eu.kanade.core.util.insertSeparators
import eu.kanade.domain.entries.anime.model.downloadedFilter
import eu.kanade.domain.entries.anime.model.isLocal
import eu.kanade.domain.entries.anime.model.seasonDownloadedFilter
import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.animesource.model.FetchType
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.data.download.anime.model.AnimeDownload
import kotlinx.collections.immutable.ImmutableList
import tachiyomi.core.common.preference.CheckboxState
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.entries.applyFilter
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.domain.entries.anime.model.AnimeRelationGroup
import tachiyomi.domain.items.episode.model.Episode
import tachiyomi.domain.items.episode.service.calculateEpisodeGap
import tachiyomi.domain.items.episode.service.getEpisodeSort
import tachiyomi.domain.items.season.service.getSeasonSortComparator
import tachiyomi.domain.items.season.service.seasonSortAlphabetically
import java.util.Calendar
import kotlin.math.floor

/**
 * CMP port: only the state/dialog/episode-list types from Aniyomi's 1760-line screen model are
 * kept here verbatim; the action surface is implemented by Miyomu's adapted screen model.
 */
open class AnimeScreenModel {

    sealed interface Dialog {
        data class ChangeCategory(
            val anime: Anime,
            val initialSelection: ImmutableList<CheckboxState<Category>>,
        ) : Dialog

        data class DeleteEpisodes(val episodes: List<Episode>) : Dialog
        data class DuplicateAnime(val anime: Anime, val duplicate: Anime) : Dialog
        data class Migrate(val newAnime: Anime, val oldAnime: Anime) : Dialog
        data class SetAnimeFetchInterval(val anime: Anime) : Dialog
        data class ShowQualities(val episode: Episode, val anime: Anime, val source: AnimeSource) : Dialog
        data object ChangeAnimeSkipIntro : Dialog
        data object EpisodeSettingsSheet : Dialog
        data object SeasonSettingsSheet : Dialog
        data object TrackSheet : Dialog
        data object FullImages : Dialog
    }

    sealed interface State {
        @Immutable
        data object Loading : State

        @Immutable
        data class Success(
            val anime: Anime,
            val source: AnimeSource,
            val isFromSource: Boolean,
            val episodes: List<EpisodeList.Item>,
            val seasons: List<AnimeSeasonItem>,
            val trackingCount: Int = 0,
            val hasLoggedInTrackers: Boolean = false,
            val relatedAnime: List<AnimeRelationGroup> = emptyList(),
            val isLoadingRelatedAnime: Boolean = false,
            val isRefreshingData: Boolean = false,
            val dialog: Dialog? = null,
            val hasPromptedToAddBefore: Boolean = false,
            val nextAiringEpisode: Pair<Int, Long> = Pair(
                anime.nextEpisodeToAir,
                anime.nextEpisodeAiringAt,
            ),
        ) : State {

            val processedSeasons by lazy {
                seasons.applySeasonFilters(anime).toList()
            }

            val processedEpisodes by lazy {
                episodes.applyFilters(anime).toList()
            }

            val episodeListItems by lazy {
                processedEpisodes.insertSeparators { before, after ->
                    val (lowerEpisode, higherEpisode) = if (anime.sortDescending()) {
                        after to before
                    } else {
                        before to after
                    }
                    if (higherEpisode == null) return@insertSeparators null

                    if (lowerEpisode == null) {
                        floor(higherEpisode.episode.episodeNumber)
                            .toInt()
                            .minus(1)
                            .coerceAtLeast(0)
                    } else {
                        calculateEpisodeGap(higherEpisode.episode, lowerEpisode.episode)
                    }
                        .takeIf { it > 0 }
                        ?.let { missingCount ->
                            EpisodeList.MissingCount(
                                id = "${lowerEpisode?.id}-${higherEpisode.id}",
                                count = missingCount,
                            )
                        }
                }
            }

            val airingEpisodeNumber: Double
                get() = nextAiringEpisode.first.toDouble()

            val airingTime: Long
                get() = nextAiringEpisode.second.times(1000L).minus(
                    Calendar.getInstance().timeInMillis,
                )

            val showPreviews: Boolean
                get() = anime.showPreviews()

            val showSummaries: Boolean
                get() = anime.showSummaries()

            private fun List<EpisodeList.Item>.applyFilters(anime: Anime): Sequence<EpisodeList.Item> {
                val isLocalAnime = anime.isLocal()
                val unseenFilter = anime.unseenFilter
                val downloadedFilter = anime.downloadedFilter
                val bookmarkedFilter = anime.bookmarkedFilter
                val fillermarkedFilter = anime.fillermarkedFilter
                return asSequence()
                    .filter { (episode) -> applyFilter(unseenFilter) { !episode.seen } }
                    .filter { (episode) -> applyFilter(bookmarkedFilter) { episode.bookmark } }
                    .filter { (episode) -> applyFilter(fillermarkedFilter) { episode.fillermark } }
                    .filter { applyFilter(downloadedFilter) { it.isDownloaded || isLocalAnime } }
                    .sortedWith { (episode1), (episode2) ->
                        getEpisodeSort(anime).invoke(
                            episode1,
                            episode2,
                        )
                    }
            }

            private fun List<AnimeSeasonItem>.applySeasonFilters(anime: Anime): Sequence<AnimeSeasonItem> {
                val unseenFilter = anime.seasonUnseenFilter
                val downloadedFilter = anime.seasonDownloadedFilter
                val startedFilter = anime.seasonStartedFilter
                val completedFilter = anime.seasonCompletedFilter
                val bookmarkedFilter = anime.seasonBookmarkedFilter
                val fillermarkedFilter = anime.seasonFillermarkedFilter

                val comparator = getSeasonSortComparator(anime)
                    .let { if (anime.seasonSortDescending()) it.reversed() else it }
                    .thenComparator(seasonSortAlphabetically)

                return asSequence()
                    .filter { (season) -> applyFilter(unseenFilter) { !season.seen } }
                    .filter { (season) -> applyFilter(startedFilter) { season.hasStarted } }
                    .filter { (season) ->
                        applyFilter(completedFilter) { season.anime.status.toInt() == SAnime.COMPLETED }
                    }
                    .filter { (season) -> applyFilter(bookmarkedFilter) { season.hasBookmarks } }
                    .filter { (season) -> applyFilter(fillermarkedFilter) { season.hasFillermarks } }
                    .filter { applyFilter(downloadedFilter) { it.downloadCount > 0 || it.seasonAnime.anime.isLocal() } }
                    .sortedWith(compareBy(comparator) { it.seasonAnime })
                    .map {
                        val itemAnime = it.seasonAnime.anime
                        AnimeSeasonItem(
                            seasonAnime = it.seasonAnime,
                            downloadCount = if (anime.seasonDownloadedOverlay) it.downloadCount else -1L,
                            unseenCount = if (anime.seasonUnseenOverlay) it.unseenCount else -1L,
                            isLocal = anime.seasonLocalOverlay && it.isLocal,
                            sourceLanguage = if (anime.seasonLangOverlay) it.sourceLanguage else "",
                            showContinueOverlay =
                            anime.seasonContinueOverlay &&
                                it.unseenCount > 0 &&
                                itemAnime.fetchType == FetchType.Episodes,
                        )
                    }
            }
        }
    }
}

@Immutable
sealed class EpisodeList {
    @Immutable
    data class MissingCount(
        val id: String,
        val count: Int,
    ) : EpisodeList()

    @Immutable
    data class Item(
        val episode: Episode,
        val downloadState: AnimeDownload.State,
        val downloadProgress: Int,
        val selected: Boolean = false,
    ) : EpisodeList() {
        val id = episode.id
        val isDownloaded = downloadState == AnimeDownload.State.DOWNLOADED
    }
}