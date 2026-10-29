package app.miyomu.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.miyomu.local.LocalBook
import app.miyomu.model.LibraryItem
import app.miyomu.ui.MiyomuGraph
import app.miyomu.ui.BookCover
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions

class MangaHistoryTab(
    private val graph: MiyomuGraph,
    private val onOpenBook: (LocalBook) -> Unit,
) : Tab {

    override val options: TabOptions
        @Composable get() = TabOptions(index = 2u, title = "历史")

    @Composable
    override fun Content() {
        val history by graph.data.library.observeLibrary().collectAsState(initial = emptyList())
        val books by graph.library.books.collectAsState()

        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "历史",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
            if (history.isEmpty()) {
                EmptyTab(
                    icon = Icons.Outlined.History,
                    title = "还没有阅读记录",
                    body = "打开一本书后，最近阅读会出现在这里。",
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(items = history, key = { it.id }) { item ->
                        val book = books.firstOrNull { it.id == item.sourceKey }
                        HistoryRow(
                            item = item,
                            book = book,
                            comics = graph.comics,
                            onClick = { book?.let(onOpenBook) },
                        )
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(
    item: LibraryItem,
    book: LocalBook?,
    comics: app.miyomu.local.LocalComicRepository,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.width(44.dp).height(60.dp)) {
            if (book != null) {
                BookCover(
                    book = book,
                    comics = comics,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(item.title.take(1), style = MaterialTheme.typography.titleLarge)
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "本地漫画",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(onClick = onClick, enabled = book != null) {
            Text("继续")
        }
    }
}
