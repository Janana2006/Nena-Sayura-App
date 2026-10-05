package lk.nenasayura.app.ui.guest.papers



import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.runtime.Composable
import lk.nenasayura.app.ui.common.CategoryItem
import lk.nenasayura.app.ui.common.CategoryScreen
import lk.nenasayura.app.ui.theme.NenaIconColors

/** Papers landing page: Past, Model and Provincial papers. [onOpenCategory] gets "past", "model" or "provincial". */
@Composable
fun PaperCategoriesScreen(
    onBack: () -> Unit,
    onOpenCategory: (category: String) -> Unit
) {
    CategoryScreen(
        title = "Papers",
        heading = "Choose a paper category",
        categories = listOf(
            CategoryItem(
                title = "Past Papers",
                subtitle = "Previous A/L exam papers",
                icon = Icons.Filled.History,
                palette = NenaIconColors.Papers,
                onClick = { onOpenCategory("past") }
            ),
            CategoryItem(
                title = "Model Papers",
                subtitle = "Practice papers for revision",
                icon = Icons.AutoMirrored.Filled.Assignment,
                palette = NenaIconColors.Quizzes,
                onClick = { onOpenCategory("model") }
            ),
            CategoryItem(
                title = "Provincial Papers",
                subtitle = "Papers from provincial exams",
                icon = Icons.Filled.LocationOn,
                palette = NenaIconColors.ResourceBooks,
                onClick = { onOpenCategory("provincial") }
            )
        ),
        onBack = onBack
    )
}