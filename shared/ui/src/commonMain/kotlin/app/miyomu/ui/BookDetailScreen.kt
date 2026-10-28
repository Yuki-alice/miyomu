package app.miyomu.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.miyomu.data.MiyomuData
import app.miyomu.local.LocalBook
import app.miyomu.local.LocalChapter
import app.miyomu.local.LocalComicRepository
import app.miyomu.model.LibraryItem
import app.miyomu.model.ReadingProgress

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailScreen(
    book: LocalBook,
    comics: LocalComicRepository,
    data: MiyomuData,
    onBack: () -> Unit,
    onRead: (chapterIndex: Int, pageIndex: Int) -> Unit,
) {
    var chapters by remember(book.id) { mutableStateOf<List<LocalChapter>>(emptyList()) }
    var latestProgress by remember(book.id) { mutableStateOf<ReadingProgress?>(null) }
    var loading by remember(book.id) { mutableStateOf(true) }
    var error by remember(book.id) { mutableStateOf<String?>(null) }

    LaunchedEffect(book.id) {
        loading = true
        error = null
        runCatching { comics.chapters(book) }
            .onSuccess { chapters = it }
            .onFailure { error = it.message ?: "无法读取章节" }
        latestProgress = runCatching {
            data.progress.latest(LibraryItem.idOf("local", book.id))
        }.getOrNull()
        loading = false
    }

    val savedChapterIndex = chapters.indexOfFirst { it.id == latestProgress?.entryId }
    val resumeChapter = if (savedChapterIndex >= 0) savedChapterIndex else 0
    val resumePage = if (savedChapterIndex >= 0) {
        latestProgress?.position?.toInt()?.coerceAtLeast(0) ?: 0
    } else {
        0
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    text = book.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        )

        when {
            loading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            error != null -> Box(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                Text(
                    text = error.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                )
            }
            else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    DetailHeader(
                        book = book,
                        comics = comics,
                        latestProgress = latestProgress,
                        resumeChapter = resumeChapter,
                        resumePage = resumePage,
                        onRead = onRead,
                    )
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "${chapters.size} 章",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = "按文件顺序",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                itemsIndexed(items = chapters, key = { _, chapter -> chapter.id }) { index, chapter ->
                    ChapterRow(
                        chapter = chapter,
                        index = index,
                        latestChapterIndex = savedChapterIndex,
                        latestProgress = latestProgress,
                        onClick = { onRead(index, 0) },
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailHeader(
    book: LocalBook,
    comics: LocalComicRepository,
    latestProgress: ReadingProgress?,
    resumeChapter: Int,
    resumePage: Int,
    onRead: (chapterIndex: Int, pageIndex: Int) -> Unit,
) {
    val cover by produceState<ImageBitmap?>(initialValue = null, book.id) {
        value = comics.cover(book)?.let(::decodeImageBitmap)
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        val backdrop = cover
        if (backdrop != null) {
            Image(
                bitmap = backdrop,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer(alpha = 0.18f),
            )
        }
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        0f to MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
                        1f to MaterialTheme.colorScheme.surface,
                    ),
                ),
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            CoverImage(
                bitmap = cover,
                title = book.title,
                modifier = Modifier.width(100.dp).height(140.dp),
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.PersonOutline,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "本地文件",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = book.format.shortLabel(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = " · 本地",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = { onRead(resumeChapter, resumePage) },
                    modifier = Modifier.testTag("start-reading"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(if (latestProgress != null) "继续阅读" else "开始阅读")
                }
            }
        }
    }

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(8.dp))
        Text(
            text = "本地路径",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = book.location,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun CoverImage(
    bitmap: ImageBitmap?,
    title: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(
                text = title.take(1),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ChapterRow(
    chapter: LocalChapter,
    index: Int,
    latestChapterIndex: Int,
    latestProgress: ReadingProgress?,
    onClick: () -> Unit,
) {
    val read = latestChapterIndex >= 0 && index <= latestChapterIndex
    val contentAlpha = if (read) 0.62f else 1f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("chapter-$index")
            .clickable(onClick = onClick)
            .padding(start = 16.dp, top = 12.dp, end = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.width(12.dp)) {
            if (!read) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(50)),
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = chapter.title,
                style = MaterialTheme.typography.bodyMedium,
                color = if (read) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "本地章节",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (latestProgress != null && index == latestChapterIndex) {
            Text(
                text = "读到第 ${latestProgress.position.toInt() + 1} 页",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        } else {
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha),
            )
        }
    }
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
    )
}
