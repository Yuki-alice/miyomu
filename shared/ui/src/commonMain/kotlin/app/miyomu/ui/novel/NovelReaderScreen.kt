package app.miyomu.ui.novel

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.miyomu.data.MiyomuData
import app.miyomu.local.LocalBook
import app.miyomu.model.LibraryItem
import app.miyomu.model.ProgressKind
import app.miyomu.model.ReadingProgress
import app.miyomu.novel.LocalNovelRepository
import app.miyomu.reader.novel.NovelBookmark
import app.miyomu.reader.novel.NovelBookmarkStore
import app.miyomu.reader.novel.NovelBookSearch
import app.miyomu.reader.novel.NovelReadingMode
import app.miyomu.reader.novel.NovelReaderController
import app.miyomu.reader.novel.NovelReaderPreferences
import app.miyomu.reader.novel.NovelReaderTheme
import app.miyomu.ui.MiyomuGraph
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.core.preference.asState
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

/** Port of the reader entry as a full-screen Voyager Screen (mirrors the manga ReaderScreen). */
class NovelReaderScreen(
    private val graph: MiyomuGraph,
    private val book: LocalBook,
    private val chapterIndex: Int,
    private val charOffset: Int,
) : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        NovelReaderContent(
            book = book,
            initialChapterIndex = chapterIndex,
            initialCharOffset = charOffset,
            novels = graph.novels,
            bookmarkStore = graph.novelBookmarks,
            data = graph.data,
            deviceId = graph.deviceId,
            nowMillis = graph.nowMillis,
            preferences = graph.novelReaderPreferences,
            onBack = { navigator.pop() },
        )
    }
}

/** Reader surface colors; the app theme does not intrude into the reading surface. */
data class NovelPalette(val background: Color, val text: Color)

internal fun NovelReaderTheme.palette(): NovelPalette = when (this) {
    NovelReaderTheme.LIGHT -> NovelPalette(Color(0xFFFFFFFF), Color(0xFF24292F))
    NovelReaderTheme.SEPIA -> NovelPalette(Color(0xFFF3E9D2), Color(0xFF53452F))
    NovelReaderTheme.DARK -> NovelPalette(Color(0xFF202125), Color(0xFFC9CDD3))
    NovelReaderTheme.BLACK -> NovelPalette(Color(0xFF000000), Color(0xFF9AA0A6))
}

