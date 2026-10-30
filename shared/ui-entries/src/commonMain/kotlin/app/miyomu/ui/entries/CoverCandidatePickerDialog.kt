package app.miyomu.ui.entries

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import app.miyomu.local.LocalBook
import app.miyomu.ui.MiyomuGraph
import app.miyomu.ui.localCoverCandidates
import app.miyomu.ui.saveCoverOverride
import coil3.compose.AsyncImage
import java.io.File

/**
 * P1-1 封面候选: pick one of the directory book's top-level images as its cover. The
 * selection persists through [saveCoverOverride] and the cover pipeline honors it
 * ([app.miyomu.ui.core.localCoverOverrideBytes]).
 */
@Composable
internal fun CoverCandidatePickerDialog(
    graph: MiyomuGraph,
    book: LocalBook,
    onDismiss: () -> Unit,
) {
    val candidates = localCoverCandidates(book)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择封面") },
        text = {
            Column {
                if (candidates.isEmpty()) {
                    Text(
                        text = "该书目没有候选封面图片（单文件书目的封面为首页）。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text(
                        text = "点击图片将其设为封面：",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(96.dp),
                        modifier = Modifier.fillMaxWidth().height(240.dp).padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(candidates, key = { it.absolutePath }) { candidate ->
                            AsyncImage(
                                model = candidate,
                                contentDescription = candidate.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(96.dp)
                                    .clickable {
                                        saveCoverOverride(graph.preferenceStore, book, candidate)
                                        onDismiss()
                                    },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("完成") }
        },
    )
}
