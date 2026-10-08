package lk.nenasayura.app.ui.papers

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import lk.nenasayura.app.util.CloudinaryUploader
import lk.nenasayura.app.util.PaperRepository

@Composable
fun UploadPaperScreen(
    streamId: String,
    subjectId: String,
    modifier: Modifier = Modifier,
    onUploaded: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var title by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Past") }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf("") }
    var isUploading by remember { mutableStateOf(false) }

    val categories = listOf("Past", "Model", "Provincial")

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedUri = uri
            selectedFileName = getFileNameFromUri(context, uri)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Upload Past / Model Paper",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Paper Title") },
            placeholder = { Text("e.g. 2023 Chemistry Paper I") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !isUploading
        )

        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Category",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                categories.forEach { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { if (!isUploading) selectedCategory = category },
                        label = { Text(category) },
                        enabled = !isUploading
                    )
                }
            }
        }

        OutlinedButton(
            onClick = { pdfPickerLauncher.launch("application/pdf") },
            enabled = !isUploading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (selectedFileName.isBlank()) "Select PDF" else "Change PDF ($selectedFileName)")
        }

        if (selectedFileName.isNotBlank()) {
            Text(
                text = "Selected File: $selectedFileName",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth()
            )
        }

        val isUploadEnabled = selectedUri != null && title.isNotBlank() && !isUploading

        Button(
            onClick = {
                val uid = FirebaseAuth.getInstance().currentUser?.uid
                if (uid == null) {
                    Toast.makeText(context, "User not authenticated. Please log in.", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                val uri = selectedUri
                if (uri == null) {
                    Toast.makeText(context, "Please select a PDF file.", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                coroutineScope.launch {
                    isUploading = true
                    val uploadResult = CloudinaryUploader.uploadPdf(
                        context = context,
                        uri = uri,
                        fileName = if (selectedFileName.isNotBlank()) selectedFileName else title
                    )

                    uploadResult.fold(
                        onSuccess = { downloadUrl ->
                            val saveResult = PaperRepository.savePaper(
                                title = title.trim(),
                                streamId = streamId,
                                subjectId = subjectId,
                                category = selectedCategory,
                                url = downloadUrl,
                                uploadedBy = uid
                            )

                            saveResult.fold(
                                onSuccess = {
                                    Toast.makeText(context, "Paper uploaded successfully!", Toast.LENGTH_SHORT).show()
                                    title = ""
                                    selectedUri = null
                                    selectedFileName = ""
                                    onUploaded()
                                },
                                onFailure = { e ->
                                    Toast.makeText(context, "Failed to save paper: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                }
                            )
                        },
                        onFailure = { e ->
                            Toast.makeText(context, "Upload failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                        }
                    )
                    isUploading = false
                }
            },
            enabled = isUploadEnabled,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isUploading) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Text("Uploading...")
                }
            } else {
                Text("Upload Paper")
            }
        }
    }
}

private fun getFileNameFromUri(context: Context, uri: Uri): String {
    var fileName = ""
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    fileName = it.getString(nameIndex) ?: ""
                }
            }
        }
    }
    if (fileName.isBlank()) {
        fileName = uri.lastPathSegment ?: "selected_document.pdf"
    }
    return fileName
}
