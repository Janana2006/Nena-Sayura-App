package lk.nenasayura.app.ui.student



import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import lk.nenasayura.app.ui.theme.NenaAccent
import lk.nenasayura.app.ui.theme.NenaPrimary
import lk.nenasayura.app.ui.theme.NenaPrimaryContainer
import lk.nenasayura.app.ui.theme.IconPalette
import lk.nenasayura.app.ui.theme.NenaIconColors


/** Resources tab: the free study material (same screens guests use). */
@Composable
fun StudentResourcesTab(actions: StudentHomeActions, modifier: Modifier = Modifier) {
    MenuTab(modifier = modifier) {
        MenuItemCard("Papers", "Past, model, provincial and target papers", Icons.Filled.Description, actions.onOpenPapers, palette = NenaIconColors.Papers)
        MenuItemCard("Marking Schemes", "Check your answers step by step", Icons.Filled.FactCheck, actions.onOpenMarkingSchemes, palette = NenaIconColors.MarkingSchemes)
        MenuItemCard("Quizzes", "Practice MCQs with explanations", Icons.Filled.Quiz, actions.onOpenQuizzes, palette = NenaIconColors.Quizzes)
        MenuItemCard("Guide Books", "Teacher guides and study books", Icons.AutoMirrored.Filled.MenuBook, actions.onOpenGuideBooks, palette = NenaIconColors.GuideBooks)
        MenuItemCard("Search", "Find any subject or resource", Icons.Filled.Search, actions.onOpenSearch, palette = NenaIconColors.Progress)
        MenuItemCard("Downloads", "Saved in the app, open without internet", Icons.Filled.DownloadDone, actions.onOpenDownloads, palette = NenaIconColors.Papers)
    }
}

/** Study tab: the student-only study tools. */
@Composable
fun StudentStudyTab(actions: StudentHomeActions, modifier: Modifier = Modifier) {
    MenuTab(modifier = modifier) {
        MenuItemCard("Teachers", "Notes, tutes, papers and videos from teachers", Icons.Filled.School, actions.onOpenTeachers, palette = NenaIconColors.Teacher)
        MenuItemCard("Timetable", "Your weekly classes and study times", Icons.Filled.CalendarMonth, actions.onOpenTimetable, palette = NenaIconColors.Timetable)
        MenuItemCard("Reminders", "Study, exam and revision reminders", Icons.Filled.Notifications, actions.onOpenReminders, palette = NenaIconColors.Reminders)
        MenuItemCard("Study Progress", "See how you are doing in each subject", Icons.Filled.BarChart, actions.onOpenAnalytics, palette = NenaIconColors.Progress)
    }
}

/**
 * Guest version of the Resources tab: same cards as the student, all open
 * (papers, marking schemes, quizzes, guide books and search are free for guests).
 */
@Composable
fun GuestResourcesTab(
    onOpenPapers: () -> Unit,
    onOpenMarkingSchemes: () -> Unit,
    onOpenQuizzes: () -> Unit,
    onOpenGuideBooks: () -> Unit,
    onOpenSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    MenuTab(modifier = modifier) {
        MenuItemCard("Papers", "Past, model, provincial and target papers", Icons.Filled.Description, onOpenPapers, palette = NenaIconColors.Papers)
        MenuItemCard("Marking Schemes", "Check your answers step by step", Icons.Filled.FactCheck, onOpenMarkingSchemes, palette = NenaIconColors.MarkingSchemes)
        MenuItemCard("Quizzes", "Practice MCQs with explanations", Icons.Filled.Quiz, onOpenQuizzes, palette = NenaIconColors.Quizzes)
        MenuItemCard("Guide Books", "Teacher guides and study books", Icons.AutoMirrored.Filled.MenuBook, onOpenGuideBooks, palette = NenaIconColors.GuideBooks)
        MenuItemCard("Search", "Find any subject or resource", Icons.Filled.Search, onOpenSearch, palette = NenaIconColors.Progress)
    }
}

/**
 * Guest version of the Study tab: same cards as the student, but locked.
 * Tapping any card sends the guest to the login page ([onLogin]).
 */
@Composable
fun GuestStudyTab(onLogin: () -> Unit, modifier: Modifier = Modifier) {
    MenuTab(modifier = modifier) {
        MenuItemCard("Teachers", "Choose a teacher and see their notes and videos", Icons.Filled.School, onLogin, palette = NenaIconColors.Teacher, locked = true)
        MenuItemCard("Timetable", "Your weekly classes and study times", Icons.Filled.CalendarMonth, onLogin, palette = NenaIconColors.Timetable, locked = true)
        MenuItemCard("Reminders", "Study, exam and revision reminders", Icons.Filled.Notifications, onLogin, palette = NenaIconColors.Reminders, locked = true)
        MenuItemCard("Study Analytics", "Track your quiz results and progress", Icons.Filled.BarChart, onLogin, palette = NenaIconColors.Progress, locked = true)
    }
}

@Composable
private fun MenuTab(
    modifier: Modifier,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        content()
    }
}

/** A full-width card with an icon, title, subtitle and an arrow. */
@Composable
fun MenuItemCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    palette: IconPalette = IconPalette(NenaPrimary, NenaPrimaryContainer),
    modifier: Modifier = Modifier,
    locked: Boolean = false
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
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
                    .background(palette.container, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = palette.tint)
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (locked) {
                // Guests: lock icon instead of the arrow (card still opens the login page)
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = "Login required",
                    tint = NenaAccent,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}