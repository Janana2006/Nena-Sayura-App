package lk.nenasayura.app.ui.guest.quizzes

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import lk.nenasayura.app.ui.common.ErrorView
import lk.nenasayura.app.ui.common.FilterChipRow
import lk.nenasayura.app.ui.common.LoadingView
import lk.nenasayura.app.ui.common.NenaTopBar
import lk.nenasayura.app.ui.student.quizzes.QuizHero
import lk.nenasayura.app.ui.student.quizzes.QuizPalettes
import lk.nenasayura.app.ui.theme.IconPalette

/**
 * Quizzes for guests. The guest first chooses a stream + subjects (same as Papers),
 * then only those subjects are shown as chips (no "All"), and only the ones that have quizzes.
 * [onChangeSelection] opens the stream / subject choice again.
 */
@Composable
fun QuizListScreen(
    onBack: () -> Unit,
    onStartQuiz: (quizId: String) -> Unit,
    onChangeSelection: (() -> Unit)? = null,
    viewModel: QuizListViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val errorMessage = state.errorMessage
    val visibleQuizzes = state.filteredQuizzes
    val side = Modifier.padding(horizontal = 16.dp)

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
                    onRetry = viewModel::load
                )

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        QuizHero(
                            title = "Pick a quiz",
                            subtitle = if (state.restricted) "Quizzes for your subjects"
                            else "${state.quizzes.size} quizzes are waiting for you",
                            modifier = side
                        ) {
                            if (state.restricted && onChangeSelection != null) {
                                Spacer(modifier = Modifier.height(14.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color.White.copy(alpha = 0.20f),
                                        modifier = Modifier.weight(1f, fill = false)
                                    ) {
                                        Text(
                                            text = state.streamName,
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
                    }

                    if (state.quizzes.isEmpty()) {
                        item {
                            Text(
                                text = if (state.restricted) "No quizzes have been added for your subjects yet."
                                else "No quizzes are available yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 32.dp, vertical = 32.dp)
                            )
                        }
                    } else {
                        item {
                            OutlinedTextField(
                                value = state.query,
                                onValueChange = viewModel::onQueryChange,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                placeholder = { Text("Search quizzes") },
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

                        // Chosen subjects that have quizzes. No "All" chip when the guest chose subjects.
                        if (state.availableSubjects.isNotEmpty()) {
                            item {
                                FilterChipRow(
                                    label = if (state.restricted) "Your subjects" else "Subject",
                                    options = state.availableSubjects,
                                    selected = state.availableSubjects
                                        .firstOrNull { it.id == state.selectedSubjectId },
                                    optionLabel = { it.name },
                                    onSelected = { subject ->
                                        if (subject != null || !state.restricted) {
                                            viewModel.onSubjectSelected(subject?.id)
                                        }
                                    },
                                    showAll = !state.restricted
                                )
                            }
                        }

                        if (visibleQuizzes.isEmpty()) {
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "No quizzes match your search.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (state.hasActiveFilters) {
                                        TextButton(onClick = viewModel::clearFilters) {
                                            Text("Clear search")
                                        }
                                    }
                                }
                            }
                        } else {
                            item {
                                Text(
                                    text = if (visibleQuizzes.size == 1) "1 quiz" else "${visibleQuizzes.size} quizzes",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = side
                                )
                            }
                            items(visibleQuizzes, key = { it.id }) { quiz ->
                                val index = state.subjects.indexOfFirst { it.id == quiz.subjectId }.coerceAtLeast(0)
                                QuizCard(
                                    quiz = quiz,
                                    palette = QuizPalettes[index % QuizPalettes.size],
                                    // The chosen subject chip is already on screen
                                    subjectName = if (state.restricted) "" else state.subjectName(quiz.subjectId),
                                    onStart = { onStartQuiz(quiz.id) },
                                    modifier = side
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** One quiz: coloured stripe, play button, title and description. The whole card can be tapped. */
@Composable
private fun QuizCard(
    quiz: Quiz,
    palette: IconPalette,
    subjectName: String,
    onStart: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onStart,
        modifier = modifier.fillMaxWidth(),
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
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Start quiz",
                        tint = Color.White
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
            }
        }
    }
}