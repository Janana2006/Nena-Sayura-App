package lk.nenasayura.app.ui.teacher.profile

import androidx.compose.foundation.background
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.util.Locale
import lk.nenasayura.app.data.model.Subject
import lk.nenasayura.app.data.model.TeacherProfile
import lk.nenasayura.app.ui.common.RoleSwitchCard
import lk.nenasayura.app.ui.theme.NenaAccentContainer
import lk.nenasayura.app.ui.theme.NenaPrimary
import lk.nenasayura.app.ui.theme.NenaPrimaryContainer
import lk.nenasayura.app.ui.theme.NenaSecondary
import lk.nenasayura.app.ui.theme.NenaSecondaryContainer

/** Profile tab: shows the teacher's profile and lets them edit it. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TeacherProfileTab(
    profile: TeacherProfile,
    streamSubjects: List<Subject>,
    saving: Boolean,
    onLoadSubjects: () -> Unit,
    onSave: (TeacherProfile, () -> Unit) -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
    onRoleChanged: (String) -> Unit = {}
) {
    var editing by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Avatar(name = profile.name)

        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = profile.name.ifBlank { "Teacher" },
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        if (profile.email.isNotBlank()) {
            Text(
                text = profile.email,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Pill("Teacher", NenaPrimaryContainer, NenaPrimary)
            if (profile.isFree) {
                Pill("Free service", NenaSecondaryContainer, Color(0xFF004D40))
            } else {
                Pill("Paid • Rs. ${money(profile.fee)}", NenaAccentContainer, Color(0xFF6B4A00))
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (editing) {
            EditForm(
                profile = profile,
                streamSubjects = streamSubjects,
                saving = saving,
                onLoadSubjects = onLoadSubjects,
                onCancel = { editing = false },
                onSave = { updated -> onSave(updated) { editing = false } }
            )
        } else {
            InfoCard("Stream", profile.streamName.ifBlank { "Not set" })
            Spacer(modifier = Modifier.height(10.dp))
            InfoCard("Subjects", profile.subjectNames.joinToString(", ").ifBlank { "Not set" })
            Spacer(modifier = Modifier.height(10.dp))
            InfoCard("About me", profile.bio.ifBlank { "Add a short bio so students know you." })
            Spacer(modifier = Modifier.height(10.dp))
            InfoCard("Phone", profile.phone.ifBlank { "Not added" })

            Spacer(modifier = Modifier.height(20.dp))
            Button(onClick = { editing = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Edit profile")
            }
            Spacer(modifier = Modifier.height(16.dp))
            // Student dashboard (switch, or use as a student too)
            RoleSwitchCard(currentRole = "teacher", onRoleChanged = onRoleChanged)
            Spacer(modifier = Modifier.height(10.dp))
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
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditForm(
    profile: TeacherProfile,
    streamSubjects: List<Subject>,
    saving: Boolean,
    onLoadSubjects: () -> Unit,
    onCancel: () -> Unit,
    onSave: (TeacherProfile) -> Unit
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

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            singleLine = true,
            enabled = !saving,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it.filter { c -> c.isDigit() || c == '+' } },
            label = { Text("Phone (optional)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            enabled = !saving,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = bio,
            onValueChange = { bio = it },
            label = { Text("About me") },
            minLines = 3,
            enabled = !saving,
            modifier = Modifier.fillMaxWidth()
        )

        Text("Subjects you teach", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
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

        Text("Your service", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
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
            OutlinedTextField(
                value = feeText,
                onValueChange = { feeText = it.filter { c -> c.isDigit() } },
                label = { Text("Fee (Rs.)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                enabled = !saving,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(4.dp))
        Button(
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
            },
            enabled = !saving,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            if (saving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = Color.White
                )
            } else {
                Text("Save changes")
            }
        }
        OutlinedButton(onClick = onCancel, enabled = !saving, modifier = Modifier.fillMaxWidth()) {
            Text("Cancel")
        }
    }
}

@Composable
private fun Avatar(name: String) {
    Box(
        modifier = Modifier
            .size(88.dp)
            .background(
                brush = Brush.linearGradient(listOf(NenaPrimary, NenaSecondary)),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name.firstOrNull()?.uppercase() ?: "T",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
private fun Pill(text: String, background: Color, textColor: Color) {
    Surface(shape = RoundedCornerShape(8.dp), color = background) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelLarge,
            color = textColor
        )
    }
}

@Composable
private fun InfoCard(label: String, value: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = NenaPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = value, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
private fun money(value: Int): String = String.format(Locale.US, "%,d", value)