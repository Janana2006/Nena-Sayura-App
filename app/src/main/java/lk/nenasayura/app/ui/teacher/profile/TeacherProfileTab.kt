package lk.nenasayura.app.ui.teacher.profile

import android.content.Context
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.util.Locale
import lk.nenasayura.app.data.model.Subject
import lk.nenasayura.app.data.model.TeacherProfile
import lk.nenasayura.app.ui.common.PhotoSourceDialog
import lk.nenasayura.app.ui.common.ProfileAvatar
import lk.nenasayura.app.ui.common.RoleSwitchCard
import lk.nenasayura.app.ui.common.rememberPhotoPicker
import lk.nenasayura.app.ui.student.profile.FormCard
import lk.nenasayura.app.ui.student.profile.PrimaryActionButton
import lk.nenasayura.app.ui.student.profile.SubPageHeader
import lk.nenasayura.app.ui.theme.NenaAccentContainer
import lk.nenasayura.app.ui.theme.NenaPrimary
import lk.nenasayura.app.ui.theme.NenaPrimaryContainer
import lk.nenasayura.app.ui.theme.NenaSecondary
import lk.nenasayura.app.ui.theme.NenaSecondaryContainer

/**
 * Profile tab of the teacher. Same look as the student profile:
 * gradient header, white cards, and the edit form opens as its own page.
 * (Bank details live in the Payments tab.)
 */
@Composable
fun TeacherProfileTab(
    profile: TeacherProfile,
    streamSubjects: List<Subject>,
    saving: Boolean,
    onLoadSubjects: () -> Unit,
    onSave: (TeacherProfile, () -> Unit) -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
    onRoleChanged: (String) -> Unit = {},
    photoUploading: Boolean = false,
    onUploadPhoto: (Context, Uri) -> Unit = { _, _ -> },
    onRemovePhoto: () -> Unit = {}
) {
    var editing by remember { mutableStateOf(false) }

    // Profile photo: "Take a photo / Choose from gallery / Remove photo"
    val context = LocalContext.current
    var showPhotoDialog by rememberSaveable { mutableStateOf(false) }
    val photoPicker = rememberPhotoPicker { uri -> onUploadPhoto(context, uri) }
    if (showPhotoDialog) {
        PhotoSourceDialog(
            hasPhoto = profile.photoUrl.isNotBlank(),
            onCamera = {
                showPhotoDialog = false
                photoPicker.launchCamera()
            },
            onGallery = {
                showPhotoDialog = false
                photoPicker.launchGallery()
            },
            onRemove = {
                showPhotoDialog = false
                onRemovePhoto()
            },
            onDismiss = { showPhotoDialog = false }
        )
    }

    // Back from the edit page returns to the profile overview
    BackHandler(enabled = editing) { if (!saving) editing = false }

    if (editing) {
        EditPage(
            profile = profile,
            streamSubjects = streamSubjects,
            saving = saving,
            onLoadSubjects = onLoadSubjects,
            onCancel = { editing = false },
            onSave = { updated -> onSave(updated) { editing = false } },
            modifier = modifier
        )
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            ProfileHeader(
                profile = profile,
                photoUploading = photoUploading,
                onChangePhoto = { showPhotoDialog = true }
            )

            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TeachingCard(profile = profile, onEdit = { editing = true })
                PersonalCard(profile = profile)

                // Student dashboard (switch, or use as a student too)
                RoleSwitchCard(currentRole = "teacher", onRoleChanged = onRoleChanged)

                Spacer(modifier = Modifier.size(4.dp))
                OutlinedButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sign out")
                }
            }
        }
    }
}

