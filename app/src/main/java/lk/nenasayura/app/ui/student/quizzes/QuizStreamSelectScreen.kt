package lk.nenasayura.app.ui.student.quizzes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import lk.nenasayura.app.data.model.StudyStream
import lk.nenasayura.app.ui.common.CategoryCard
import lk.nenasayura.app.ui.common.CategoryItem
import lk.nenasayura.app.ui.common.EmptyView
import lk.nenasayura.app.ui.common.ErrorView
import lk.nenasayura.app.ui.common.LoadingView
import lk.nenasayura.app.ui.common.NenaTopBar
import lk.nenasayura.app.ui.student.papers.StreamSelectViewModel
import lk.nenasayura.app.ui.theme.NenaIconColors

private val streamPalettes = listOf(
    NenaIconColors.Quizzes,
    NenaIconColors.Papers,
    NenaIconColors.MarkingSchemes,
    NenaIconColors.ResourceBooks,
    NenaIconColors.Timetable
)

/** Step 1 of Quizzes (students): choose a stream. Streams come from the Firestore "streams" collection. */
@Composable
fun QuizStreamSelectScreen(
    onBack: () -> Unit,
    onStreamSelected: (StudyStream) -> Unit,
    viewModel: StreamSelectViewModel = viewModel()
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
                    onRetry = viewModel::load
                )

                state.streams.isEmpty() -> EmptyView(message = "No streams are available yet.")

                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Column {
                            Text(
                                text = "Choose your stream",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "You will only see quizzes for this stream",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    itemsIndexed(state.streams, key = { _, stream -> stream.id }) { index, stream ->
                        CategoryCard(
                            CategoryItem(
                                title = stream.name,
                                subtitle = "${stream.subjects.size} subjects",
                                icon = Icons.Filled.School,
                                palette = streamPalettes[index % streamPalettes.size],
                                onClick = { onStreamSelected(stream) }
                            )
                        )
                    }
                }
            }
        }
    }
}