@Composable
fun NovelReaderContent(
    book: LocalBook,
    initialChapterIndex: Int,
    initialCharOffset: Int,
    novels: LocalNovelRepository,
    bookmarkStore: NovelBookmarkStore,
    data: MiyomuData,
    deviceId: String,
    nowMillis: () -> Long,
    preferences: NovelReaderPreferences,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val controller = remember(book.id) { NovelReaderController(novels, scope) }
    val state by controller.state.collectAsState()

    LaunchedEffect(book.id) {
        controller.open(book, initialChapterIndex, initialCharOffset)
    }
    DisposableEffect(controller) {
        onDispose { controller.close() }
    }

    val readingMode by remember(preferences) { preferences.readingMode() }.asState(scope)
    val theme by remember(preferences) { preferences.theme() }.asState(scope)
    val fontSize by remember(preferences) { preferences.fontSize() }.asState(scope)
    val lineHeight by remember(preferences) { preferences.lineHeight() }.asState(scope)
    val pagePadding by remember(preferences) { preferences.pagePadding() }.asState(scope)
    val paragraphSpacing by remember(preferences) { preferences.paragraphSpacing() }.asState(scope)
    val warmFilter by remember(preferences) { preferences.warmFilter() }.asState(scope)
    val volumeKeyPaging by remember(preferences) { preferences.volumeKeyPaging() }.asState(scope)

    val palette = theme.palette()
    val itemId = LibraryItem.idOf("local", book.id)
    var chromeVisible by remember { mutableStateOf(true) }
    var settingsVisible by remember { mutableStateOf(false) }
    var catalogVisible by remember { mutableStateOf(false) }
    var bookmarks by remember(book.id) { mutableStateOf(bookmarkStore.bookmarks(book.location)) }

    // Char offset of the reading position inside the current chapter. Re-synced from the
    // controller when a chapter loads (not keyed on chapterIndex: resuming inside chapter 0
    // must still pick up the saved offset).
    var currentOffset = remember { mutableIntStateOf(0) }
    LaunchedEffect(state.text, state.chapterIndex) {
        currentOffset.intValue = state.pendingOffset.coerceAtLeast(0)
    }

    fun saveProgress(kind: ProgressKind, offset: Int) {
        if (state.text.isEmpty()) return
        scope.launch {
            data.progress.save(
                ReadingProgress(
                    itemId = itemId,
                    entryId = LocalNovelRepository.chapterId(book, state.chapterIndex),
                    kind = kind,
                    position = offset.toDouble().coerceAtLeast(0.0),
                    updatedAt = nowMillis(),
                    deviceId = deviceId,
                ),
            )
            data.library.markViewed(itemId, nowMillis())
        }
    }

    fun toggleBookmark() {
        val offset = currentOffset.intValue
        if (bookmarkStore.has(book.id, state.chapterIndex, offset)) {
            bookmarks = bookmarkStore.remove(book.id, state.chapterIndex, offset)
        } else {
            val bookmark = NovelBookmark(
                chapterIndex = state.chapterIndex,
                offset = offset,
                excerpt = excerptAt(state.text, offset),
                createdAt = nowMillis(),
            )
            bookmarks = bookmarkStore.add(book.id, bookmark)
        }
    }

    val searchInBook: suspend (String) -> List<NovelBookSearch.Match> = remember(book.id) {
        { query ->
            runCatching { novels.parse(book) }
                .map { parsed -> NovelBookSearch.search(parsed.chapters, query) }
                .getOrDefault(emptyList())
        }
    }

    // ---- Paged mode machinery ----
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    val paddingPx = with(density) { pagePadding.dp.roundToPx() }
    val spacingPx = with(density) { paragraphSpacing.dp.roundToPx() }

    val pages by produceState<NovelChapterPages?>(
        null,
        state.text, state.chapterIndex, containerSize, fontSize, lineHeight, paddingPx, spacingPx,
    ) {
        if (state.text.isEmpty() || containerSize == IntSize.Zero) {
            value = null
            return@produceState
        }
        value = measureChapter(
            chapterIndex = state.chapterIndex,
            text = state.text,
            measurer = measurer,
            density = density,
            usableWidthPx = (containerSize.width - 2 * paddingPx).coerceAtLeast(1),
            usableHeightPx = (containerSize.height - 2 * paddingPx).coerceAtLeast(1),
            fontSizeSp = fontSize,
            lineHeightMultiplier = lineHeight,
            paragraphSpacingPx = spacingPx,
        )
    }

    val pagerState = rememberPagerState(pageCount = { pages?.pageCount ?: 0 })

    // Landing page: chapter open (offset = saved offset) and config changes (offset = current
    // position) share one path — always the page containing currentOffset.
    LaunchedEffect(pages) {
        pages?.let { pagerState.scrollToPage(it.pageIndexForOffset(currentOffset.intValue)) }
    }
    LaunchedEffect(pagerState, pages) {
        snapshotFlow { pagerState.currentPage }
            .drop(1)
            .collect { index ->
                pages?.let { currentOffset.intValue = it.startOffsetOf(index) }
            }
    }
    LaunchedEffect(currentOffset.intValue, state.chapterIndex, state.text) {
        saveProgress(ProgressKind.PAGE, currentOffset.intValue)
    }

    fun pagedStep(direction: Int) {
        val target = pagerState.currentPage + direction
        if (target in 0 until (pages?.pageCount ?: 0)) {
            scope.launch { pagerState.animateScrollToPage(target) }
        }
    }

    // ---- Scroll mode machinery ----
    val paragraphs = remember(state.text) { splitParagraphs(state.text) }
    val listState = rememberLazyListState()
    LaunchedEffect(paragraphs, readingMode, state.chapterIndex) {
        if (readingMode == NovelReadingMode.SCROLL && paragraphs.isNotEmpty()) {
            listState.scrollToItem(paragraphIndexForOffset(paragraphs, currentOffset.intValue))
        }
    }
    LaunchedEffect(listState, readingMode) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .drop(1)
            .collect { index ->
                if (readingMode == NovelReadingMode.SCROLL && index in paragraphs.indices) {
                    currentOffset.intValue = paragraphs[index].first
                }
            }
    }
    fun scrollSeek(fraction: Float) {
        if (paragraphs.isEmpty()) return
        val offset = (fraction * state.text.length).toInt()
        scope.launch { listState.scrollToItem(paragraphIndexForOffset(paragraphs, offset)) }
    }

    fun stepForward() {
        when (readingMode) {
            NovelReadingMode.PAGED -> pagedStep(+1)
            NovelReadingMode.SCROLL -> scope.launch {
                listState.animateScrollBy(containerSize.height * 0.85f)
            }
        }
    }

    fun stepBackward() {
        when (readingMode) {
            NovelReadingMode.PAGED -> pagedStep(-1)
            NovelReadingMode.SCROLL -> scope.launch {
                listState.animateScrollBy(-containerSize.height * 0.85f)
            }
        }
    }

    fun jumpTo(chapterIndex: Int, offset: Int) {
        catalogVisible = false
        if (chapterIndex == state.chapterIndex) {
            currentOffset.intValue = offset
            when (readingMode) {
                NovelReadingMode.PAGED -> pages?.let { target ->
                    scope.launch { pagerState.scrollToPage(target.pageIndexForOffset(offset)) }
                }
                NovelReadingMode.SCROLL -> scope.launch {
                    listState.scrollToItem(paragraphIndexForOffset(paragraphs, offset))
                }
            }
        } else {
            controller.open(book, chapterIndex, offset)
        }
    }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .onSizeChanged { containerSize = it }
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                val forward = when (event.key) {
                    Key.DirectionRight, Key.DirectionDown, Key.PageDown, Key.Spacebar -> true
                    Key.VolumeDown -> volumeKeyPaging
                    else -> false
                }
                val backward = when (event.key) {
                    Key.DirectionLeft, Key.DirectionUp, Key.PageUp -> true
                    Key.VolumeUp -> volumeKeyPaging
                    else -> false
                }
                when {
                    forward -> {
                        stepForward(); true
                    }
                    backward -> {
                        stepBackward(); true
                    }
                    else -> false
                }
            },
    ) {
        when {
            state.error != null -> NovelReaderError(
                message = state.error.orEmpty(),
                palette = palette,
                onRetry = { controller.openChapter(state.chapterIndex) },
            )

            state.loading || state.text.isEmpty() -> CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
            )

            readingMode == NovelReadingMode.SCROLL -> NovelScrollContent(
                paragraphs = paragraphs,
                listState = listState,
                fontSize = fontSize,
                lineHeight = lineHeight,
                pagePadding = pagePadding,
                paragraphSpacing = paragraphSpacing,
                palette = palette,
                onToggleChrome = { chromeVisible = !chromeVisible },
            )

            else -> {
                val chapterPages = pages
                if (chapterPages == null) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("novel-reader-pager"),
                        beyondViewportPageCount = 1,
                    ) { index ->
                        NovelPageView(
                            chapterPages = chapterPages,
                            pageIndex = index,
                            palette = palette,
                            pagePadding = pagePadding,
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("novel-page-$index"),
                            onTap = { xFraction ->
                                when {
                                    xFraction < 1f / 3f -> pagedStep(-1)
                                    xFraction > 2f / 3f -> pagedStep(+1)
                                    else -> chromeVisible = !chromeVisible
                                }
                            },
                        )
                    }
                }
            }
        }

        val pagedCount = pages?.pageCount ?: 0
        NovelReaderChrome(
            visible = chromeVisible,
            bookTitle = book.title,
            chapterTitle = state.chapterTitle
                ?: stringResource(MR.strings.novel_chapter_position, state.chapterIndex + 1, state.chapterCount),
            positionLabel = when (readingMode) {
                NovelReadingMode.PAGED ->
                    if (pagedCount > 0) {
                        stringResource(MR.strings.novel_page_position, pagerState.currentPage + 1, pagedCount)
                    } else {
                        ""
                    }
                NovelReadingMode.SCROLL ->
                    if (state.text.isEmpty()) "0%" else "${currentOffset.intValue * 100 / state.text.length}%"
            },
            positionFraction = when (readingMode) {
                NovelReadingMode.PAGED ->
                    if (pagedCount > 1) pagerState.currentPage / (pagedCount - 1f) else 0f
                NovelReadingMode.SCROLL ->
                    if (state.text.isEmpty()) 0f else currentOffset.intValue / state.text.length.toFloat()
            },
            hasPreviousChapter = state.hasPreviousChapter,
            hasNextChapter = state.hasNextChapter,
            bookmarked = bookmarkStore.has(book.id, state.chapterIndex, currentOffset.intValue),
            palette = palette,
            onBack = onBack,
            onPreviousChapter = controller::previousChapter,
            onNextChapter = controller::nextChapter,
            onPositionChange = { fraction ->
                when (readingMode) {
                    NovelReadingMode.PAGED -> {
                        if (pagedCount > 0) {
                            val target = (fraction * (pagedCount - 1)).toInt().coerceIn(0, pagedCount - 1)
                            scope.launch { pagerState.scrollToPage(target) }
                        }
                    }
                    NovelReadingMode.SCROLL -> scrollSeek(fraction)
                }
            },
            onOpenSettings = { settingsVisible = true },
            onOpenCatalog = { catalogVisible = true },
            onToggleBookmark = { toggleBookmark() },
        )

        // Warm (eye-comfort) overlay above everything, like Play Books' Night Light; a plain
        // Box does not consume pointer events, so taps still reach the pager below.
        if (warmFilter > 0) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFFF9800).copy(alpha = warmFilter * 0.08f))
                    .testTag("novel-warm-filter"),
            )
        }
    }

    if (settingsVisible) {
        NovelReaderSettingsSheet(
            preferences = preferences,
            onDismiss = { settingsVisible = false },
        )
    }

    if (catalogVisible) {
        NovelCatalogSheet(
            chapterTitles = state.chapterTitles,
            currentChapterIndex = state.chapterIndex,
            bookmarks = bookmarks,
            search = searchInBook,
            onJumpChapter = { index -> jumpTo(index, 0) },
            onJumpMatch = { match -> jumpTo(match.chapterIndex, match.offset) },
            onDeleteBookmark = { bookmark ->
                bookmarks = bookmarkStore.remove(book.id, bookmark.chapterIndex, bookmark.offset)
            },
            onDismiss = { catalogVisible = false },
        )
    }
}

