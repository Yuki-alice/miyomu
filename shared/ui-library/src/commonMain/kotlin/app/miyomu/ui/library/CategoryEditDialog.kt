package app.miyomu.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.miyomu.model.Category

/**
 * Category membership editor for one item (Aniyomi's edit-categories dialog): check the
 * categories the item belongs to, optionally create a new one on the fly.
 */
@Composable
fun CategoryEditDialog(
    categories: List<Category>,
    memberOf: Set<Long>,
    onDismiss: () -> Unit,
    onConfirm: (selectedIds: List<Long>, newCategoryName: String?) -> Unit,
) {
    val checked = remember { mutableStateMapOf<Long, Boolean>().apply { memberOf.forEach { put(it, true) } } }
    var newName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑分类") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (categories.isEmpty()) {
                    Text(
                        text = "还没有分类，可在下方新建。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                LazyColumn(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    items(categories, key = { it.id }) { category ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Checkbox(
                                checked = checked[category.id] ?: false,
                                onCheckedChange = { checked[category.id] = it },
                            )
                            Text(category.name)
                        }
                    }
                }
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("新建分类") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(
                    checked.filterValues { it }.keys.toList(),
                    newName.trim().takeIf { it.isNotEmpty() },
                )
                onDismiss()
            }) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}
