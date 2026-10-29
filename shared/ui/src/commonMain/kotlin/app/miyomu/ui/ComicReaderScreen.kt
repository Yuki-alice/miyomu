package app.miyomu.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.ViewStream
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
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
import app.miyomu.reader.comic.PageSpread
import app.miyomu.reader.comic.pageSpreads
import app.miyomu.reader.comic.spreadIndexForPage
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val MAX_ZOOM = 4f
private const val DOUBLE_TAP_ZOOM = 2.5f

/** Pinch/double-tap zoom state shared by the page container; offset is in container pixels. */
private class ReaderZoomState {
    var scale by mutableFloatStateOf(1f)
        private set
    var offset by mutableStateOf(Offset.Zero)
        private set
    var containerSize: IntSize by mutableStateOf(IntSize.Zero)

    fun reset() {
        scale = 1f
        offset = Offset.Zero
    }

    fun toggleZoom(focal: Offset) {
        if (scale > 1f) reset() else applyZoom(focal, DOUBLE_TAP_ZOOM)
    }

    fun applyZoom(centroid: Offset, gestureZoom: Float) {
        val newScale = (scale * gestureZoom).coerceIn(1f, MAX_ZOOM)
        if (newScale == scale) return
        val ratio = newScale / scale
        val newOffset = centroid - (centroid - offset) * ratio
        scale = newScale
        offset = clamp(newOffset)
    }

    fun applyPan(pan: Offset) {
        offset = clamp(offset + pan)
    }

    private fun clamp(value: Offset): Offset {
        if (scale <= 1f || containerSize == IntSize.Zero) return Offset.Zero
        val maxX = containerSize.width * (scale - 1f)
        val maxY = containerSize.height * (scale - 1f)
        return Offset(value.x.coerceIn(-maxX, 0f), value.y.coerceIn(-maxY, 0f))
    }
}

/**
 * Pinch zoom that yields single-finger drags to the pager/list: transforms are only consumed
 * for multi-touch gestures or while already zoomed in, so 1x swipes and scrolls stay native.
 */
private fun Modifier.zoomableGestures(zoom: ReaderZoomState): Modifier = pointerInput(zoom) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)
        while (true) {
            val event = awaitPointerEvent()
            val pressed = event.changes.count { it.pressed }
            if (pressed == 0) break
            if (pressed >= 2 || zoom.scale > 1f) {
                val gestureZoom = event.calculateZoom()
                if (gestureZoom != 1f) zoom.applyZoom(event.calculateCentroid(), gestureZoom)
                if (zoom.scale > 1f) zoom.applyPan(event.calculatePan())
                event.changes.fastForEach { change ->
                    if (change.positionChanged()) change.consume()
                }
            }
        }
    }
}

private fun Modifier.readerTapGestures(
    zoom: ReaderZoomState,
    onTap: (xFraction: Float) -> Unit,
): Modifier = pointerInput(zoom) {
    detectTapGestures(
        onTap = { position ->
            val width = zoom.containerSize.width.coerceAtLeast(1)
            onTap(position.x / width)
        },
        onDoubleTap = { position -> zoom.toggleZoom(position) },
    )
}

