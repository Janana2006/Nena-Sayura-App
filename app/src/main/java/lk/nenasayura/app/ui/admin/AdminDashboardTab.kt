package lk.nenasayura.app.ui.admin

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
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import lk.nenasayura.app.ui.theme.NenaPrimary
import lk.nenasayura.app.ui.theme.NenaPrimaryContainer
import lk.nenasayura.app.ui.theme.NenaSecondary

/** Home tab of the Admin: counts, earnings, shortcuts and the latest payments. */
@Composable
fun AdminDashboardTab(
    state: AdminUiState,
    onOpenUsers: () -> Unit,
    onOpenPayments: () -> Unit,
    onOpenContent: () -> Unit,
    onOpenStaff: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // ---- Header ----
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(listOf(NenaPrimary, NenaSecondary)),
                    shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)
                )
        ) {
            Row(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(start = 20.dp, end = 8.dp, top = 16.dp, bottom = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Welcome back,",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                    Text(
                        text = "Admin Dashboard",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Manage students, teachers, payments and content",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = Color.White)
                }
            }
        }

        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp)) {
            // ---- Counts ----
            val freeTeachers = state.teachers.count { it.isFree }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("Students", state.students.size.toString(), Modifier.weight(1f))
                StatCard("Teachers", state.teachers.size.toString(), Modifier.weight(1f))
                StatCard("Staff", state.staff.size.toString(), Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("Free teachers", freeTeachers.toString(), Modifier.weight(1f))
                StatCard("Paid teachers", (state.teachers.size - freeTeachers).toString(), Modifier.weight(1f))
                StatCard("Streams", state.streams.size.toString(), Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("Papers", state.papers.size.toString(), Modifier.weight(1f))
                StatCard("Guide books", state.guideBooks.size.toString(), Modifier.weight(1f))
                StatCard("Payments", state.paidPayments.size.toString(), Modifier.weight(1f))
            }

            // ---- Earnings ----
            Spacer(modifier = Modifier.height(24.dp))
            SectionTitle("Earnings")
            Spacer(modifier = Modifier.height(12.dp))
            AdminCard {
                MoneyRow("Paid by students", money(state.gross))
                Spacer(modifier = Modifier.height(8.dp))
                MoneyRow("Your commission (${percentText(state.commissionPercent)}%)", money(state.commission), NenaSecondary)
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(10.dp))
                MoneyRow("Paid out to teachers", money(state.teachersTotal), bold = true)
            }

            // ---- Shortcuts ----
            Spacer(modifier = Modifier.height(24.dp))
            SectionTitle("Quick access")
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ShortcutTile("Users", Icons.Filled.People, onOpenUsers, Modifier.weight(1f))
                ShortcutTile("Payments", Icons.Filled.Payments, onOpenPayments, Modifier.weight(1f))
                ShortcutTile("Content", Icons.Filled.Folder, onOpenContent, Modifier.weight(1f))
                ShortcutTile("Staff", Icons.Filled.AdminPanelSettings, onOpenStaff, Modifier.weight(1f))
            }

            // ---- Latest payments ----
            Spacer(modifier = Modifier.height(24.dp))
            SectionTitle("Latest payments")
            Spacer(modifier = Modifier.height(12.dp))
            if (state.payments.isEmpty()) {
                Text(
                    text = "No payments yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                state.payments.take(5).forEach { payment ->
                    PaymentRow(payment)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun ShortcutTile(
    label: String,
    icon: ImageVector,
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
                    .background(NenaPrimaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = NenaPrimary)
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
fun MoneyRow(
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

fun percentText(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else String.format(java.util.Locale.US, "%.1f", value)