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
import lk.nenasayura.app.data.model.AdminPermission
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
            // ---- Counts (only what this admin has access to) ----
            val freeTeachers = state.teachers.count { it.isFree }
            val stats = buildList {
                if (state.can(AdminPermission.USERS)) {
                    add("Students" to state.students.size.toString())
                    add("Teachers" to state.teachers.size.toString())
                }
                if (state.can(AdminPermission.STAFF)) add("Staff" to state.staff.size.toString())
                if (state.can(AdminPermission.USERS)) {
                    add("Free teachers" to freeTeachers.toString())
                    add("Paid teachers" to (state.teachers.size - freeTeachers).toString())
                }
                if (state.can(AdminPermission.STREAMS)) add("Streams" to state.streams.size.toString())
                if (state.can(AdminPermission.PAPERS)) add("Papers" to state.papers.size.toString())
                if (state.can(AdminPermission.GUIDE_BOOKS)) add("Guide books" to state.guideBooks.size.toString())
                if (state.can(AdminPermission.PAYMENTS)) add("Payments" to state.paidPayments.size.toString())
            }
            stats.chunked(3).forEachIndexed { index, rowItems ->
                if (index > 0) Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    rowItems.forEach { (label, value) -> StatCard(label, value, Modifier.weight(1f)) }
                    repeat(3 - rowItems.size) { Spacer(modifier = Modifier.weight(1f)) }
                }
            }

            // ---- Earnings ----
            if (state.can(AdminPermission.PAYMENTS)) {
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
            }

            // ---- Shortcuts ----
            val tiles = buildList<Triple<String, ImageVector, () -> Unit>> {
                if (state.can(AdminPermission.USERS)) add(Triple("Users", Icons.Filled.People, onOpenUsers))
                if (state.can(AdminPermission.PAYMENTS)) add(Triple("Payments", Icons.Filled.Payments, onOpenPayments))
                if (state.canSeeContent) add(Triple("Content", Icons.Filled.Folder, onOpenContent))
                add(Triple("Profile", Icons.Filled.AdminPanelSettings, onOpenStaff)) // always shown
            }
            Spacer(modifier = Modifier.height(24.dp))
            SectionTitle("Quick access")
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                tiles.forEach { (label, icon, onClick) -> ShortcutTile(label, icon, onClick, Modifier.weight(1f)) }
                repeat(4 - tiles.size) { Spacer(modifier = Modifier.weight(1f)) }
            }

            // ---- Latest payments ----
            if (state.can(AdminPermission.PAYMENTS)) {
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