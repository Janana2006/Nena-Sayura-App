package lk.nenasayura.app.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import lk.nenasayura.app.data.model.StudyStream
import lk.nenasayura.app.data.model.Subject

/**
 * Two drop-downs one after the other: first the stream, then a subject of that stream.
 * Only the chosen subject is reported; changing the stream clears it.
 */
@Composable
fun StreamSubjectPicker(
    streams: List<StudyStream>,
    selectedSubject: Subject?,
    enabled: Boolean,
    onSubjectSelected: (Subject?) -> Unit
) {
    var stream by remember { mutableStateOf<StudyStream?>(null) }

    Column {
        Text("Stream", style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(6.dp))
        PickerDropdown(
            items = streams,
            selected = stream,
            placeholder = if (streams.isEmpty()) "No streams yet. Add one in Streams." else "Select stream",
            enabled = enabled,
            label = { it.name },
            onSelect = {
                stream = it
                onSubjectSelected(null)
            }
        )

        Spacer(modifier = Modifier.height(12.dp))
        Text("Subject", style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(6.dp))
        PickerDropdown(
            items = stream?.subjects ?: emptyList(),
            selected = selectedSubject,
            placeholder = if (stream == null) "Choose a stream first" else "Select subject",
            enabled = enabled && stream != null,
            label = { it.name },
            onSelect = { onSubjectSelected(it) }
        )
    }
}

/** A drop-down for any list. */
@Composable
private fun <T> PickerDropdown(
    items: List<T>,
    selected: T?,
    placeholder: String,
    enabled: Boolean,
    label: (T) -> String,
    onSelect: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Surface(
            onClick = { expanded = true },
            enabled = enabled,
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (selected != null) label(selected) else placeholder,
                    color = if (selected != null) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
            }
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(label(item)) },
                    onClick = {
                        onSelect(item)
                        expanded = false
                    }
                )
            }
        }
    }
}