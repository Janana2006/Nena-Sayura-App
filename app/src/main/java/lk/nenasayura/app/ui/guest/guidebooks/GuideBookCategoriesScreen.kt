package lk.nenasayura.app.ui.guest.guidebooks

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.runtime.Composable
import lk.nenasayura.app.core.browse.GuideBrowseState
import lk.nenasayura.app.ui.common.CategoryItem
import lk.nenasayura.app.ui.common.CategoryScreen
import lk.nenasayura.app.ui.common.StreamSummaryCard
import lk.nenasayura.app.ui.theme.NenaIconColors

/** Guide Books landing page. [onOpenCategory] gets "guide" or "resource". */
@Composable
fun GuideBookCategoriesScreen(
    onBack: () -> Unit,
    onOpenCategory: (category: String) -> Unit,
    // Students: the stream they picked (shown on top with a "Change" button).
    selection: GuideBrowseState? = null,
    onChangeSelection: (() -> Unit)? = null
) {
    CategoryScreen(
        title = "Guide Books",
        heading = "Choose a category",
        categories = listOf(
            CategoryItem(
                title = "Guide Books",
                subtitle = "Teacher guides and study guides",
                icon = Icons.AutoMirrored.Filled.MenuBook,
                palette = NenaIconColors.GuideBooks,
                onClick = { onOpenCategory("guide") }
            ),
            CategoryItem(
                title = "Resource Books",
                subtitle = "Extra reading and reference books",
                icon = Icons.AutoMirrored.Filled.LibraryBooks,
                palette = NenaIconColors.ResourceBooks,
                onClick = { onOpenCategory("resource") }
            )
        ),
        onBack = onBack,
        header = if (selection != null && selection.hasSelection && onChangeSelection != null) {
            {
                StreamSummaryCard(
                    streamName = selection.streamName,
                    details = "${selection.subjects.size} subjects",
                    onChange = onChangeSelection
                )
            }
        } else null
    )
}