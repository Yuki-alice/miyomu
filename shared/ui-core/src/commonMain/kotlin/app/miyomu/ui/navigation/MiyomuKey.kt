package app.miyomu.ui.navigation

import androidx.navigation3.runtime.NavKey
import app.miyomu.model.AnimeEpisode
import kotlinx.serialization.Serializable

/**
 * Every destination in Miyomu is a serializable [NavKey].
 *
 * The key carries identity only — ids, titles and indexes — never live objects such as
 * `MiyomuGraph`, a `LocalBook` instance or a lambda. The entry block in [MiyomuNavHost] resolves
 * the real data from the graph, which is what keeps the back stack serializable and makes each
 * destination addressable from anywhere (deep link, restored state, tests).
 *
 * Media that only exists online (legado books) is flattened into its own fields because the
 * original model lives in a JVM-only source set and cannot be referenced from common code.
 */
@Serializable
sealed interface MiyomuKey : NavKey

// --- Shell -----------------------------------------------------------------------------------

/** The tab shell. Tabs are local state inside the screen, not back stack entries. */
@Serializable
data object HomeKey : MiyomuKey

// --- Local entries ---------------------------------------------------------------------------

@Serializable
data class MangaDetailKey(val bookId: String) : MiyomuKey

/**
 * A video entry. Season folders are discovered from disk rather than stored in the shelf, so the
 * key carries the minimum needed to rebuild one when the id is not in the library.
 */
@Serializable
data class VideoDetailKey(
    val bookId: String,
    val location: String? = null,
    val title: String? = null,
    val lastModified: Long = 0L,
    /** [app.miyomu.local.LocalBookFormat] name; season folders are VIDEO, not DIRECTORY. */
    val format: String = "DIRECTORY",
) : MiyomuKey

@Serializable
data class NovelDetailKey(val bookId: String) : MiyomuKey

@Serializable
data class MangaReaderKey(
    val bookId: String,
    val chapterIndex: Int,
    val pageIndex: Int,
) : MiyomuKey

@Serializable
data class NovelReaderKey(
    val bookId: String,
    val chapterIndex: Int,
    val charOffset: Int,
) : MiyomuKey

@Serializable
data class VideoPlayerKey(
    val bookId: String,
    val episodeId: String? = null,
) : MiyomuKey

// --- Installed sources -----------------------------------------------------------------------

@Serializable
data class SourceSearchKey(
    val sourceId: String,
    val sourceName: String,
    val mediaType: String = "COMIC",
) : MiyomuKey

@Serializable
data class SourceDetailKey(
    val sourceId: String,
    val sourceName: String,
    val comicId: String,
    val title: String,
    val coverUrl: String? = null,
) : MiyomuKey

@Serializable
data class SourceAnimeDetailKey(
    val sourceId: String,
    val sourceName: String,
    val animeId: String,
    val title: String,
    val coverUrl: String? = null,
) : MiyomuKey

@Serializable
data class OnlineReaderKey(
    val sourceId: String,
    val comicId: String,
    val title: String,
    val chapterIndex: Int,
) : MiyomuKey

/**
 * Playback of one episode from an installed source.
 *
 * The play queue travels with the key: `resolve` is per-episode and episodes are cheap data
 * classes, so carrying them lets the player resolve the next episode itself instead of holding a
 * callback that would make the destination impossible to restore.
 */
@Serializable
data class OnlineVideoPlayerKey(
    val sourceId: String,
    val itemId: String,
    val animeTitle: String,
    val episodes: List<AnimeEpisode>,
    val episodeIndex: Int,
    val uri: String,
    val headers: Map<String, String> = emptyMap(),
) : MiyomuKey

@Serializable
data object SourceRepositoryKey : MiyomuKey

@Serializable
data object GlobalSearchKey : MiyomuKey

// --- Online novels (legado) ------------------------------------------------------------------

@Serializable
data class NovelExploreKey(val sourceUrl: String?) : MiyomuKey

@Serializable
data class NovelSourceSearchKey(val sourceUrl: String?) : MiyomuKey

@Serializable
data class NovelOnlineDetailKey(
    val name: String,
    val author: String?,
    val bookUrl: String,
    val tocUrl: String,
    val coverUrl: String?,
    val sourceUrl: String?,
) : MiyomuKey

@Serializable
data class NovelOnlineReaderKey(
    val name: String,
    val author: String?,
    val bookUrl: String,
    val tocUrl: String,
    val coverUrl: String?,
    val sourceUrl: String?,
    val chapterIndex: Int,
    val charOffset: Int,
) : MiyomuKey

// --- Settings and utility pages ---------------------------------------------------------------

@Serializable
data object SettingsKey : MiyomuKey

@Serializable
data object SettingsAppearanceKey : MiyomuKey

@Serializable
data object SettingsReaderKey : MiyomuKey

@Serializable
data object SettingsDownloadKey : MiyomuKey

@Serializable
data object SettingsDataKey : MiyomuKey

@Serializable
data object DanmakuSettingsKey : MiyomuKey

@Serializable
data object BackupKey : MiyomuKey

@Serializable
data object DownloadsKey : MiyomuKey

@Serializable
data object ReadingStatsKey : MiyomuKey

@Serializable
data object TrackSettingsKey : MiyomuKey

@Serializable
data object AboutKey : MiyomuKey
