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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.miyomu.core.model.ContentFilter
import app.miyomu.core.model.ContentSummary
import app.miyomu.core.model.FilterTag
import app.miyomu.core.ui.MiyomuCover
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(
    onOpenDetails: (sourceName: String, contentId: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: DiscoverViewModel = koinViewModel()
    val state by viewModel.state.collectAsState()

    Column(modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("Browse", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                Text(
                    "SourceRuntime → Repository → UI",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = viewModel::openFilter) { Text("Filter") }
        }
        OutlinedTextField(
            value = state.query,
            onValueChange = viewModel::search,
            singleLine = true,
            placeholder = { Text("Search titles…") },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 8.dp),
        ) {
            items(state.sources, key = { it.id.value }) { source ->
                FilterChip(
                    selected = source.name == state.selectedSource,
                    onClick = { viewModel.selectSource(source.name) },
                    label = { Text(source.name) },
                )
            }
        }
        when {
            state.loading -> Text("Loading source…", Modifier.padding(24.dp))
            state.error != null -> Column(Modifier.padding(24.dp)) {
                Text("Failed: ${state.error!!::class.simpleName}")
                TextButton(onClick = viewModel::refresh) { Text("Retry") }
            }
            else -> LazyColumn(
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(state.items, key = { "${it.source.value}:${it.id.value}" }) { item ->
                    ContentRow(
                        item = item,
                        onClick = { onOpenDetails(item.source.value, item.id.value) },
                    )
                }
            }
        }
    }

    if (state.showFilter) {
        ModalBottomSheet(onDismissRequest = viewModel::closeFilter) {
            FilterSheet(
                initial = state.filter,
                availableTags = state.filterOptions.tags,
                availableStates = state.filterOptions.states,
                sortOrders = state.filterOptions.sortOrders,
                onApply = viewModel::applyFilter,
                onClear = viewModel::clearFilter,
            )
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
                "${item.contentType} · ${item.state} · ${item.source.value}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun FilterSheet(
    initial: ContentFilter,
    availableTags: List<FilterTag>,
    availableStates: Set<String>,
    sortOrders: Set<String>,
    onApply: (ContentFilter) -> Unit,
    onClear: () -> Unit,
) {
    var draft by remember(initial) { mutableStateOf(initial) }

    Column(
        Modifier.fillMaxWidth().heightIn(max = 560.dp).padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Filters", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            TextButton(onClick = onClear) { Text("Clear") }
        }

        if (sortOrders.isNotEmpty()) {
            Text("Sort", style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(sortOrders.toList()) { order ->
                    FilterChip(
                        selected = draft.order == order,
                        onClick = {
                            draft = draft.copy(order = if (draft.order == order) null else order)
                        },
                        label = { Text(order) },
                    )
                }
            }
        }

        if (availableStates.isNotEmpty()) {
            Text("Status", style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(availableStates.toList()) { state ->
                    FilterChip(
                        selected = state in draft.states,
                        onClick = {
                            draft = draft.copy(
                                states = if (state in draft.states) draft.states - state else draft.states + state,
                            )
                        },
                        label = { Text(state) },
                    )
                }
            }
        }

        OutlinedTextField(
            value = draft.year?.toString().orEmpty(),
            onValueChange = { value ->
                draft = draft.copy(year = value.filter { it.isDigit() }.take(4).toIntOrNull())
            },
            singleLine = true,
            label = { Text("Year") },
            modifier = Modifier.fillMaxWidth(),
        )

        if (availableTags.isNotEmpty()) {
            Text("Tags (${draft.tags.size})", style = MaterialTheme.typography.labelLarge)
            LazyVerticalGrid(
                columns = GridCells.Adaptive(100.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.heightIn(max = 220.dp),
            ) {
                items(availableTags, key = { it.id }) { tag ->
                    FilterChip(
                        selected = tag in draft.tags,
                        onClick = {
                            draft = draft.copy(
                                tags = if (tag in draft.tags) draft.tags - tag else draft.tags + tag,
                            )
                        },
                        label = { Text(tag.title, maxLines = 1) },
                    )
                }
            }
        }

        Button(
            onClick = { onApply(draft) },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        ) { Text("Apply") }
    }
}
