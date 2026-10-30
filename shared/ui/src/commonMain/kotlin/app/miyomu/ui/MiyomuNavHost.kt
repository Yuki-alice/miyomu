package app.miyomu.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.entryProvider
import app.miyomu.ui.navigation.*
import androidx.navigation3.ui.NavDisplay
import app.miyomu.local.LocalBook
import app.miyomu.local.LocalBookFormat
import app.miyomu.model.MediaType
import app.miyomu.model.ComicSummary
import app.miyomu.source.novel.LegadoBookSource
import app.miyomu.ui.backup.BackupSyncRoute
import app.miyomu.ui.download.DownloadQueueRoute
import app.miyomu.ui.entries.MangaDetailScreen
import app.miyomu.ui.entries.VideoDetailScreen
import app.miyomu.ui.home.HomeScreen
import app.miyomu.ui.novel.NovelDetailScreen
import app.miyomu.ui.novel.NovelExploreScreen
import app.miyomu.ui.novel.NovelOnlineDetailScreen
import app.miyomu.ui.novel.NovelOnlineReaderScreen
import app.miyomu.ui.novel.NovelReaderScreen
import app.miyomu.ui.novel.NovelSourceSearchScreen
import app.miyomu.ui.player.OnlineVideoPlayerRoute
import app.miyomu.ui.player.VideoPlayerRoute
import app.miyomu.ui.reader.OnlineReaderScreen
import app.miyomu.ui.settings.ReadingStatsRoute
import app.miyomu.ui.settings.TrackSettingsRoute
import app.miyomu.ui.reader.ReaderScreen
import app.miyomu.ui.settings.AboutRoute
import app.miyomu.ui.settings.DanmakuSettingsRoute
import app.miyomu.ui.settings.SettingsAppearanceRoute
import app.miyomu.ui.settings.SettingsDataRoute
import app.miyomu.ui.settings.SettingsDownloadRoute
import app.miyomu.ui.settings.SettingsMainRoute
import app.miyomu.ui.settings.SettingsReaderRoute
import app.miyomu.ui.sources.GlobalSearchRoute
import app.miyomu.ui.sources.SourceAnimeDetailScreen
import app.miyomu.ui.sources.SourceDetailScreen
import app.miyomu.ui.sources.SourceRepositoryScreen
import app.miyomu.ui.sources.SourceSearchScreen

/**
 * The one place where a [MiyomuKey] turns into a screen.
 *
 * Keeping the mapping in a single `entryProvider` means the destination list is reviewable in one
 * file instead of being spread across 20 `Screen` classes, and every destination is forced to
 * declare the data it actually needs as key fields.
 */
