package lk.nenasayura.app.ui.student.quizzes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import lk.nenasayura.app.ui.common.CategoryCard
import lk.nenasayura.app.ui.common.CategoryItem
import lk.nenasayura.app.ui.common.EmptyView
import lk.nenasayura.app.ui.common.ErrorView
import lk.nenasayura.app.ui.common.LoadingView
import lk.nenasayura.app.ui.common.NenaTopBar
import lk.nenasayura.app.ui.theme.NenaIconColors

private val subjectPalettes = listOf(
    NenaIconColors.Quizzes,
    NenaIconColors.Papers,
    NenaIconColors.MarkingSchemes,
    NenaIconColors.ResourceBooks,
    NenaIconColors.Timetable
)

/** Step 2 of Quizzes (students): subjects of the chosen stream. Tapping one opens its quizzes. */
@Composable
fun QuizSubjectSelectScreen(
    onBack: () -> Unit,
    onChangeStream: () -> Unit,
    onSubjectSelected: (subjectId: String) -> Unit,
    viewModel: QuizSubjectSelectViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
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

                state.subjects.isEmpty() ->
                    EmptyView(message = "No subjects are available for this stream yet.")

                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item { StreamSummary(streamName = state.streamName, onChange = onChangeStream) }
                    item {
                        Text(
                            text = "Choose a subject",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    itemsIndexed(state.subjects, key = { _, subject -> subject.id }) { index, subject ->
                        val count = state.quizCounts[subject.id] ?: 0
                        CategoryCard(
                            CategoryItem(
                                title = subject.name,
                                subtitle = when (count) {
                                    0 -> "No quizzes yet"
                                    1 -> "1 quiz"
                                    else -> "$count quizzes"
                                },
                                icon = Icons.Filled.Quiz,
                                palette = subjectPalettes[index % subjectPalettes.size],
                                onClick = { onSubjectSelected(subject.id) }
                            )
                        )
                    }
                }
            }
        }
    }
}

/** Small card: "Your stream - Maths Stream" + Change. */
@Composable
private fun StreamSummary(streamName: String, onChange: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Your stream", style = MaterialTheme.typography.labelSmall)
                Text(
                    text = streamName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            TextButton(onClick = onChange) { Text("Change") }
        }
    }
}