package lk.nenasayura.app.ui.student

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.runtime.Composable
import lk.nenasayura.app.ui.common.CategoryItem
import lk.nenasayura.app.ui.common.CategoryScreen
import lk.nenasayura.app.ui.theme.NenaIconColors

/** The full Quick access list opened from "See all" on Student Home. */
@Composable
fun QuickAccessScreen(
    onBack: () -> Unit,
    onOpenPapers: () -> Unit,
    onOpenQuizzes: () -> Unit,
    onOpenTimetable: () -> Unit,
    onOpenReminders: () -> Unit,
    onOpenMarkingSchemes: () -> Unit,
    onOpenGuideBooks: () -> Unit
) {
    CategoryScreen(
        title = "Quick access",
        heading = "Jump straight in",
        categories = listOf(
            CategoryItem(
                title = "Papers",
                subtitle = "Past, model and provincial papers",
                icon = Icons.Filled.Description,
                palette = NenaIconColors.Papers,
                onClick = onOpenPapers
            ),
            CategoryItem(
                title = "Quizzes",
                subtitle = "Practice MCQs with explanations",
                icon = Icons.Filled.Quiz,
                palette = NenaIconColors.Quizzes,
                onClick = onOpenQuizzes
            ),
            CategoryItem(
                title = "Timetable",
                subtitle = "Your weekly classes and study times",
                icon = Icons.Filled.CalendarMonth,
                palette = NenaIconColors.Timetable,
                onClick = onOpenTimetable
            ),
            CategoryItem(
                title = "Reminders",
                subtitle = "Study, exam and revision reminders",
                icon = Icons.Filled.Notifications,
                palette = NenaIconColors.Reminders,
                onClick = onOpenReminders
            ),
            CategoryItem(
                title = "Marking Schemes",
                subtitle = "Check your answers step by step",
                icon = Icons.Filled.FactCheck,
                palette = NenaIconColors.MarkingSchemes,
                onClick = onOpenMarkingSchemes
            ),
            CategoryItem(
                title = "Guide Books",
                subtitle = "Guide books and resource books",
                icon = Icons.AutoMirrored.Filled.MenuBook,
                palette = NenaIconColors.GuideBooks,
                onClick = onOpenGuideBooks
            )
        ),
        onBack = onBack
    )
}