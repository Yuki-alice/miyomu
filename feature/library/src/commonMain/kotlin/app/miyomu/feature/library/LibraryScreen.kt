package app.miyomu.feature.library

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.miyomu.core.model.ContentSummary
import app.miyomu.core.ui.MiyomuCover
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun LibraryScreen(
    onOpenDetails: (String, Long) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
) {
    val viewModel: LibraryViewModel = koinViewModel()
    val favourites by viewModel.favourites.collectAsState()

    Column(
        modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("Good evening", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                Text(
                    "${favourites.size} saved · MangaDex + Demo",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                "Library",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Continue reading", style = MaterialTheme.typography.labelLarge)
                Text("Sky Archive", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Chapter 80 · The library that drifts", style = MaterialTheme.typography.bodyMedium)
                Button(
                    onClick = { onOpenDetails("mangadex", 1L) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onPrimary,
                        contentColor = MaterialTheme.colorScheme.primary,
                    ),
                ) { Text("Continue") }
            }
        }

        Text("Saved", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        if (favourites.isEmpty()) {
            Text(
                "No saved titles yet. Open Browse, tap a title, then press ☆ Save.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(132.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(favourites, key = { it.id.value }) { item ->
                    FavouriteCard(item = item, onClick = { onOpenDetails(item.source.value, item.id.value) })
                }
            }
        }
    }
}

@Composable
private fun FavouriteCard(item: ContentSummary, onClick: () -> Unit) {
    Column(Modifier.clickable(onClick = onClick), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        MiyomuCover(
            url = item.coverUrl,
            title = item.title,
            modifier = Modifier.fillMaxWidth().height(176.dp).clip(RoundedCornerShape(18.dp)),
        )
        Text(item.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Text(
            item.authors.joinToString(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box(
            Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(99.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
    }
}

@Composable
fun HomeScreen(
    onOpenDetails: (String, Long) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
) = LibraryScreen(onOpenDetails, modifier)
