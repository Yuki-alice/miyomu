package app.miyomu.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.miyomu.data.MiyomuData
import app.miyomu.local.LocalBook
import app.miyomu.local.LocalComicRepository
import app.miyomu.local.LocalPage
import app.miyomu.model.LibraryItem
import app.miyomu.model.ProgressKind
import app.miyomu.model.ReadingProgress
import app.miyomu.reader.comic.ComicReaderController
import app.miyomu.reader.comic.ComicReaderUiState
import app.miyomu.reader.comic.ComicReadingMode
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun ComicReaderScreen(
    book: LocalBook,
    initialChapterIndex: Int,
    initialPageIndex: Int,
    comics: LocalComicRepository,
    data: MiyomuData,
    deviceId: String,
    nowMillis: () -> Long,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val controller = remember(book.id) { ComicReaderController(comics, scope) }
    val state by controller.state.collectAsState()

    LaunchedEffect(book.id) {
        controller.open(book, initialChapterIndex, initialPageIndex)
    }
    DisposableEffect(controller) {
        onDispose { controller.close() }
    }

    val itemId = LibraryItem.idOf("local", book.id)
    val chapter = state.currentChapter
    LaunchedEffect(chapter?.id, state.pageIndex, state.pages.size) {
        if (chapter != null && state.pages.isNotEmpty()) {
            val now = nowMillis()
            data.progress.save(
                ReadingProgress(
                    itemId = itemId,
                    entryId = chapter.id,
                    kind = ProgressKind.PAGE,
                    position = state.pageIndex.toDouble(),
                    updatedAt = now,
                    deviceId = deviceId,
                ),
            )
            data.library.markViewed(itemId, now)
        }
    }

    val pageCount = state.pages.size
    val pagerState = rememberPagerState(
        initialPage = state.pageIndex.coerceIn(0, maxOf(0, pageCount - 1)),
    ) { pageCount }

    LaunchedEffect(state.chapterIndex, pageCount) {
        if (pageCount > 0) {
            pagerState.scrollToPage(state.pageIndex.coerceIn(0, pageCount - 1))
        }
    }
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .drop(1)
            .collect { controller.setPage(it) }
    }

    val seekTo: (Int) -> Unit = { requested ->
        if (pageCount > 0) {
            val target = requested.coerceIn(0, pageCount - 1)
            controller.setPage(target)
            scope.launch { pagerState.animateScrollToPage(target) }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        when {
            state.error != null -> ReaderError(
                message = state.error.orEmpty(),
                onRetry = controller::retry,
            )
            state.pages.isEmpty() -> CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.align(Alignment.Center),
            )
            else -> ReaderPager(
                state = state,
                pagerState = pagerState,
                comics = comics,
                modifier = Modifier.fillMaxSize().testTag("reader-pager"),
            )
        }

        ReaderTopBar(
            book = book,
            state = state,
            onBack = onBack,
            modifier = Modifier.align(Alignment.TopCenter),
        )
        ReaderBottomBar(
            state = state,
            onSeek = seekTo,
            controller = controller,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun ReaderPager(
    state: ComicReaderUiState,
    pagerState: PagerState,
    comics: LocalComicRepository,
    modifier: Modifier = Modifier,
) {
    when (state.mode) {
        ComicReadingMode.LEFT_TO_RIGHT,
        ComicReadingMode.RIGHT_TO_LEFT,
        -> HorizontalPager(
            state = pagerState,
            modifier = modifier,
            reverseLayout = state.mode == ComicReadingMode.RIGHT_TO_LEFT,
            key = { index -> state.pages[index].location },
        ) { index ->
            ComicPage(page = state.pages[index], comics = comics)
        }
        ComicReadingMode.VERTICAL -> VerticalPager(
            state = pagerState,
            modifier = modifier,
            key = { index -> state.pages[index].location },
        ) { index ->
            ComicPage(page = state.pages[index], comics = comics)
        }
    }
}

@Composable
private fun ComicPage(
    page: LocalPage,
    comics: LocalComicRepository,
) {
    var image by remember(page.location) { mutableStateOf<ImageBitmap?>(null) }
    var error by remember(page.location) { mutableStateOf<String?>(null) }

    LaunchedEffect(page.location) {
        runCatching { decodeImageBitmap(comics.readPage(page)) }
            .onSuccess { image = it }
            .onFailure { error = it.message ?: "无法读取页面" }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        val bitmap = image
        when {
            bitmap != null -> Image(
                bitmap = bitmap,
                contentDescription = page.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
            error != null -> Text(text = error.orEmpty(), color = Color.White)
            else -> CircularProgressIndicator(color = Color.White)
        }
    }
}

@Composable
private fun ReaderTopBar(
    book: LocalBook,
    state: ComicReaderUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        tonalElevation = 3.dp,
        shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = state.currentChapter?.title.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (state.pages.isNotEmpty()) {
                Text(
                    text = "${state.pageIndex + 1} / ${state.pages.size}",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun ReaderBottomBar(
    state: ComicReaderUiState,
    onSeek: (Int) -> Unit,
    controller: ComicReaderController,
    modifier: Modifier = Modifier,
) {
    var modeMenu by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        tonalElevation = 3.dp,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = state.currentChapter?.title.orEmpty(),
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (state.pages.isNotEmpty()) {
                    Text(
                        text = "${state.pageIndex + 1} / ${state.pages.size}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (state.pages.size > 1) {
                Slider(
                    value = state.pageIndex.toFloat(),
                    onValueChange = { onSeek(it.roundToInt()) },
                    valueRange = 0f..state.pages.lastIndex.toFloat(),
                    steps = (state.pages.size - 2).coerceAtLeast(0),
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                Spacer(Modifier.height(8.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    enabled = state.hasPreviousChapter,
                    onClick = controller::previousChapter,
                ) {
                    Icon(Icons.Outlined.SkipPrevious, contentDescription = "上一章")
                }

                Box {
                    IconButton(onClick = { modeMenu = true }) {
                        Icon(Icons.AutoMirrored.Outlined.ViewList, contentDescription = "阅读模式")
                    }
                    DropdownMenu(
                        expanded = modeMenu,
                        onDismissRequest = { modeMenu = false },
                    ) {
                        ReadingModeOption(
                            label = "从左到右",
                            selected = state.mode == ComicReadingMode.LEFT_TO_RIGHT,
                        ) {
                            controller.setMode(ComicReadingMode.LEFT_TO_RIGHT)
                            modeMenu = false
                        }
                        ReadingModeOption(
                            label = "从右到左",
                            selected = state.mode == ComicReadingMode.RIGHT_TO_LEFT,
                        ) {
                            controller.setMode(ComicReadingMode.RIGHT_TO_LEFT)
                            modeMenu = false
                        }
                        ReadingModeOption(
                            label = "纵向翻页",
                            selected = state.mode == ComicReadingMode.VERTICAL,
                        ) {
                            controller.setMode(ComicReadingMode.VERTICAL)
                            modeMenu = false
                        }
                    }
                }

                IconButton(
                    enabled = state.hasNextChapter,
                    onClick = controller::nextChapter,
                ) {
                    Icon(Icons.Outlined.SkipNext, contentDescription = "下一章")
                }
            }
        }
    }
}

@Composable
private fun ReadingModeOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = { Text(label) },
        leadingIcon = {
            if (selected) {
                Icon(Icons.Outlined.Check, contentDescription = null)
            } else {
                Spacer(Modifier.size(24.dp))
            }
        },
        onClick = onClick,
    )
}

@Composable
private fun ReaderError(
    message: String,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = message, color = Color.White)
        Spacer(Modifier.height(4.dp))
        androidx.compose.material3.TextButton(onClick = onRetry) {
            Text("重试", color = Color.White)
        }
    }
}
