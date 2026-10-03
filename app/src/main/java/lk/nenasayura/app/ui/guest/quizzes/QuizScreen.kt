package lk.nenasayura.app.ui.guest.quizzes



import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import lk.nenasayura.app.core.session.currentSession
import lk.nenasayura.app.core.session.isStudent
import lk.nenasayura.app.ui.common.EmptyView
import lk.nenasayura.app.ui.common.ErrorView
import lk.nenasayura.app.ui.common.LoadingView
import lk.nenasayura.app.ui.common.NenaTopBar
import lk.nenasayura.app.ui.theme.NenaPrimary
import lk.nenasayura.app.ui.theme.NenaPrimaryContainer

// Colours for answer feedback. Text on these is always dark, so it is readable in dark mode too.
internal val CorrectFill = Color(0xFFDFF5E3)
internal val CorrectBorder = Color(0xFF2E7D32)
internal val WrongFill = Color(0xFFFDE0DD)
internal val WrongBorder = Color(0xFFC62828)
private val DarkText = Color(0xFF1B1B1F)

private enum class OptionState { Normal, Selected, Correct, Wrong }

/** Plays one quiz: question by question, then shows the result. */
@Composable
fun QuizScreen(
    onBack: () -> Unit,
    onLogin: () -> Unit,
    viewModel: QuizViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val session by currentSession()
    val errorMessage = state.errorMessage

    Scaffold(
        topBar = { NenaTopBar(title = state.quizTitle.ifBlank { "Quiz" }, onBack = onBack) },
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

                state.questions.isEmpty() -> EmptyView(message = "This quiz has no questions yet.")

                state.isFinished -> QuizResultContent(
                    state = state,
                    showLoginNote = !session.isStudent,
                    onTryAgain = viewModel::restart,
                    onBackToQuizzes = onBack,
                    onLogin = onLogin
                )

                else -> QuestionContent(
                    state = state,
                    onSelect = viewModel::selectAnswer,
                    onCheck = viewModel::checkAnswer,
                    onNext = viewModel::next
                )
            }
        }
    }
}

@Composable
private fun QuestionContent(
    state: QuizUiState,
    onSelect: (Int) -> Unit,
    onCheck: () -> Unit,
    onNext: () -> Unit
) {
    val question = state.currentQuestion ?: return
    val selected = state.selectedAnswers[state.currentIndex]
    val checked = state.currentIndex in state.checkedQuestions
    val total = state.questions.size

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Question ${state.currentIndex + 1} of $total",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { (state.currentIndex + 1).toFloat() / total },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = question.text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(16.dp))

        question.options.forEachIndexed { index, optionText ->
            val optionState = when {
                checked && index == question.correctIndex -> OptionState.Correct
                checked && index == selected -> OptionState.Wrong
                !checked && index == selected -> OptionState.Selected
                else -> OptionState.Normal
            }
            OptionRow(
                letter = ('A' + index).toString(),
                text = optionText,
                optionState = optionState,
                enabled = !checked,
                onClick = { onSelect(index) }
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        if (checked) {
            Spacer(modifier = Modifier.height(4.dp))
            FeedbackCard(
                isCorrect = selected == question.correctIndex,
                explanation = question.explanation
            )
            Spacer(modifier = Modifier.height(16.dp))
        } else {
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (!checked) {
            Button(
                onClick = onCheck,
                enabled = selected != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Check answer")
            }
        } else {
            Button(
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(if (state.isLastQuestion) "Finish quiz" else "Next question")
            }
        }
    }
}

@Composable
private fun OptionRow(
    letter: String,
    text: String,
    optionState: OptionState,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val fill: Color
    val border: Color
    when (optionState) {
        OptionState.Correct -> { fill = CorrectFill; border = CorrectBorder }
        OptionState.Wrong -> { fill = WrongFill; border = WrongBorder }
        OptionState.Selected -> { fill = NenaPrimaryContainer; border = NenaPrimary }
        OptionState.Normal -> {
            fill = MaterialTheme.colorScheme.surface
            border = MaterialTheme.colorScheme.outlineVariant
        }
    }
    val textColor =
        if (optionState == OptionState.Normal) MaterialTheme.colorScheme.onSurface else DarkText

    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = fill,
        border = BorderStroke(width = 1.5.dp, color = border)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .background(border.copy(alpha = 0.18f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = letter,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = text,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                color = textColor
            )
            if (optionState == OptionState.Correct) {
                Icon(Icons.Filled.CheckCircle, contentDescription = "Correct", tint = CorrectBorder)
            } else if (optionState == OptionState.Wrong) {
                Icon(Icons.Filled.Cancel, contentDescription = "Wrong", tint = WrongBorder)
            }
        }
    }
}

@Composable
private fun FeedbackCard(isCorrect: Boolean, explanation: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = if (isCorrect) CorrectFill else WrongFill
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = if (isCorrect) "Correct!" else "Not quite",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isCorrect) CorrectBorder else WrongBorder
            )
            if (explanation.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Explanation",
                    style = MaterialTheme.typography.labelLarge,
                    color = DarkText
                )
                Text(
                    text = explanation,
                    style = MaterialTheme.typography.bodyMedium,
                    color = DarkText
                )
            }
        }
    }
}