@Composable
private fun NovelPageView(
    chapterPages: NovelChapterPages,
    pageIndex: Int,
    palette: NovelPalette,
    pagePadding: Int,
    onTap: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val page = chapterPages.pages[pageIndex.coerceIn(0, chapterPages.pages.lastIndex)]
    Box(
        modifier = modifier
            .padding(pagePadding.dp)
            .clipToBounds()
            .pointerInput(onTap) {
                detectTapGestures(
                    onTap = { position ->
                        val width = size.width.coerceAtLeast(1)
                        onTap(position.x / width)
                    },
                )
            }
            .drawBehind {
                drawRect(palette.background)
                page.placements.forEach { placement ->
                    val piece = chapterPages.pieces[placement.pieceIndex]
                    drawText(
                        textLayoutResult = piece.layout,
                        color = palette.text,
                        topLeft = Offset(0f, placement.yInPage - placement.topInPiece),
                    )
                }
            },
    )
}

@Composable
private fun NovelScrollContent(
    paragraphs: List<Pair<Int, String>>,
    listState: LazyListState,
    fontSize: Int,
    lineHeight: Float,
    pagePadding: Int,
    paragraphSpacing: Int,
    palette: NovelPalette,
    onToggleChrome: () -> Unit,
) {
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .testTag("novel-reader-scroll")
            .pointerInput(onToggleChrome) {
                detectTapGestures(onTap = { onToggleChrome() })
            },
        contentPadding = PaddingValues(
            horizontal = pagePadding.dp,
            vertical = (pagePadding * 2).dp,
        ),
        verticalArrangement = Arrangement.spacedBy(paragraphSpacing.dp),
    ) {
        items(
            count = paragraphs.size,
            key = { index -> paragraphs[index].first },
        ) { index ->
            Text(
                text = paragraphs[index].second,
                color = palette.text,
                style = TextStyle(
                    fontSize = fontSize.sp,
                    lineHeight = (fontSize * lineHeight).sp,
                ),
            )
        }
    }
}

@Composable
private fun NovelReaderError(
    message: String,
    palette: NovelPalette,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = message, color = palette.text)
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = onRetry) {
            Text(stringResource(MR.strings.action_retry), color = palette.text)
        }
    }
}

internal fun excerptAt(text: String, offset: Int, radius: Int = 22): String {
    if (text.isEmpty()) return ""
    val start = (offset - radius / 2).coerceAtLeast(0)
    val end = (offset + radius).coerceAtMost(text.length)
    val prefix = if (start > 0) "…" else ""
    val suffix = if (end < text.length) "…" else ""
    return prefix + text.substring(start, end).replace('\n', ' ') + suffix
}

/** Paragraph whose range contains [offset]; clamps to the last paragraph. */
internal fun paragraphIndexForOffset(paragraphs: List<Pair<Int, String>>, offset: Int): Int {
    if (paragraphs.isEmpty()) return 0
    var low = 0
    var high = paragraphs.lastIndex
    var result = 0
    while (low <= high) {
        val mid = (low + high) / 2
        if (paragraphs[mid].first <= offset) {
            result = mid
            low = mid + 1
        } else {
            high = mid - 1
        }
    }
    return result
}
