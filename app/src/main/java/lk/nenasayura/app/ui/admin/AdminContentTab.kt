package lk.nenasayura.app.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import lk.nenasayura.app.data.model.AdminPermission
import lk.nenasayura.app.ui.theme.NenaPrimary
import lk.nenasayura.app.ui.theme.NenaSecondary

private enum class ContentSection(
    val label: String,
    val permission: AdminPermission,
    val icon: ImageVector,
    val tint: Color,
    val soft: Color
) {
    TeacherUploads("Teacher\nUploads", AdminPermission.TEACHER_UPLOADS, Icons.Filled.CloudUpload, Color(0xFF3949AB), Color(0xFFE3E7FA)),
    Papers("Papers", AdminPermission.PAPERS, Icons.Filled.Description, Color(0xFF1E88E5), Color(0xFFDDEBFB)),
    MarkingSchemes("Marking\nSchemes", AdminPermission.MARKING_SCHEMES, Icons.Filled.FactCheck, Color(0xFF7E57C2), Color(0xFFECE3F8)),
    GuideBooks("Guide\nBooks", AdminPermission.GUIDE_BOOKS, Icons.Filled.Book, Color(0xFFF9A825), Color(0xFFFFF1CC)),
    Quizzes("Quizzes", AdminPermission.QUIZZES, Icons.Filled.Quiz, Color(0xFF00897B), Color(0xFFD9F2EE)),
    Streams("Streams &\nSubjects", AdminPermission.STREAMS, Icons.Filled.AccountTree, Color(0xFFE64A19), Color(0xFFFBE3DC))
}

/** Content tab: add papers, marking schemes, guide books, quizzes, streams and subjects. */
@Composable
fun AdminContentTab(
    state: AdminUiState,
    viewModel: AdminViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    UploadSeenStore.init(context)
    val seen by UploadSeenStore.seen.collectAsStateWithLifecycle()
    // pending uploads + uploads the admin has not looked at yet
    val newUploads = UploadSeenStore.attentionCount(state.teacherContents, seen)

    // Only the sections this admin was given access to
    val sections = ContentSection.entries.filter { state.can(it.permission) }
    var sectionIndex by rememberSaveable { mutableIntStateOf(0) } // ordinal of the section
    val section = ContentSection.entries[sectionIndex].takeIf { it in sections }
        ?: sections.firstOrNull()
        ?: return

    Column(modifier = modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.height(12.dp))
        // Colourful tiles; they scroll sideways because they do not all fit on a small phone
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            sections.forEach { s ->
                SectionTile(
                    section = s,
                    selected = s == section,
                    badge = if (s == ContentSection.TeacherUploads) newUploads else 0,
                    onClick = { sectionIndex = s.ordinal }
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))

        when (section) {
            ContentSection.TeacherUploads -> TeacherUploadsSection(state, viewModel)
            ContentSection.Papers -> PapersSection(state, viewModel)
            ContentSection.MarkingSchemes -> MarkingSchemesSection(state)
            ContentSection.GuideBooks -> GuideBooksSection(state, viewModel)
            ContentSection.Quizzes -> QuizzesSection(state)
            ContentSection.Streams -> StreamsSection(state, viewModel)
        }
    }
}

@Composable
private fun SectionTile(
    section: ContentSection,
    selected: Boolean,
    badge: Int,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    Box {
        Surface(
            shape = shape,
            color = Color.Transparent,
            border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shadowElevation = if (selected) 6.dp else 1.dp
        ) {
            Column(
                modifier = Modifier
                    .width(92.dp)
                    .clickable(onClick = onClick)
                    .then(
                        if (selected) {
                            Modifier.background(Brush.linearGradient(listOf(NenaPrimary, NenaSecondary)))
                        } else {
                            Modifier.background(MaterialTheme.colorScheme.surface)
                        }
                    )
                    .padding(vertical = 12.dp, horizontal = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(
                            when {
                                selected -> Color.White.copy(alpha = 0.22f)
                                isSystemInDarkTheme() -> section.tint.copy(alpha = 0.22f)
                                else -> section.soft
                            },
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        section.icon,
                        contentDescription = null,
                        tint = if (selected) Color.White else section.tint,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = section.label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )
            }
        }
        if (badge > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 4.dp, end = 4.dp)
                    .background(Color(0xFFE53935), CircleShape)
                    .padding(horizontal = 7.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "$badge",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}