private fun Modifier.readerZoomLayer(zoom: ReaderZoomState): Modifier = graphicsLayer {
    transformOrigin = TransformOrigin(0f, 0f)
    scaleX = zoom.scale
    scaleY = zoom.scale
    translationX = zoom.offset.x
    translationY = zoom.offset.y
}

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

    var chromeVisible by remember { mutableStateOf(true) }
    var settingsVisible by remember { mutableStateOf(false) }
    val zoom = remember { ReaderZoomState() }
    val focusRequester = remember { FocusRequester() }
    val isWebtoon = state.mode == ComicReadingMode.WEBTOON
    val rtl = state.mode == ComicReadingMode.RIGHT_TO_LEFT

    val spreads = remember(state.pages, state.usesDualPage, state.firstPageAlone) {
        pageSpreads(state.pages, state.usesDualPage, state.firstPageAlone)
    }
    val pagerState = rememberPagerState(pageCount = { spreads.size })
    val listState = rememberLazyListState()

    LaunchedEffect(pagerState, spreads) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .drop(1)
            .collect { index ->
                spreads.getOrNull(index)?.let { controller.setPage(it.startIndex) }
            }
    }
    LaunchedEffect(state.pages, listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .drop(1)
            .collect { index ->
                if (index in state.pages.indices) controller.setPage(index)
            }
    }
    LaunchedEffect(state.chapterIndex, state.pages.size, state.mode, spreads.size) {
        if (state.pages.isEmpty()) return@LaunchedEffect
        if (isWebtoon) {
            listState.scrollToItem(state.pageIndex.coerceIn(0, state.pages.lastIndex))
        } else {
            pagerState.scrollToPage(spreadIndexForPage(spreads, state.pageIndex))
        }
    }
    LaunchedEffect(state.mode, state.chapterIndex) {
        zoom.reset()
        focusRequester.requestFocus()
    }

    fun goToSpread(index: Int) {
        if (spreads.isEmpty()) return
        val target = index.coerceIn(0, spreads.lastIndex)
        controller.setPage(spreads[target].startIndex)
        scope.launch { pagerState.animateScrollToPage(target) }
    }
    fun goToWebtoonPage(index: Int) {
        if (state.pages.isEmpty()) return
        val target = index.coerceIn(0, state.pages.lastIndex)
        controller.setPage(target)
        scope.launch { listState.animateScrollToItem(target) }
    }
    val stepForward: () -> Unit = {
        if (isWebtoon) goToWebtoonPage(state.pageIndex + 1)
        else goToSpread(spreadIndexForPage(spreads, state.pageIndex) + 1)
    }
    val stepBack: () -> Unit = {
        if (isWebtoon) goToWebtoonPage(state.pageIndex - 1)
        else goToSpread(spreadIndexForPage(spreads, state.pageIndex) - 1)
    }
    val onReaderTap: (Float) -> Unit = { xFraction ->
        when {
            xFraction < 1f / 3f -> if (rtl) stepForward() else stepBack()
            xFraction > 2f / 3f -> if (rtl) stepBack() else stepForward()
            else -> chromeVisible = !chromeVisible
        }
    }
    val sliderMax = if (isWebtoon) state.pages.lastIndex else spreads.lastIndex
    val sliderIndex = if (isWebtoon) state.pageIndex else spreadIndexForPage(spreads, state.pageIndex)
    val onSeek: (Int) -> Unit = { requested ->
        if (isWebtoon) goToWebtoonPage(requested) else goToSpread(requested)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionRight -> {
                        if (rtl) stepBack() else stepForward(); true
                    }
                    Key.DirectionLeft -> {
                        if (rtl) stepForward() else stepBack(); true
                    }
                    Key.DirectionDown, Key.PageDown, Key.Spacebar -> {
                        stepForward(); true
                    }
                    Key.DirectionUp, Key.PageUp -> {
                        stepBack(); true
                    }
                    else -> false
                }
            },
    ) {
        when {
            state.error != null -> ReaderError(
                message = state.error.orEmpty(),
                onRetry = controller::retry,
            )
            state.pages.isEmpty() -> CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.align(Alignment.Center),
            )
            isWebtoon -> WebtoonColumn(
                state = state,
                listState = listState,
                comics = comics,
                zoom = zoom,
                onTap = onReaderTap,
                modifier = Modifier.fillMaxSize().testTag("reader-webtoon"),
            )
            else -> PagedPager(
                state = state,
                spreads = spreads,
                pagerState = pagerState,
                comics = comics,
                zoom = zoom,
                onTap = onReaderTap,
                modifier = Modifier.fillMaxSize().testTag("reader-pager"),
            )
        }

        AnimatedVisibility(
            visible = chromeVisible,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            ReaderTopBar(book = book, state = state, onBack = onBack)
        }
        AnimatedVisibility(
            visible = chromeVisible,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            ReaderBottomBar(
                state = state,
                sliderIndex = sliderIndex,
                sliderMax = sliderMax,
                onSeek = onSeek,
                onOpenSettings = { settingsVisible = true },
                controller = controller,
            )
        }
    }

    if (settingsVisible) {
        ReaderSettingsSheet(
            state = state,
            controller = controller,
            onDismiss = { settingsVisible = false },
        )
    }
}

@Composable
private fun PagedPager(
    state: ComicReaderUiState,
    spreads: List<PageSpread>,
    pagerState: PagerState,
    comics: LocalComicRepository,
    zoom: ReaderZoomState,
    onTap: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state.mode) {
        ComicReadingMode.LEFT_TO_RIGHT,
        ComicReadingMode.RIGHT_TO_LEFT,
        -> HorizontalPager(
            state = pagerState,
            modifier = modifier,
            reverseLayout = state.mode == ComicReadingMode.RIGHT_TO_LEFT,
            key = { index -> spreads[index].startIndex },
            beyondViewportPageCount = 1,
        ) { index ->
            SpreadPage(
                spread = spreads[index],
                rtl = state.mode == ComicReadingMode.RIGHT_TO_LEFT,
                comics = comics,
                zoom = zoom,
                onTap = onTap,
            )
        }
        ComicReadingMode.VERTICAL -> VerticalPager(
            state = pagerState,
            modifier = modifier,
            key = { index -> spreads[index].startIndex },
            beyondViewportPageCount = 1,
        ) { index ->
            SpreadPage(
                spread = spreads[index],
                rtl = false,
                comics = comics,
                zoom = zoom,
                onTap = onTap,
            )
        }
        ComicReadingMode.WEBTOON -> Unit
    }
}

