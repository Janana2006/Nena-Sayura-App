package lk.nenasayura.app.ui.guest.markingschemes



import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.runtime.Composable
import lk.nenasayura.app.ui.common.CategoryItem
import lk.nenasayura.app.ui.common.CategoryScreen
import lk.nenasayura.app.ui.theme.NenaIconColors

/** Marking Schemes landing page. [onOpenCategory] gets "past", "model" or "provincial". */
@Composable
fun MarkingSchemeCategoriesScreen(
    onBack: () -> Unit,
    onOpenCategory: (category: String) -> Unit
) {
    CategoryScreen(
        title = "Marking Schemes",
        heading = "Choose a category",
        categories = listOf(
            CategoryItem(
                title = "Past Papers",
                subtitle = "Marking schemes for past papers",
                icon = Icons.Filled.History,
                palette = NenaIconColors.MarkingSchemes,
                onClick = { onOpenCategory("past") }
            ),
            CategoryItem(
                title = "Model Papers",
                subtitle = "Marking schemes for model papers",
                icon = Icons.AutoMirrored.Filled.Assignment,
                palette = NenaIconColors.Quizzes,
                onClick = { onOpenCategory("model") }
            ),
            CategoryItem(
                title = "Provincial Papers",
                subtitle = "Marking schemes for provincial papers",
                icon = Icons.Filled.LocationOn,
                palette = NenaIconColors.ResourceBooks,
                onClick = { onOpenCategory("provincial") }
            )
        ),
        onBack = onBack
    )
}