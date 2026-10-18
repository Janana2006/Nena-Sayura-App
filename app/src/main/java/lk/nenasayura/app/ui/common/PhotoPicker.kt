package lk.nenasayura.app.ui.common

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import java.io.File

/** Opens the phone's camera or the photo gallery and gives back the chosen picture. */
class PhotoPicker internal constructor(
    val launchCamera: () -> Unit,
    val launchGallery: () -> Unit
)

/**
 * [onPicked] receives the picture the user took or chose.
 * The camera needs no permission (it uses the phone's camera app). The gallery uses the
 * system photo picker, which needs no permission either.
 */
@Composable
fun rememberPhotoPicker(onPicked: (Uri) -> Unit): PhotoPicker {
    val context = LocalContext.current
    // where the camera is saving the picture (kept if the screen turns while the camera is open)
    var cameraUri by rememberSaveable { mutableStateOf<Uri?>(null) }

    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        if (saved) cameraUri?.let(onPicked)
    }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) onPicked(uri)
    }

    return remember(camera, gallery) {
        PhotoPicker(
            launchCamera = {
                val uri = newCameraUri(context)
                cameraUri = uri
                camera.launch(uri)
            },
            launchGallery = {
                gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
        )
    }
}

/** An empty file in the app's cache that the camera app fills with the new photo. */
private fun newCameraUri(context: Context): Uri {
    val folder = File(context.cacheDir, "camera").apply { mkdirs() }
    val file = File(folder, "photo_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

/** "Take a photo / Choose from gallery / Remove photo". */
@Composable
fun PhotoSourceDialog(
    hasPhoto: Boolean,
    onCamera: () -> Unit,
    onGallery: () -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Profile photo") },
        text = {
            Column {
                SourceRow(Icons.Filled.PhotoCamera, "Take a photo", onCamera)
                SourceRow(Icons.Filled.PhotoLibrary, "Choose from gallery", onGallery)
                if (hasPhoto) {
                    SourceRow(Icons.Filled.Delete, "Remove photo", onRemove, danger = true)
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun SourceRow(icon: ImageVector, label: String, onClick: () -> Unit, danger: Boolean = false) {
    val color = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = if (danger) color else MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = label, style = MaterialTheme.typography.bodyLarge, color = color)
    }
}