package com.example.taptoplay.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.taptoplay.adyen.SaleToAcquirerDataConfig
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull

@Composable
internal fun SaleToAcquirerDataDialog(
    config: SaleToAcquirerDataConfig,
    onEdit: (List<String>, String) -> Unit,
    onRemove: (List<String>) -> Unit,
    onApply: () -> Unit,
    onSaveFavorite: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .height(620.dp),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(Modifier.fillMaxWidth()) {
                        Text("SaleToAcquirerData", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Text(
                            "${config.displayName} | ${config.fieldCount} fields",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        item {
                            OutlinedButton(onClick = onSaveFavorite) { Text("Save", maxLines = 1) }
                        }
                        item {
                            OutlinedButton(onClick = onApply) { Text("Apply", maxLines = 1) }
                        }
                        item {
                            TextButton(onClick = onDismiss) { Text("Close", maxLines = 1) }
                        }
                    }
                }
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
                    items(config.data.entries.toList()) { (key, value) ->
                        JsonNodeRow(
                            name = key,
                            value = value,
                            depth = 0,
                            path = listOf(key),
                            editable = true,
                            onEdit = onEdit,
                            onRemove = onRemove,
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun JsonNodeRow(name: String, value: JsonElement, depth: Int) {
    JsonNodeRow(
        name = name,
        value = value,
        depth = depth,
        path = emptyList(),
        editable = false,
        onEdit = { _, _ -> },
        onRemove = {},
    )
}

@Composable
private fun JsonNodeRow(
    name: String,
    value: JsonElement,
    depth: Int,
    path: List<String>,
    editable: Boolean,
    onEdit: (List<String>, String) -> Unit,
    onRemove: (List<String>) -> Unit,
) {
    var expanded by remember { mutableStateOf(depth == 0) }
    var editing by remember { mutableStateOf(false) }
    var editedValue by remember(value) { mutableStateOf(value.editableText()) }
    val isExpandable = value is JsonObject || value is JsonArray
    val summary = value.summary()
    OutlinedCard(shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = (12 + depth * 12).dp, top = 10.dp, end = 12.dp, bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(summary, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                if (isExpandable) {
                    TextButton(onClick = { expanded = !expanded }) {
                        Text(if (expanded) "Hide" else "View")
                    }
                } else if (editable) {
                    TextButton(onClick = { editing = !editing }) {
                        Text(if (editing) "Cancel" else "Edit")
                    }
                }
            }
            if (editing && editable && !isExpandable) {
                OutlinedTextField(
                    value = editedValue,
                    onValueChange = { editedValue = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Value") },
                    singleLine = false,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            onEdit(path, editedValue)
                            editing = false
                        },
                    ) {
                        Text("Save")
                    }
                    TextButton(onClick = { onRemove(path) }) {
                        Text("Remove")
                    }
                }
            }
            if (editable && isExpandable && depth > 0) {
                TextButton(onClick = { onRemove(path) }) {
                    Text("Remove group")
                }
            }
            if (expanded && value is JsonObject) {
                value.entries.forEach { (childKey, childValue) ->
                    JsonNodeRow(
                        name = childKey,
                        value = childValue,
                        depth = depth + 1,
                        path = path + childKey,
                        editable = editable,
                        onEdit = onEdit,
                        onRemove = onRemove,
                    )
                }
            }
            if (expanded && value is JsonArray) {
                value.forEachIndexed { index, childValue ->
                    JsonNodeRow(
                        name = "[$index]",
                        value = childValue,
                        depth = depth + 1,
                        path = path + index.toString(),
                        editable = false,
                        onEdit = onEdit,
                        onRemove = onRemove,
                    )
                }
            }
        }
    }
}

private fun JsonElement.summary(): String = when (this) {
    is JsonObject -> "${size} field${if (size == 1) "" else "s"}"
    is JsonArray -> "${size} item${if (size == 1) "" else "s"}"
    is JsonPrimitive -> when {
        isString -> contentOrNull.orEmpty()
        booleanOrNull != null -> booleanOrNull.toString()
        longOrNull != null -> longOrNull.toString()
        doubleOrNull != null -> doubleOrNull.toString()
        else -> toString()
    }
}

private fun JsonElement.editableText(): String = when (this) {
    is JsonPrimitive -> contentOrNull ?: toString()
    else -> toString()
}
