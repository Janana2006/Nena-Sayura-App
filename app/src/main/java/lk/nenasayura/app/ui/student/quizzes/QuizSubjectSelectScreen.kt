package lk.nenasayura.app.ui.student.quizzes

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
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import lk.nenasayura.app.core.browse.PaperBrowseSelection
import lk.nenasayura.app.ui.common.EmptyView
import lk.nenasayura.app.ui.common.ErrorView
import lk.nenasayura.app.ui.common.LoadingView
import lk.nenasayura.app.ui.common.NenaTopBar
import lk.nenasayura.app.ui.theme.IconPalette

/**
 * Quizzes (students): a colourful grid with ONLY the student's own subjects.
 * The "Change" button opens the same page as Papers / Marking Schemes.
 */
@Composable
fun QuizSubjectSelectScreen(
    onBack: () -> Unit,
    onChangeSelection: () -> Unit,
    onSubjectSelected: (subjectId: String) -> Unit,
    viewModel: QuizSubjectSelectViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val selection by PaperBrowseSelection.state.collectAsStateWithLifecycle()
    val errorMessage = state.errorMessage

    Scaffold(
        topBar = { NenaTopBar(title = "Quizzes", onBack = onBack) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                state.isLoading -> LoadingView()

                errorMessage != null -> ErrorView(
                    message = errorMessage,
                    onRetry = viewModel::retry
                )

                selection.subjects.isEmpty() ->
                    EmptyView(message = "You have not chosen any subjects yet.")

                else -> {
                    val totalQuizzes = selection.subjects.sumOf { state.quizCounts[it.id] ?: 0 }

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            QuizHero(
                                title = "Test yourself!",
                                subtitle = "${selection.subjects.size} subjects  •  " +
                                        if (totalQuizzes == 1) "1 quiz ready" else "$totalQuizzes quizzes ready"
                            ) {
                                Spacer(modifier = Modifier.height(14.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color.White.copy(alpha = 0.20f),
                                        modifier = Modifier.weight(1f, fill = false)
                                    ) {
                                        Text(
                                            text = selection.streamName,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    TextButton(
                                        onClick = onChangeSelection,
                                        colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Edit,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Change")
                                    }
                                }
                            }
                        }

                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Text(
                                text = "Choose a subject",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        itemsIndexed(selection.subjects, key = { _, subject -> subject.id }) { index, subject ->
                            SubjectTile(
                                name = subject.name,
                                quizCount = state.quizCounts[subject.id] ?: 0,
                                palette = QuizPalettes[index % QuizPalettes.size],
                                onClick = { onSubjectSelected(subject.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/** One subject tile: icon, big faded first letter, name and a quiz-count pill. */
@Composable
private fun SubjectTile(
    name: String,
    quizCount: Int,
    palette: IconPalette,
    onClick: () -> Unit
) {
    val countLabel = when (quizCount) {
        0 -> "Coming soon"
        1 -> "1 quiz"
        else -> "$quizCount quizzes"
    }

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(156.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Big faded first letter in the corner
            Text(
                text = name.trim().take(1).uppercase(),
                fontSize = 84.sp,
                fontWeight = FontWeight.Black,
                color = palette.tint.copy(alpha = 0.10f),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 12.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(palette.container, RoundedCornerShape(15.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Quiz,
                        contentDescription = null,
                        tint = palette.tint
                    )
                }

                Column {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = CircleShape,
                        color = if (quizCount > 0) palette.container
                        else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = countLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (quizCount > 0) palette.tint
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }
    }
}