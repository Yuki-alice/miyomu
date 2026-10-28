package app.miyomu.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import app.miyomu.data.MiyomuData
import app.miyomu.local.LocalBook
import app.miyomu.local.LocalComicRepository
import app.miyomu.local.LocalFileRef
import app.miyomu.local.LocalLibraryRepository
import app.miyomu.model.ComicSummary
import app.miyomu.model.LibraryItem
import app.miyomu.model.MediaType
import app.miyomu.model.SourceDescriptor
import kotlinx.coroutines.launch

private sealed interface AppRoute {
    data object Home : AppRoute
    data class Detail(val book: LocalBook) : AppRoute
    data class Reader(
        val book: LocalBook,
        val chapterIndex: Int,
        val pageIndex: Int,
    ) : AppRoute
}

private val LocalSource = SourceDescriptor(
    id = "local",
    name = "本地文件",
    mediaType = MediaType.COMIC,
    version = "1",
)

@Composable
fun MiyomuApp(
    library: LocalLibraryRepository,
    comics: LocalComicRepository,
    data: MiyomuData,
    root: LocalFileRef,
    deviceId: String,
    nowMillis: () -> Long,
    onOpenLibraryFolder: (() -> Unit)? = null,
) {
    var route by remember { mutableStateOf<AppRoute>(AppRoute.Home) }
    val books by library.books.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(root) {
        library.refresh(root)
    }

    fun openBook(book: LocalBook) {
        scope.launch {
            val now = nowMillis()
            val item = LibraryItem.from(
                source = LocalSource,
                comic = ComicSummary(id = book.id, title = book.title),
                now = now,
            )
            data.library.upsert(item)
            data.library.markViewed(item.id, now)
        }
        route = AppRoute.Detail(book)
    }

    MiyomuTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            when (val current = route) {
            AppRoute.Home -> HomeScreen(
                books = books,
                comics = comics,
                data = data,
                libraryPath = root.location,
                onOpenBook = ::openBook,
                onRefresh = { scope.launch { library.refresh(root) } },
                onOpenLibraryFolder = onOpenLibraryFolder,
            )
            is AppRoute.Detail -> BookDetailScreen(
                book = current.book,
                comics = comics,
                data = data,
                onBack = { route = AppRoute.Home },
                onRead = { chapterIndex, pageIndex ->
                    route = AppRoute.Reader(current.book, chapterIndex, pageIndex)
                },
            )
                is AppRoute.Reader -> ComicReaderScreen(
                    book = current.book,
                    initialChapterIndex = current.chapterIndex,
                    initialPageIndex = current.pageIndex,
                    comics = comics,
                    data = data,
                    deviceId = deviceId,
                    nowMillis = nowMillis,
                    onBack = { route = AppRoute.Detail(current.book) },
                )
            }
        }
    }
}
