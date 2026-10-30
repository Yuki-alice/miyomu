package app.miyomu.ui.novel

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.miyomu.reader.novel.NovelHighlight

/**
 * P1-7 高亮/批注: edit dialog for one scroll-paragraph highlight — an optional note plus
 * delete when the paragraph was already highlighted.
 */
@Composable
internal fun HighlightEditDialog(
    existing: NovelHighlight?,
    onSave: (note: String) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    var note by remember { mutableStateOf(existing?.note.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "添加批注" else "编辑批注") },
        text = {
            Column {
                Text(
                    text = if (existing == null) {
                        "为本段落保存高亮，可附一句批注。"
                    } else {
                        "本段落已有高亮；修改批注或删除。"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("批注") },
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(note.trim()) }) {
                Text(if (existing == null) "保存高亮" else "保存修改")
            }
        },
        dismissButton = {
            if (existing != null) {
                TextButton(onClick = onDelete) { Text("删除高亮", color = MaterialTheme.colorScheme.error) }
            } else {
                TextButton(onClick = onDismiss) { Text("取消") }
            }
        },
    )
}
