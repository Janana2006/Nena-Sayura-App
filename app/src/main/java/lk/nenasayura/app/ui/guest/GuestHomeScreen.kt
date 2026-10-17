package lk.nenasayura.app.ui.guest

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import lk.nenasayura.app.ui.common.NenaTopBar
import lk.nenasayura.app.ui.student.GuestResourcesTab
import lk.nenasayura.app.ui.student.MenuItemCard
import lk.nenasayura.app.ui.student.GuestStudyTab
import lk.nenasayura.app.ui.theme.IconPalette
import lk.nenasayura.app.ui.theme.NenaAccent
import lk.nenasayura.app.ui.theme.NenaIconColors
import lk.nenasayura.app.ui.theme.NenaPrimary
import lk.nenasayura.app.ui.theme.NenaPrimaryContainer
import lk.nenasayura.app.ui.theme.NenaSecondary

private data class ResourceItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val palette: IconPalette,
    val onClick: () -> Unit
)

/** The four bottom tabs. Home and Resources are open to guests. */
private enum class GuestTab(
    val label: String,
    val title: String,
    val icon: ImageVector,
    val locked: Boolean,
    val benefit: String
) {
    Home("Home", "Home", Icons.Filled.Home, false, ""),
    Resources(
        "Resources", "Resources", Icons.AutoMirrored.Filled.LibraryBooks, false,
        "all your study resources in one place"
    ),
    Study(
        "Study", "Study Tools", Icons.Filled.CalendarMonth, true,
        "your teachers, timetable, reminders and study progress"
    ),
    Profile(
        "Profile", "Profile", Icons.Filled.Person, true,
        "your profile and account"
    )
}

/**
 * Home page for users who are not logged in.
 * Home shows the free resources; Study and Profile ask the guest to log in.
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
    // Saved as a number so the selected tab survives opening another screen and coming back
    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }
    val selectedTab = GuestTab.entries[selectedIndex]

    // Back from a locked tab returns to the Home tab first
    BackHandler(enabled = selectedTab != GuestTab.Home) { selectedIndex = 0 }

    val resources = listOf(
        ResourceItem("Papers", "Past & model papers", Icons.Filled.Description, NenaIconColors.Papers, onOpenPapers),
        ResourceItem("Marking Schemes", "Check your answers", Icons.Filled.FactCheck, NenaIconColors.MarkingSchemes, onOpenMarkingSchemes),
        ResourceItem("Quizzes", "Practice with MCQs", Icons.Filled.Quiz, NenaIconColors.Quizzes, onOpenQuizzes),
        ResourceItem("Guide Books", "Teacher guides & books", Icons.AutoMirrored.Filled.MenuBook, NenaIconColors.GuideBooks, onOpenGuideBooks)
    )

    Scaffold(
        topBar = {
            // The Home tab draws its own gradient header
            if (selectedTab != GuestTab.Home) {
                NenaTopBar(title = selectedTab.title)
            }
        },
        bottomBar = {
            GuestBottomBar(
                selectedIndex = selectedIndex,
                onTabSelected = { selectedIndex = it }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        if (selectedTab == GuestTab.Home) {
            // Home goes edge to edge at the top, so only the bottom padding is applied
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

                    // Student-only cards, shown locked: tap -> login page
                    Spacer(modifier = Modifier.height(12.dp))
                    SectionTitle(title = "For you", subtitle = "")
                    Spacer(modifier = Modifier.height(12.dp))
                    MenuItemCard(
                        title = "My teacher",
                        subtitle = "Choose a teacher to get notes, model papers and videos",
                        icon = Icons.Filled.School,
                        onClick = onLogin,
                        palette = NenaIconColors.Teacher,
                        locked = true
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    MenuItemCard(
                        title = "Study progress",
                        subtitle = "See how your quizzes and study time are going",
                        icon = Icons.Filled.BarChart,
                        onClick = onLogin,
                        palette = NenaIconColors.Progress,
                        locked = true
                    )
                }
            }
        } else {
            when (selectedTab) {
                // Free for guests -> opens the real screens (Downloads is locked -> login page)
                GuestTab.Resources -> GuestResourcesTab(
                    onOpenPapers = onOpenPapers,
                    onOpenMarkingSchemes = onOpenMarkingSchemes,
                    onOpenQuizzes = onOpenQuizzes,
                    onOpenGuideBooks = onOpenGuideBooks,
                    onOpenSearch = onOpenSearch,
                    onLogin = onLogin,
                    modifier = Modifier.padding(innerPadding)
                )
                GuestTab.Study -> GuestStudyTab(
                    onLogin = onLogin,
                    modifier = Modifier.padding(innerPadding)
                )
                GuestTab.Profile -> GuestProfileTab(
                    onLogin = onLogin,
                    modifier = Modifier.padding(innerPadding)
                )
                else -> GuestLockedTab(
                    featureName = selectedTab.title,
                    benefit = selectedTab.benefit,
                    onLogin = onLogin,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}

/** Same look as the Student Home bottom bar; locked tabs carry a small lock badge. */
@Composable
private fun GuestBottomBar(selectedIndex: Int, onTabSelected: (Int) -> Unit) {
    NavigationBar {
        GuestTab.entries.forEachIndexed { index, tab ->
            NavigationBarItem(
                selected = index == selectedIndex,
                onClick = { onTabSelected(index) },
                icon = {
                    if (tab.locked) {
                        BadgedBox(
                            badge = {
                                Badge(containerColor = NenaAccent) {
                                    Icon(
                                        imageVector = Icons.Filled.Lock,
                                        contentDescription = null,
                                        modifier = Modifier.size(9.dp),
                                        tint = Color.White
                                    )
                                }
                            }
                        ) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = "${tab.label} (login required)"
                            )
                        }
                    } else {
                        Icon(imageVector = tab.icon, contentDescription = tab.label)
                    }
                },
                label = { Text(tab.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = NenaPrimary,
                    selectedTextColor = NenaPrimary,
                    indicatorColor = NenaPrimaryContainer
                )
            )
        }
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
            // Looks like a search bar; tapping opens the Search screen
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
                    .background(item.palette.container, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = item.icon, contentDescription = null, tint = item.palette.tint)
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