package app.miyomu.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.miyomu.local.LocalBook
import app.miyomu.local.LocalBookFormat
import app.miyomu.local.LocalComicRepository

private enum class LibraryFilter(val label: String) {
    ALL("全部"),
    DIRECTORY("目录"),
    ZIP("压缩包"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    books: List<LocalBook>,
    libraryPath: String,
    comics: LocalComicRepository,
    onOpen: (LocalBook) -> Unit,
    onRefresh: () -> Unit,
    onOpenLibraryFolder: (() -> Unit)? = null,
) {
    var searchActive by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var filterIndex by remember { mutableIntStateOf(0) }
    val filter = LibraryFilter.entries[filterIndex]

    val visibleBooks = books
        .filter { book -> query.isBlank() || book.title.contains(query, ignoreCase = true) }
        .filter { book ->
            when (filter) {
                LibraryFilter.ALL -> true
                LibraryFilter.DIRECTORY -> book.format == LocalBookFormat.DIRECTORY
                LibraryFilter.ZIP -> book.format == LocalBookFormat.ZIP
            }
        }

    Column(modifier = Modifier.fillMaxSize()) {
        if (searchActive) {
            TextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("搜索书架") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                trailingIcon = {
                    IconButton(
                        onClick = {
                            if (query.isNotEmpty()) query = "" else searchActive = false
                        },
                    ) {
                        Icon(Icons.Outlined.Close, contentDescription = "关闭搜索")
                    }
                },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
            )
        } else {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("书架", maxLines = 1)
                        if (books.isNotEmpty()) {
                            Spacer(Modifier.width(8.dp))
                            CountPill(books.size)
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { searchActive = true }) {
                        Icon(Icons.Outlined.Search, contentDescription = "搜索")
                    }
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "刷新")
                    }
                    if (onOpenLibraryFolder != null) {
                        IconButton(onClick = onOpenLibraryFolder) {
                            Icon(Icons.Outlined.FolderOpen, contentDescription = "打开目录")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        }

        PrimaryScrollableTabRow(
            selectedTabIndex = filterIndex,
            edgePadding = 0.dp,
            divider = {},
        ) {
            LibraryFilter.entries.forEachIndexed { index, item ->
                val count = when (item) {
                    LibraryFilter.ALL -> books.size
                    LibraryFilter.DIRECTORY -> books.count { it.format == LocalBookFormat.DIRECTORY }
                    LibraryFilter.ZIP -> books.count { it.format == LocalBookFormat.ZIP }
                }
                Tab(
                    selected = filterIndex == index,
                    onClick = { filterIndex = index },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(item.label)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = count.toString(),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    unselectedContentColor = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        HorizontalDivider()

        when {
            books.isEmpty() -> EmptyLibrary(libraryPath = libraryPath, onRefresh = onRefresh)
            visibleBooks.isEmpty() -> EmptyFilterResult()
            else -> LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 112.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(items = visibleBooks, key = { it.id }) { book ->
                    CompactBookCard(
                        book = book,
                        comics = comics,
                        onClick = { onOpen(book) },
                    )
                }
            }
        }
    }
}

@Composable
private fun CountPill(count: Int) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f),
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun EmptyLibrary(libraryPath: String, onRefresh: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.MenuBook,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(56.dp).height(56.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text("书架是空的", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            text = "把漫画目录或 CBZ/ZIP 放进：\n$libraryPath",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Text(
                text = "刷新书架",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.clickable(onClick = onRefresh).padding(horizontal = 20.dp, vertical = 10.dp),
            )
        }
    }
}

@Composable
private fun EmptyFilterResult() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = "没有匹配的书",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CompactBookCard(
    book: LocalBook,
    comics: LocalComicRepository,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("book-${book.id}")
            .aspectRatio(0.68f)
            .clip(RoundedCornerShape(4.dp))
            .clickable(onClick = onClick),
    ) {
        BookCover(
            book = book,
            comics = comics,
            modifier = Modifier.fillMaxSize(),
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.36f)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        1f to Color(0xCC000000),
                    ),
                ),
        )

        Surface(
            modifier = Modifier.align(Alignment.TopStart).padding(4.dp),
            shape = RoundedCornerShape(4.dp),
            color = Color(0x99000000),
        ) {
            Text(
                text = book.format.shortLabel(),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
            )
        }

        Text(
            text = book.title,
            style = MaterialTheme.typography.titleSmall.copy(
                color = Color.White,
                shadow = Shadow(color = Color.Black, blurRadius = 4f),
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp),
        )
    }
}

@Composable
fun BookCover(
    book: LocalBook,
    comics: LocalComicRepository,
    modifier: Modifier = Modifier,
) {
    val image by produceState<ImageBitmap?>(initialValue = null, book.id) {
        value = comics.cover(book)?.let(::decodeImageBitmap)
    }

    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        val bitmap = image
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = book.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(
                text = book.title.take(1),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

internal fun LocalBookFormat.shortLabel(): String = when (this) {
    LocalBookFormat.DIRECTORY -> "目录"
    LocalBookFormat.ZIP -> "ZIP"
    LocalBookFormat.SEVEN_ZIP -> "7Z"
    LocalBookFormat.PDF -> "PDF"
    LocalBookFormat.EPUB -> "EPUB"
    LocalBookFormat.TXT -> "TXT"
}
