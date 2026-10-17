package lk.nenasayura.app.ui.student.quizzes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import lk.nenasayura.app.data.model.Quiz
import lk.nenasayura.app.ui.common.EmptyView
import lk.nenasayura.app.ui.common.ErrorView
import lk.nenasayura.app.ui.common.FilterChipRow
import lk.nenasayura.app.ui.common.LoadingView
import lk.nenasayura.app.ui.common.NenaTopBar
import lk.nenasayura.app.ui.theme.IconPalette

/** Lists available quizzes. Students arrive with one subject already chosen. */
@Composable
fun QuizListScreen(
    onBack: () -> Unit,
    onStartQuiz: (quizId: String) -> Unit,
    viewModel: QuizListViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val errorMessage = state.errorMessage
    val visibleQuizzes = state.filteredQuizzes

    // Student: the subject was chosen before
    val lockedSubjectId = viewModel.subjectId
    val topBarTitle = if (lockedSubjectId != null) {
        state.subjectName(lockedSubjectId).ifBlank { "Quizzes" }
    } else {
        "Quizzes"
    }

    Scaffold(
        topBar = { NenaTopBar(title = topBarTitle, onBack = onBack) },
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
                    onRetry = viewModel::load
                )

                state.quizzes.isEmpty() -> EmptyView(
                    message = if (lockedSubjectId != null) "No quizzes for this subject yet."
                    else "No quizzes are available yet."
                )

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        QuizHero(
                            title = "Pick a quiz",
                            subtitle = if (state.quizzes.size == 1) "1 quiz is waiting for you"
                            else "${state.quizzes.size} quizzes are waiting for you"
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = state.query,
                            onValueChange = viewModel::onQueryChange,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = {
                                Text(if (lockedSubjectId != null) "Search quizzes" else "Search by title or subject")
                            },
                            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                            trailingIcon = {
                                if (state.query.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.onQueryChange("") }) {
                                        Icon(Icons.Filled.Clear, contentDescription = "Clear search")
                                    }
                                }
                            },
                            singleLine = true,
                            shape = CircleShape
                        )
                    }

                    // Subject filter only when no subject was chosen before
                    if (lockedSubjectId == null && state.availableSubjects.isNotEmpty()) {
                        item {
                            FilterChipRow(
                                label = "Subject",
                                options = state.availableSubjects,
                                selected = state.availableSubjects
                                    .firstOrNull { it.id == state.selectedSubjectId },
                                optionLabel = { it.name },
                                onSelected = { viewModel.onSubjectSelected(it?.id) }
                            )
                        }
                    }

                    if (visibleQuizzes.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "No quizzes match your filters.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                if (state.hasActiveFilters) {
                                    TextButton(onClick = viewModel::clearFilters) {
                                        Text("Clear filters")
                                    }
                                }
                            }
                        }
                    } else {
                        itemsIndexed(visibleQuizzes, key = { _, quiz -> quiz.id }) { index, quiz ->
                            QuizCard(
                                number = index + 1,
                                quiz = quiz,
                                palette = QuizPalettes[index % QuizPalettes.size],
                                // The subject is already in the top bar for students
                                subjectName = if (lockedSubjectId != null) ""
                                else state.subjectName(quiz.subjectId),
                                onStart = { onStartQuiz(quiz.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/** One quiz: coloured side stripe, numbered badge, title/description and a play button. */
@Composable
private fun QuizCard(
    number: Int,
    quiz: Quiz,
    palette: IconPalette,
    subjectName: String,
    onStart: () -> Unit
) {
    Card(
        onClick = onStart,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(palette.tint)
            )

            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            Brush.linearGradient(
                                listOf(palette.tint, palette.tint.copy(alpha = 0.70f))
                            ),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = number.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = quiz.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (subjectName.isNotBlank()) {
                        Text(
                            text = subjectName,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.tint
                        )
                    }
                    if (quiz.description.isNotBlank()) {
                        Text(
                            text = quiz.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(palette.container, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Start quiz",
                        tint = palette.tint
                    )
                }
            }
        }
    }
}