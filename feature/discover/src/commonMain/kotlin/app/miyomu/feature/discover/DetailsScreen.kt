package app.miyomu.feature.discover

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.miyomu.core.data.ContentRepository
import app.miyomu.core.model.ContentDetails
import app.miyomu.core.model.ContentId
import app.miyomu.core.model.Outcome

@Composable
fun DetailsScreen(
    repository: ContentRepository,
    contentId: String,
    onBack: () -> Unit,
    onOpenReader: (String, Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var details by remember { mutableStateOf<ContentDetails?>(null) }
    var failed by remember { mutableStateOf(false) }

    LaunchedEffect(contentId) {
        when (val result = repository.details(ContentId(contentId.toLongOrNull() ?: -1L))) {
            is Outcome.Success -> details = result.value
            is Outcome.Failure -> failed = true
        }
    }

    Column(modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = onBack) { Text("← Back") }
        val current = details
        when {
            failed -> Text("Content not found", style = MaterialTheme.typography.headlineSmall)
            current == null -> Text("Loading details…", style = MaterialTheme.typography.bodyLarge)
            else -> {
                Text(current.summary.title, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                Text(
                    current.summary.authors.joinToString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        current.description.orEmpty(),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(16.dp),
                    )
                }
                Text("Chapters", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    items(current.chapters, key = { it.id.value }) { chapter ->
                        Column(
                            Modifier.fillMaxWidth()
                                .clickable { onOpenReader(contentId, chapter.id.value) }
                                .padding(vertical = 10.dp),
                        ) {
                            Text(chapter.title, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Chapter ${chapter.number} · tap to read",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}
