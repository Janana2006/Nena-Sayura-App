package lk.nenasayura.app.ui.teacher.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import lk.nenasayura.app.data.model.AccessType
import lk.nenasayura.app.data.model.Payment
import lk.nenasayura.app.data.model.TeacherContent
import lk.nenasayura.app.data.model.TeacherProfile
import lk.nenasayura.app.ui.theme.NenaPrimary
import lk.nenasayura.app.ui.theme.NenaPrimaryContainer
import lk.nenasayura.app.ui.theme.NenaSecondary
import lk.nenasayura.app.ui.theme.IconPalette
import lk.nenasayura.app.ui.theme.NenaIconColors

/** Home tab of the Teacher: welcome header, upload counts, shortcuts and earnings. */
@Composable
fun TeacherDashboardScreen(
    profile: TeacherProfile,
    contents: List<TeacherContent>,
    onOpenUpload: () -> Unit,
    onOpenContent: () -> Unit,
    onOpenProfile: () -> Unit,
    modifier: Modifier = Modifier,
    earningsViewModel: TeacherDashboardViewModel = viewModel()
) {
    val earnings by earningsViewModel.state.collectAsStateWithLifecycle()

    // Free teachers have no payments, so only paid teachers load earnings.
    LaunchedEffect(profile.uid, profile.isFree) {
        if (!profile.isFree) earningsViewModel.load(profile.uid)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        TeacherHeader(profile = profile)

        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp)) {
            // ---- Upload counts ----
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("Total uploads", contents.size.toString(), Modifier.weight(1f))
                StatCard(
                    "Paid items",
                    contents.count { it.access == AccessType.PAID }.toString(),
                    Modifier.weight(1f)
                )
                StatCard(
                    "Free items",
                    contents.count { it.access == AccessType.FREE }.toString(),
                    Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Quick access",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ShortcutTile("Upload", Icons.Filled.CloudUpload, NenaIconColors.Quizzes, onOpenUpload, Modifier.weight(1f))
                ShortcutTile("My Content", Icons.Filled.Folder, NenaIconColors.GuideBooks, onOpenContent, Modifier.weight(1f))
                ShortcutTile("Profile", Icons.Filled.Person, NenaIconColors.MarkingSchemes, onOpenProfile, Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ---- Students & earnings ----
            if (profile.isFree) {
                InfoCard(
                    title = "Your service is free",
                    message = "Every upload is free for students. To charge a fee, change your plan in the Profile tab."
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Students & earnings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = { earningsViewModel.load(profile.uid) },
                        enabled = !earnings.loading
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                when {
                    earnings.loading -> Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator() }

                    earnings.errorMessage != null -> Text(
                        text = earnings.errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    else -> EarningsContent(earnings)
                }
            }
        }
    }
}

@Composable
private fun TeacherHeader(profile: TeacherProfile) {
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
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                text = "Welcome back,",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f)
            )
            Text(
                text = profile.name.ifBlank { "Teacher" },
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            val subjects = profile.subjectNames.joinToString(", ")
            Text(
                text = if (subjects.isNotBlank()) "$subjects teacher" else "Share your knowledge with students",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f)
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun EarningsContent(state: EarningsUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${state.paidStudents}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = NenaPrimary
                )
                Text(
                    text = "Students who paid and have access",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (state.pendingCount > 0) {
                Text(
                    text = "${state.pendingCount} pending",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            MoneyRow("Total paid by students", money(state.gross))
            Spacer(modifier = Modifier.height(8.dp))
            MoneyRow(
                label = "Admin commission (${percentText(state.currentPercent)}%)",
                value = "- ${money(state.adminCommission)}",
                valueColor = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(10.dp))
            MoneyRow(
                label = "You receive",
                value = money(state.teacherNet),
                valueColor = NenaSecondary,
                bold = true
            )
        }
    }

    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = "Paid students",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold
    )
    Spacer(modifier = Modifier.height(8.dp))

    if (state.paidPayments.isEmpty()) {
        Text(
            text = "No paid students yet.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    } else {
        for (payment in state.paidPayments) {
            PaidStudentRow(payment)
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun PaidStudentRow(payment: Payment) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = payment.studentName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                val sub = listOf(payment.subjectName, formatDate(payment.createdAtMillis))
                    .filter { it.isNotBlank() }
                    .joinToString(" • ")
                if (sub.isNotBlank()) {
                    Text(
                        text = sub,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = money(payment.amount),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = NenaPrimary
            )
        }
    }
}

@Composable
private fun MoneyRow(
    label: String,
    value: String,
    valueColor: Color = Color.Unspecified,
    bold: Boolean = false
) {
    val style = if (bold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = style,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal
        )
        Text(text = value, style = style, fontWeight = FontWeight.Bold, color = valueColor)
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = NenaPrimary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ShortcutTile(
    label: String,
    icon: ImageVector,
    palette: IconPalette,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(palette.container, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = palette.tint)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun InfoCard(title: String, message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun money(value: Double): String = "Rs. " + String.format(Locale.US, "%,.2f", value)

private fun percentText(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else String.format(Locale.US, "%.1f", value)

private fun formatDate(millis: Long): String =
    if (millis <= 0L) "" else SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(millis))