@Composable
private fun WebtoonColumn(
    state: ComicReaderUiState,
    listState: LazyListState,
    comics: LocalComicRepository,
    zoom: ReaderZoomState,
    onTap: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .onSizeChanged { zoom.containerSize = it }
            .readerTapGestures(zoom, onTap)
            .zoomableGestures(zoom)
            .clipToBounds()
            .readerZoomLayer(zoom),
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            items(
                count = state.pages.size,
                key = { index -> state.pages[index].location },
            ) { index ->
                ComicPage(
                    page = state.pages[index],
                    comics = comics,
                    modifier = Modifier.fillMaxWidth(),
                    imageModifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.FillWidth,
                )
            }
        }
    }
}

@Composable
private fun SpreadPage(
    spread: PageSpread,
    rtl: Boolean,
    comics: LocalComicRepository,
    zoom: ReaderZoomState,
    onTap: (Float) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { zoom.containerSize = it }
            .readerTapGestures(zoom, onTap)
            .zoomableGestures(zoom)
            .clipToBounds()
            .readerZoomLayer(zoom),
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val pages = if (rtl) spread.pages.asReversed() else spread.pages
            pages.forEach { page ->
                ComicPage(
                    page = page,
                    comics = comics,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
            }
        }
    }
}

@Composable
private fun ComicPage(
    page: LocalPage,
    comics: LocalComicRepository,
    modifier: Modifier = Modifier,
    imageModifier: Modifier = Modifier.fillMaxSize(),
    contentScale: ContentScale = ContentScale.Fit,
) {
    var image by remember(page.location) { mutableStateOf<ImageBitmap?>(null) }
    var error by remember(page.location) { mutableStateOf<String?>(null) }

    LaunchedEffect(page.location) {
        runCatching { decodeImageBitmap(comics.readPage(page)) }
            .onSuccess { image = it }
            .onFailure { failure -> error = failure.message ?: "无法读取页面" }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        val bitmap = image
        when {
            bitmap != null -> Image(
                bitmap = bitmap,
                contentDescription = page.name,
                contentScale = contentScale,
                modifier = imageModifier,
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
    sliderIndex: Int,
    sliderMax: Int,
    onSeek: (Int) -> Unit,
    onOpenSettings: () -> Unit,
    controller: ComicReaderController,
    modifier: Modifier = Modifier,
) {
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
                Text(
                    text = modeLabel(state.mode),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
                if (sliderMax >= 0) {
                    Text(
                        text = "${sliderIndex + 1} / ${sliderMax + 1}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (sliderMax > 0) {
                Slider(
                    value = sliderIndex.toFloat().coerceIn(0f, sliderMax.toFloat()),
                    onValueChange = { onSeek(it.roundToInt()) },
                    valueRange = 0f..sliderMax.toFloat(),
                    steps = (sliderMax - 1).coerceAtLeast(0),
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
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Outlined.Tune, contentDescription = "阅读设置")
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReaderSettingsSheet(
    state: ComicReaderUiState,
    controller: ComicReaderController,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
        ) {
            Text(
                text = "阅读设置",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            Text(
                text = "阅读模式",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            ReaderModeRow(
                label = "从左到右",
                icon = Icons.AutoMirrored.Outlined.ArrowForward,
                selected = state.mode == ComicReadingMode.LEFT_TO_RIGHT,
                onClick = { controller.setMode(ComicReadingMode.LEFT_TO_RIGHT) },
            )
            ReaderModeRow(
                label = "从右到左",
                icon = Icons.AutoMirrored.Outlined.ArrowBack,
                selected = state.mode == ComicReadingMode.RIGHT_TO_LEFT,
                onClick = { controller.setMode(ComicReadingMode.RIGHT_TO_LEFT) },
            )
            ReaderModeRow(
                label = "纵向翻页",
                icon = Icons.Outlined.SwapVert,
                selected = state.mode == ComicReadingMode.VERTICAL,
                onClick = { controller.setMode(ComicReadingMode.VERTICAL) },
            )
            ReaderModeRow(
                label = "Webtoon（连续纵向）",
                icon = Icons.Outlined.ViewStream,
                selected = state.mode == ComicReadingMode.WEBTOON,
                onClick = { controller.setMode(ComicReadingMode.WEBTOON) },
            )
            if (state.supportsDualPage) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "页面布局",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                SettingToggle(
                    label = "双页显示",
                    checked = state.dualPage,
                    onCheckedChange = { controller.setDualPage(it) },
                )
                if (state.dualPage) {
                    SettingToggle(
                        label = "首页单独显示",
                        checked = state.firstPageAlone,
                        onCheckedChange = { controller.setFirstPageAlone(it) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ReaderModeRow(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        if (selected) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = "已选中",
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun SettingToggle(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private fun modeLabel(mode: ComicReadingMode): String = when (mode) {
    ComicReadingMode.LEFT_TO_RIGHT -> "从左到右"
    ComicReadingMode.RIGHT_TO_LEFT -> "从右到左"
    ComicReadingMode.VERTICAL -> "纵向翻页"
    ComicReadingMode.WEBTOON -> "Webtoon"
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
        TextButton(onClick = onRetry) {
            Text("重试", color = Color.White)
        }
    }
}
