package lk.nenasayura.app.ui.admin

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import lk.nenasayura.app.data.model.AppUser
import lk.nenasayura.app.data.model.Payment
import lk.nenasayura.app.ui.common.EmptyView
import lk.nenasayura.app.ui.theme.NenaAccentContainer
import lk.nenasayura.app.ui.theme.NenaPrimary
import lk.nenasayura.app.ui.theme.NenaSecondary
import lk.nenasayura.app.ui.theme.NenaSecondaryContainer

private enum class UserFilter(val label: String) {
    Students("Students"),
    Teachers("Teachers"),
    Free("Free teachers"),
    Paid("Paid teachers")
}

/** Users tab: every student and teacher, with Free / Paid for teachers. */
@Composable
fun AdminUsersTab(
    users: List<AppUser>,
    payments: List<Payment>,
    modifier: Modifier = Modifier
) {
    var filter by remember { mutableStateOf(UserFilter.Students) }
    var query by remember { mutableStateOf("") }

    fun listFor(f: UserFilter): List<AppUser> = when (f) {
        UserFilter.Students -> users.filter { it.isStudent }
        UserFilter.Teachers -> users.filter { it.isTeacher }
        UserFilter.Free -> users.filter { it.isTeacher && it.isFree }
        UserFilter.Paid -> users.filter { it.isTeacher && !it.isFree }
    }

    val shown = listFor(filter).filter {
        query.isBlank() ||
                it.name.contains(query, ignoreCase = true) ||
                it.email.contains(query, ignoreCase = true)
    }

    Column(modifier = modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(UserFilter.entries.toList()) { f ->
                FilterChip(
                    selected = filter == f,
                    onClick = { filter = f },
                    label = { Text("${f.label} (${listFor(f).size})") }
                )
            }
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Search by name or email") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        if (shown.isEmpty()) {
            EmptyView(message = "No users found.")
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(shown, key = { it.uid }) { user ->
                    UserCard(user = user, payments = payments)
                }
            }
        }
    }
}

@Composable
private fun UserCard(user: AppUser, payments: List<Payment>) {
    AdminCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        brush = Brush.linearGradient(listOf(NenaPrimary, NenaSecondary)),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = user.name.firstOrNull()?.uppercase() ?: "?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.name.ifBlank { "No name" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = user.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (user.isTeacher) {
            Spacer(modifier = Modifier.height(10.dp))
            if (user.isFree) {
                Pill("Free teacher", NenaSecondaryContainer, Color(0xFF004D40))
            } else {
                Pill("Paid teacher • ${money(user.fee.toDouble())}", NenaAccentContainer, Color(0xFF6B4A00))
            }
        }

        val details = listOf(
            user.streamName,
            user.subjectNames.joinToString(", ")
        ).filter { it.isNotBlank() }.joinToString(" • ")
        if (details.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = details,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Paid teachers: how many students paid them and how much
        if (user.isTeacher && !user.isFree) {
            val paid = payments.filter { it.teacherId == user.uid && it.isPaid }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Paid students: ${paid.map { it.studentId }.distinct().size}" +
                        " • Total received: ${money(paid.sumOf { it.amount })}",
                style = MaterialTheme.typography.labelLarge,
                color = NenaPrimary
            )
        }
    }
}