/** Gradient header, same look as the student profile and the Home header. */
@Composable
private fun ProfileHeader(
    profile: TeacherProfile,
    photoUploading: Boolean,
    onChangePhoto: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.linearGradient(listOf(NenaPrimary, NenaSecondary)),
                shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Photo (or the first letter) with a small camera button to change it
            Box(modifier = Modifier.size(96.dp)) {
                ProfileAvatar(
                    photoUrl = profile.photoUrl,
                    name = profile.name.ifBlank { "T" },
                    size = 90.dp,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .clickable(onClick = onChangePhoto)
                )
                if (photoUploading) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(90.dp)
                            .background(Color.Black.copy(alpha = 0.45f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            color = Color.White,
                            strokeWidth = 3.dp
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(32.dp)
                        .background(Color.White, CircleShape)
                        .clickable(onClick = onChangePhoto),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.PhotoCamera,
                        contentDescription = "Change profile photo",
                        tint = NenaPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.size(12.dp))
            Text(
                text = profile.name.ifBlank { "Teacher" },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            if (profile.email.isNotBlank()) {
                Text(
                    text = profile.email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
            Spacer(modifier = Modifier.size(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HeaderPill("Teacher")
                HeaderPill(if (profile.isFree) "Free service" else "Paid • Rs. ${money(profile.fee)}")
                if (profile.streamName.isNotBlank()) HeaderPill(profile.streamName)
            }
        }
    }
}

@Composable
private fun HeaderPill(text: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = Color.White.copy(alpha = 0.2f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
    }
}

/** Stream, numbered subjects and the service (free / paid), with the edit button. */
@Composable
private fun TeachingCard(profile: TeacherProfile, onEdit: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Teaching details",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.size(10.dp))

            if (profile.subjectNames.isNotEmpty()) {
                if (profile.streamName.isNotBlank()) {
                    Text(
                        text = profile.streamName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = NenaPrimary
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                }
                profile.subjectNames.forEachIndexed { index, subject ->
                    Row(
                        modifier = Modifier.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(NenaPrimaryContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${index + 1}",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = NenaPrimary
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = subject, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            } else {
                Text(
                    text = "You haven't chosen the subjects you teach yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.size(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.size(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Your service",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (profile.isFree) {
                    ServicePill("Free", NenaSecondaryContainer, Color(0xFF004D40))
                } else {
                    ServicePill("Paid • Rs. ${money(profile.fee)}", NenaAccentContainer, Color(0xFF6B4A00))
                }
            }

            Spacer(modifier = Modifier.size(12.dp))
            OutlinedButton(onClick = onEdit, modifier = Modifier.fillMaxWidth()) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Edit profile")
            }
        }
    }
}

@Composable
private fun ServicePill(text: String, background: Color, textColor: Color) {
    Surface(shape = RoundedCornerShape(8.dp), color = background) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelLarge,
            color = textColor
        )
    }
}

/** About me and phone. */
@Composable
private fun PersonalCard(profile: TeacherProfile) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Personal details",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.size(12.dp))
            DetailRow("About me", profile.bio.ifBlank { "Add a short bio so students know you." })
            Spacer(modifier = Modifier.size(10.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.size(10.dp))
            DetailRow("Phone", profile.phone.ifBlank { "Not added" })
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = NenaPrimary,
        fontWeight = FontWeight.SemiBold
    )
    Spacer(modifier = Modifier.height(2.dp))
    Text(text = value, style = MaterialTheme.typography.bodyLarge)
}

/** The edit page: opens inside this tab, same look as the student's profile pages. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditPage(
    profile: TeacherProfile,
    streamSubjects: List<Subject>,
    saving: Boolean,
    onLoadSubjects: () -> Unit,
    onCancel: () -> Unit,
    onSave: (TeacherProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf(profile.name) }
    var bio by remember { mutableStateOf(profile.bio) }
    var phone by remember { mutableStateOf(profile.phone) }
    var isFree by remember { mutableStateOf(profile.isFree) }
    var feeText by remember { mutableStateOf(if (profile.fee > 0) profile.fee.toString() else "") }
    var selectedIds by remember { mutableStateOf(profile.subjectIds) }

    LaunchedEffect(Unit) { onLoadSubjects() }

    // Subjects to choose from: the stream's subjects, plus any current one not in that list.
    val options = remember(streamSubjects, profile.subjectIds) {
        (profile.subjects + streamSubjects).distinctBy { it.id }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SubPageHeader(
            title = "Edit profile",
            subtitle = "Update how students see you",
            onBack = { if (!saving) onCancel() }
        )

        FormCard {
            Text("Personal details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                singleLine = true,
                enabled = !saving,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it.filter { c -> c.isDigit() || c == '+' } },
                label = { Text("Phone (optional)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                enabled = !saving,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = bio,
                onValueChange = { bio = it },
                label = { Text("About me") },
                minLines = 3,
                enabled = !saving,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }

        FormCard {
            Text("Subjects you teach", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                options.forEach { subject ->
                    FilterChip(
                        selected = subject.id in selectedIds,
                        onClick = {
                            selectedIds = if (subject.id in selectedIds) {
                                selectedIds - subject.id
                            } else {
                                selectedIds + subject.id
                            }
                        },
                        label = { Text(subject.name) },
                        enabled = !saving
                    )
                }
            }
        }

        FormCard {
            Text("Your service", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = isFree,
                    onClick = { isFree = true },
                    label = { Text("Free") },
                    enabled = !saving
                )
                FilterChip(
                    selected = !isFree,
                    onClick = { isFree = false },
                    label = { Text("Paid") },
                    enabled = !saving
                )
            }
            if (!isFree) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = feeText,
                    onValueChange = { feeText = it.filter { c -> c.isDigit() } },
                    label = { Text("Fee (Rs.)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    enabled = !saving,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        PrimaryActionButton(
            text = "Save changes",
            busy = saving,
            enabled = true,
            onClick = {
                val chosen = options.filter { it.id in selectedIds }
                onSave(
                    profile.copy(
                        name = name,
                        bio = bio,
                        phone = phone,
                        isFree = isFree,
                        fee = if (isFree) 0 else (feeText.toIntOrNull() ?: 0),
                        subjectIds = chosen.map { it.id },
                        subjectNames = chosen.map { it.name }
                    )
                )
            }
        )
        OutlinedButton(onClick = onCancel, enabled = !saving, modifier = Modifier.fillMaxWidth()) {
            Text("Cancel")
        }
    }
}

private fun money(value: Int): String = String.format(Locale.US, "%,d", value)