@Composable
fun MiyomuNavHost(
    graph: MiyomuGraph,
    onExit: () -> Unit = {},
    start: MiyomuKey = HomeKey,
) {
    val (navigator, backStack) = rememberMiyomuNavigator(start)

    CompositionLocalProvider(LocalMiyomuNavigator provides navigator) {
        NavDisplay(
            backStack = backStack,
            onBack = { if (!navigator.pop()) onExit() },
            entryProvider = entryProvider {
                entry<HomeKey> { HomeScreen(graph) }

                // Local entries -------------------------------------------------------------
                entry<MangaDetailKey> { key ->
                    LocalBookEntry(graph, key.bookId) { book -> MangaDetailScreen(graph, book) }
                }
                entry<VideoDetailKey> { key ->
                    val shelved = graph.findLocalBook(key.bookId)
                    val book = shelved ?: remember(key) {
                        key.location?.let { location ->
                            LocalBook(
                                id = key.bookId,
                                title = key.title ?: "",
                                format = LocalBookFormat.valueOf(key.format),
                                mediaType = MediaType.VIDEO,
                                location = location,
                                lastModified = key.lastModified,
                            )
                        }
                    }
                    if (book == null) MissingEntry("找不到这个视频（${key.bookId}）") else {
                        VideoDetailScreen(graph, book)
                    }
                }
                entry<NovelDetailKey> { key ->
                    LocalBookEntry(graph, key.bookId) { book -> NovelDetailScreen(graph, book) }
                }
                entry<MangaReaderKey> { key ->
                    LocalBookEntry(graph, key.bookId) { book ->
                        ReaderScreen(graph, book, key.chapterIndex, key.pageIndex)
                    }
                }
                entry<NovelReaderKey> { key ->
                    LocalBookEntry(graph, key.bookId) { book ->
                        NovelReaderScreen(graph, book, key.chapterIndex, key.charOffset)
                    }
                }
                entry<VideoPlayerKey> { key ->
                    LocalBookEntry(graph, key.bookId) { book ->
                        VideoPlayerRoute(graph, book, key.episodeId)
                    }
                }

                // Installed sources ---------------------------------------------------------
                entry<SourceSearchKey> { key ->
                    SourceSearchScreen(graph, key.sourceId, key.sourceName, key.mediaType)
                }
                entry<SourceDetailKey> { key ->
                    SourceDetailScreen(graph, key.sourceId, key.sourceName, remember(key) { key.summary() })
                }
                entry<SourceAnimeDetailKey> { key ->
                    SourceAnimeDetailScreen(graph, key.sourceId, key.sourceName, remember(key) { key.summary() })
                }
                entry<OnlineReaderKey> { key ->
                    OnlineReaderScreen(graph, key.sourceId, key.comicId, key.title, key.chapterIndex)
                }
                entry<OnlineVideoPlayerKey> { key -> OnlineVideoPlayerRoute(graph, key) }
                entry<SourceRepositoryKey> { SourceRepositoryScreen(graph) }
                entry<GlobalSearchKey> { GlobalSearchRoute(graph) }

                // Online novels -------------------------------------------------------------
                entry<NovelExploreKey> { key -> NovelExploreScreen(graph, remember(key) { graph.legadoSource(key.sourceUrl) }) }
                entry<NovelSourceSearchKey> { key -> NovelSourceSearchScreen(graph, remember(key) { graph.legadoSource(key.sourceUrl) }) }
                entry<NovelOnlineDetailKey> { key ->
                    NovelOnlineDetailScreen(graph, remember(key) { key.toOnlineBook() })
                }
                entry<NovelOnlineReaderKey> { key ->
                    NovelOnlineReaderScreen(graph, remember(key) { key.toOnlineBook() }, key.chapterIndex, key.charOffset)
                }

                // Settings and utilities ----------------------------------------------------
                entry<SettingsKey> { SettingsMainRoute(graph) }
                entry<SettingsAppearanceKey> { SettingsAppearanceRoute(graph) }
                entry<SettingsReaderKey> { SettingsReaderRoute(graph) }
                entry<SettingsDownloadKey> { SettingsDownloadRoute(graph) }
                entry<SettingsDataKey> { SettingsDataRoute(graph) }
                entry<DanmakuSettingsKey> { DanmakuSettingsRoute(graph) }
                entry<AboutKey> { AboutRoute() }
                entry<BackupKey> { BackupSyncRoute(graph) }
                entry<DownloadsKey> {
                    val queue = graph.downloadQueue
                    if (queue == null) MissingEntry("下载队列不可用") else DownloadQueueRoute(queue, graph.downloadStore)
                }
                entry<ReadingStatsKey> { ReadingStatsRoute(graph) }
                entry<TrackSettingsKey> { TrackSettingsRoute(graph) }
            },
        )
    }
}

/**
 * Resolves the book a key points at. Books are addressed by id so the back stack stays
 * serializable; the lookup re-runs when the library flow changes, which also covers the case of a
 * detail screen restored after a rescan.
 */
@Composable
private fun LocalBookEntry(
    graph: MiyomuGraph,
    bookId: String,
    content: @Composable (LocalBook) -> Unit,
) {
    // Comics/videos and novels live in two separate shelves; a key only carries an id, so both
    // have to be searched before declaring the destination unreachable.
    val book = graph.findLocalBook(bookId)
    if (book == null) MissingEntry("找不到这本书（$bookId）") else content(book)
}

@Composable
private fun MissingEntry(message: String) {
    val navigator = LocalMiyomuNavigator.current
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = message, style = MaterialTheme.typography.bodyMedium)
        TextButton(onClick = { navigator.pop() }) { Text("返回") }
    }
}

/**
 * Books are split across two shelves (comic/video vs novel) and season folders are not shelved at
 * all, so resolution has to look in both before giving up.
 */
@Composable
private fun MiyomuGraph.findLocalBook(bookId: String): LocalBook? {
    val books by library.books.collectAsState()
    val novels by novelLibrary.books.collectAsState()
    return books.firstOrNull { it.id == bookId } ?: novels.firstOrNull { it.id == bookId }
}

/** Legado sources are stored by the user; the key only carries the url, so resolve it here. */
private fun MiyomuGraph.legadoSource(url: String?): LegadoBookSource =
    novelSourceStore.sources().firstOrNull { it.bookSourceUrl == url } ?: legadoSourceFor(url)

private fun SourceDetailKey.summary(): ComicSummary = ComicSummary(
    id = comicId,
    title = title,
    coverUrl = coverUrl,
)

private fun SourceAnimeDetailKey.summary(): ComicSummary = ComicSummary(
    id = animeId,
    title = title,
    coverUrl = coverUrl,
)

private fun NovelExploreKey.source(): LegadoBookSource = legadoSourceFor(sourceUrl)

private fun NovelSourceSearchKey.source(): LegadoBookSource = legadoSourceFor(sourceUrl)
