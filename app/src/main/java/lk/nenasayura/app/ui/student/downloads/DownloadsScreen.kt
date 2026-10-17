package lk.nenasayura.app.ui.student.downloads

import android.text.format.Formatter
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import lk.nenasayura.app.ui.common.NenaTopBar
import lk.nenasayura.app.ui.theme.NenaPrimary
import lk.nenasayura.app.ui.theme.NenaPrimaryContainer
import lk.nenasayura.app.ui.theme.NenaSecondaryContainer
import lk.nenasayura.app.util.DownloadStore
import lk.nenasayura.app.util.DownloadedFile
import lk.nenasayura.app.util.FileActions
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Everything the student saved inside the app. Opens with no internet because the files
 * are on the phone (app private storage), not on Firebase / Cloudinary.
 */
@Composable
fun DownloadsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { DownloadStore.load(context) }

    val items by DownloadStore.items.collectAsState()
    val busy by DownloadStore.busy.collectAsState()
    var toDelete by remember { mutableStateOf<DownloadedFile?>(null) }

    Scaffold(
        topBar = { NenaTopBar(title = "Downloads", onBack = onBack) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        if (items.isEmpty() && busy.isEmpty()) {
            EmptyDownloads(modifier = Modifier.padding(innerPadding))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    val total = items.sumOf { it.sizeBytes }
                    Text(
                        text = "${items.size} file(s) saved in the app" +
                                " - " + Formatter.formatShortFileSize(context, total) +
                                ". They open without internet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (busy.isNotEmpty()) {
                    item {
                        Text(
                            text = "Downloading ${busy.size} file(s)...",
                            style = MaterialTheme.typography.labelLarge,
                            color = NenaPrimary
                        )
                    }
                }
                items(items, key = { it.id }) { file ->
                    DownloadedCard(
                        file = file,
                        onOpen = { FileActions.openFile(context, file.url, file.title) },
                        onDelete = { toDelete = file }
                    )
                }
            }
        }
    }

    toDelete?.let { file ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Delete this download?") },
            text = {
                Text("\"${file.title}\" will be removed from this phone. You can download it again later when you have internet.")
            },
            confirmButton = {
                TextButton(onClick = {
                    DownloadStore.delete(context, file.id)
                    toDelete = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { toDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun DownloadedCard(
    file: DownloadedFile,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val date = remember(file.savedAtMillis) {
        SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(file.savedAtMillis))
    }
    val ext = file.fileName.substringAfterLast('.', "").uppercase(Locale.getDefault())

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(NenaPrimaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Description, contentDescription = null, tint = NenaPrimary)
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = file.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = listOf(ext, Formatter.formatShortFileSize(context, file.sizeBytes), date)
                            .filter { it.isNotBlank() }
                            .joinToString(" - "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Surface(shape = RoundedCornerShape(8.dp), color = NenaSecondaryContainer) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.DownloadDone,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = Color(0xFF004D40)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Available offline",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF004D40)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Visibility, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Open")
            }
        }
    }
}

@Composable
private fun EmptyDownloads(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Filled.DownloadDone,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = NenaPrimary
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "No downloads yet",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Tap Download on a paper, marking scheme or guide book. It is saved inside the app and opens here even without internet.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}