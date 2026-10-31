package app.miyomu.feature.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.miyomu.core.model.ContentSummary
import app.miyomu.core.ui.MiyomuCover
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DiscoverScreen(
    onOpenDetails: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: DiscoverViewModel = koinViewModel()
    val state by viewModel.state.collectAsState()

    Column(modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Text("Browse", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text(
            "Demo source · SourceRuntime → Repository → UI",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        when {
            state.loading -> Text("Loading fixture source…", Modifier.padding(24.dp))
            state.error != null -> Text("Failed: ${state.error!!::class.simpleName}", Modifier.padding(24.dp))
            else -> LazyColumn(
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(state.items, key = { it.id.value }) { item ->
                    ContentRow(item = item, onClick = { onOpenDetails(item.id.value) })
                }
            }
        }
    }
}

@Composable
private fun ContentRow(item: ContentSummary, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MiyomuCover(
            url = item.coverUrl,
            title = item.title,
            modifier = Modifier.height(96.dp).fillMaxWidth(.22f).clip(RoundedCornerShape(12.dp)),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                item.authors.joinToString(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "${item.contentType} · ${item.state} · ★ ${item.rating ?: "-"}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
