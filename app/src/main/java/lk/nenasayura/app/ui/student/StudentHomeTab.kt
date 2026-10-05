package lk.nenasayura.app.ui.student



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
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.TextButton
import lk.nenasayura.app.ui.theme.IconPalette
import lk.nenasayura.app.ui.theme.NenaIconColors
import lk.nenasayura.app.ui.student.reviews.ReviewsSection

private data class Shortcut(
    val label: String,
    val icon: ImageVector,
    val palette: IconPalette,
    val onClick: () -> Unit
)


/** Home tab: welcome header, quick shortcuts and highlight cards. */
@Composable
fun StudentHomeTab(
    name: String,
    actions: StudentHomeActions,
    modifier: Modifier = Modifier
) {
    val shortcuts = listOf(
        Shortcut("Papers", Icons.Filled.Description, NenaIconColors.Papers, actions.onOpenPapers),
        Shortcut("Quizzes", Icons.Filled.Quiz, NenaIconColors.Quizzes, actions.onOpenQuizzes),
        Shortcut("Timetable", Icons.Filled.CalendarMonth, NenaIconColors.Timetable, actions.onOpenTimetable),
        Shortcut("Reminders", Icons.Filled.Notifications, NenaIconColors.Reminders, actions.onOpenReminders)
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        StudentHeader(name = name, onOpenSearch = actions.onOpenSearch)

        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp)) {
            // Heading with a small "See all" action on the same row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Quick access",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                TextButton(
                    onClick = actions.onOpenQuickAccess,
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Text(
                        text = "See all",
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                shortcuts.forEach { shortcut ->
                    ShortcutTile(shortcut = shortcut, modifier = Modifier.weight(1f))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            ReviewsSection(
                onViewAll = actions.onOpenReviews,
                onWriteReview = actions.onWriteReview
            )

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "For you",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            // These two cards show real data in Tasks 8 and 12
            MenuItemCard(
                title = "My teacher",
                subtitle = "Choose a teacher to get notes, model papers and videos",
                icon = Icons.Filled.School,
                onClick = actions.onOpenTeachers,
                palette = NenaIconColors.Teacher
            )
            Spacer(modifier = Modifier.height(10.dp))
            MenuItemCard(
                title = "Study progress",
                subtitle = "See how your quizzes and study time are going",
                icon = Icons.Filled.BarChart,
                onClick = actions.onOpenAnalytics,
                palette = NenaIconColors.Progress
            )
        }
    }
}

@Composable
private fun StudentHeader(name: String, onOpenSearch: () -> Unit) {
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
                text = name,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Ready to study for your A/L exams today?",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f)
            )

            Spacer(modifier = Modifier.height(20.dp))
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
private fun ShortcutTile(shortcut: Shortcut, modifier: Modifier = Modifier) {
    Card(
        onClick = shortcut.onClick,
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
                    .background(shortcut.palette.container, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = shortcut.icon,
                    contentDescription = null,
                    tint = shortcut.palette.tint
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = shortcut.label,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                textAlign = TextAlign.Center
            )
        }
    }
}