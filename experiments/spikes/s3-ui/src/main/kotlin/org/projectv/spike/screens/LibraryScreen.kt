package org.projectv.spike.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.projectv.spike.model.DemoComic

@Composable
fun LibraryScreen(comics: List<DemoComic>, onOpen: (DemoComic) -> Unit) {
    var query by remember { mutableStateOf("") }
    var stressMode by remember { mutableStateOf(false) }
    val stressComics = remember {
        List(1_000) { index ->
            DemoComic(
                id = 10_000 + index,
                title = "Stress Title ${index + 1}",
                author = "Author ${index % 37 + 1}",
                tags = listOf("Stress"),
                chapters = 20,
                accent = Color.hsv((index * 13 % 360).toFloat(), 0.45f, 0.80f),
            )
        }
    }
    val source = if (stressMode) stressComics else comics
    val filtered = source.filter {
        it.title.contains(query, ignoreCase = true) || it.author.contains(query, ignoreCase = true)
    }

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Library",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "S3 spike · ${if (stressMode) "stress" else "mock"} data · ${filtered.size} items",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.width(260.dp),
                placeholder = { Text("Search title or author") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
            )
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = true, onClick = {}, label = { Text("All") })
            FilterChip(selected = false, onClick = {}, label = { Text("Manga") })
            FilterChip(selected = false, onClick = {}, label = { Text("Novel") })
            FilterChip(selected = false, onClick = {}, label = { Text("Video") })
            FilterChip(
                selected = stressMode,
                onClick = { stressMode = !stressMode },
                label = { Text("Stress 1,000") },
                modifier = Modifier.testTag("stress-toggle"),
            )
        }
        Spacer(Modifier.height(16.dp))
        LazyVerticalGrid(
            columns = GridCells.Adaptive(180.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.fillMaxWidth().weight(1f).testTag("library-grid"),
        ) {
            items(filtered, key = { it.id }) { comic ->
                ComicCard(comic = comic, onClick = { onOpen(comic) })
            }
        }
    }
}

@Composable
private fun ComicCard(comic: DemoComic, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("comic-card-${comic.id}"),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.68f)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.linearGradient(
                        listOf(comic.accent, comic.accent.copy(alpha = 0.45f)),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = comic.title.take(1),
                style = MaterialTheme.typography.displayLarge,
                color = Color.White.copy(alpha = 0.92f),
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = comic.title,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = comic.author,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
