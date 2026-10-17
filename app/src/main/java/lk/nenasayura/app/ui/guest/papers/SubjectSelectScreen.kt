package lk.nenasayura.app.ui.student.papers

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import lk.nenasayura.app.ui.common.EmptyView
import lk.nenasayura.app.ui.common.ErrorView
import lk.nenasayura.app.ui.common.HeroBanner
import lk.nenasayura.app.ui.common.LoadingView
import lk.nenasayura.app.ui.common.NenaTopBar
import lk.nenasayura.app.ui.theme.IconPalette
import lk.nenasayura.app.ui.theme.NenaIconColors
import lk.nenasayura.app.ui.theme.NenaPrimary

private val subjectPalettes = listOf(
    NenaIconColors.Papers,
    NenaIconColors.MarkingSchemes,
    NenaIconColors.ResourceBooks,
    NenaIconColors.Quizzes,
    NenaIconColors.Timetable
)

/** Step 2 (Papers / Marking Schemes): tick up to 3 subjects of the chosen stream, then Continue. */
@Composable
fun SubjectSelectScreen(
    onBack: () -> Unit,
    onDone: () -> Unit,
    title: String = "Papers",
    hint: String = "Papers will be shown for these subjects only",
    viewModel: SubjectSelectViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val errorMessage = state.errorMessage
    val stream = state.stream

    Scaffold(
        topBar = { NenaTopBar(title = stream?.name ?: title, onBack = onBack) },
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (stream != null && stream.subjects.isNotEmpty()) {
                Surface(shadowElevation = 8.dp, color = MaterialTheme.colorScheme.surface) {
                    Button(
                        onClick = { if (viewModel.confirm()) onDone() },
                        enabled = state.selectedIds.isNotEmpty(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            if (state.selectedIds.isEmpty()) "Select at least one subject"
                            else "Continue (${state.selectedIds.size} selected)"
                        )
                    }
                }
            }
        }
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

                stream == null || stream.subjects.isEmpty() ->
                    EmptyView(message = "No subjects are available for this stream yet.")

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        HeroBanner(
                            title = "Choose your subjects",
                            subtitle = hint,
                            icon = Icons.AutoMirrored.Filled.MenuBook
                        ) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // One dot per allowed subject: filled = chosen
                                repeat(state.maxSubjects) { index ->
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .background(
                                                if (index < state.selectedIds.size) Color.White
                                                else Color.White.copy(alpha = 0.30f),
                                                CircleShape
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (state.isFull) "3 of 3 selected - untick one to change"
                                    else "${state.selectedIds.size} of ${state.maxSubjects} selected",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    itemsIndexed(stream.subjects, key = { _, subject -> subject.id }) { index, subject ->
                        val checked = subject.id in state.selectedIds
                        SubjectChoiceCard(
                            name = subject.name,
                            checked = checked,
                            // When 3 are chosen the others are locked (but a ticked one can always be unticked)
                            enabled = checked || !state.isFull,
                            palette = subjectPalettes[index % subjectPalettes.size],
                            onClick = { viewModel.toggle(subject.id) }
                        )
                    }
                }
            }
        }
    }
}

/** One subject: coloured letter avatar, name and a round tick. */
@Composable
private fun SubjectChoiceCard(
    name: String,
    checked: Boolean,
    enabled: Boolean,
    palette: IconPalette,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.45f),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (checked) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface,
            disabledContainerColor = MaterialTheme.colorScheme.surface
        ),
        border = if (checked) BorderStroke(2.dp, NenaPrimary) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(palette.container, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = name.trim().take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = palette.tint
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Box(
                modifier = Modifier
                    .size(26.dp)
                    .then(
                        if (checked) Modifier.background(NenaPrimary, CircleShape)
                        else Modifier.border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (checked) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}