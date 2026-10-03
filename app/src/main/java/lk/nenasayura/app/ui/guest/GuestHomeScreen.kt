package lk.nenasayura.app.ui.guest



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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import lk.nenasayura.app.ui.common.LoginRequiredDialog
import lk.nenasayura.app.ui.theme.NenaAccentContainer
import lk.nenasayura.app.ui.theme.NenaPrimary
import lk.nenasayura.app.ui.theme.NenaPrimaryContainer
import lk.nenasayura.app.ui.theme.NenaSecondary

private val LockedIconColor = Color(0xFF8A5A00)

private data class ResourceItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

private data class LockedItem(
    val title: String,
    val description: String,
    val icon: ImageVector
)

/**
 * Home page for users who are not logged in.
 * Free resources open directly; student-only features ask the guest to log in.
 */
@Composable
fun GuestHomeScreen(
    onOpenPapers: () -> Unit,
    onOpenMarkingSchemes: () -> Unit,
    onOpenQuizzes: () -> Unit,
    onOpenGuideBooks: () -> Unit,
    onOpenSearch: () -> Unit,
    onLogin: () -> Unit
) {
    // Name of the locked feature the guest tapped (null = dialog hidden)
    var lockedFeature by remember { mutableStateOf<String?>(null) }

    val resources = listOf(
        ResourceItem("Papers", "Past & model papers", Icons.Filled.Description, onOpenPapers),
        ResourceItem("Marking Schemes", "Check your answers", Icons.Filled.FactCheck, onOpenMarkingSchemes),
        ResourceItem("Quizzes", "Practice with MCQs", Icons.Filled.Quiz, onOpenQuizzes),
        ResourceItem("Guide Books", "Teacher guides & books", Icons.AutoMirrored.Filled.MenuBook, onOpenGuideBooks)
    )

    val lockedItems = listOf(
        LockedItem("Choose a teacher", "Notes, model papers and study videos", Icons.Filled.School),
        LockedItem("Timetable", "Plan your weekly classes", Icons.Filled.CalendarMonth),
        LockedItem("Reminders", "Never miss a study session or exam", Icons.Filled.Notifications),
        LockedItem("Study analytics", "Track your quiz progress", Icons.Filled.BarChart)
    )

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
                .verticalScroll(rememberScrollState())
        ) {
            GuestHeader(onLogin = onLogin, onOpenSearch = onOpenSearch)

            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp)) {
                SectionTitle(title = "Free study resources", subtitle = "")
                Spacer(modifier = Modifier.height(12.dp))

                resources.chunked(2).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowItems.forEach { item ->
                            ResourceCard(item = item, modifier = Modifier.weight(1f))
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Spacer(modifier = Modifier.height(12.dp))
                SectionTitle(title = "More with a student account", subtitle = "Log in to unlock")
                Spacer(modifier = Modifier.height(12.dp))

                lockedItems.forEach { item ->
                    LockedCard(item = item, onClick = { lockedFeature = item.title })
                    Spacer(modifier = Modifier.height(10.dp))
                }

                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onLogin,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Log in or register")
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    lockedFeature?.let { feature ->
        LoginRequiredDialog(
            featureName = feature,
            onDismiss = { lockedFeature = null },
            onLogin = {
                lockedFeature = null
                onLogin()
            }
        )
    }
}

@Composable
private fun GuestHeader(onLogin: () -> Unit, onOpenSearch: () -> Unit) {
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Nena Sayura",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                }
                FilledTonalButton(onClick = onLogin) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Login,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Log in")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Prepare smarter for your A/L exams",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Papers, marking schemes, quizzes and guide books for every stream.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f)
            )

            Spacer(modifier = Modifier.height(20.dp))
            // Looks like a search bar; tapping opens the Search screen (Task 6)
            Surface(
                onClick = onOpenSearch,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color.White
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                        tint = Color(0xFF6B7280)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Search subjects, papers, quizzes…",
                        color = Color(0xFF6B7280)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ResourceCard(item: ResourceItem, modifier: Modifier = Modifier) {
    Card(
        onClick = item.onClick,
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(NenaPrimaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = item.icon, contentDescription = null, tint = NenaPrimary)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = item.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LockedCard(item: LockedItem, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(NenaAccentContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = item.icon, contentDescription = null, tint = LockedIconColor)
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = "Login required",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}