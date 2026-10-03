package lk.nenasayura.app.ui.guest.quizzes



import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import lk.nenasayura.app.data.model.QuizQuestion
import lk.nenasayura.app.ui.theme.NenaAccentContainer
import lk.nenasayura.app.ui.theme.NenaPrimary
import lk.nenasayura.app.ui.theme.NenaSecondary

/** Score summary + a review of every question with its explanation. */
@Composable
fun QuizResultContent(
    state: QuizUiState,
    showLoginNote: Boolean,
    onTryAgain: () -> Unit,
    onBackToQuizzes: () -> Unit,
    onLogin: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { ScoreCard(state = state) }

        if (showLoginNote) {
            item { LoginNote(onLogin = onLogin) }
        }

        item {
            Text(
                text = "Review your answers",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        itemsIndexed(state.questions) { index, question ->
            ReviewCard(
                number = index + 1,
                question = question,
                selectedIndex = state.selectedAnswers[index]
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(onClick = onBackToQuizzes, modifier = Modifier.weight(1f)) {
                    Text("All quizzes")
                }
                Button(onClick = onTryAgain, modifier = Modifier.weight(1f)) {
                    Text("Try again")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ScoreCard(state: QuizUiState) {
    val total = state.questions.size
    val message = when {
        state.percent >= 80 -> "Excellent work!"
        state.percent >= 50 -> "Good effort. Keep practising!"
        else -> "Keep going. Read the explanations below and try again."
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.linearGradient(listOf(NenaPrimary, NenaSecondary)),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(24.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Your score",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.85f)
            )
            Text(
                text = "${state.score} / $total",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "${state.percent}%",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${state.score} correct  •  ${total - state.score} incorrect",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f)
            )
        }
    }
}

@Composable
private fun LoginNote(onLogin: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = NenaAccentContainer
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, top = 6.dp, bottom = 6.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Log in as a student to save your results and track your progress.",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF3E2A00)
            )
            TextButton(onClick = onLogin) { Text("Log in") }
        }
    }
}

@Composable
private fun ReviewCard(number: Int, question: QuizQuestion, selectedIndex: Int?) {
    val isCorrect = selectedIndex == question.correctIndex

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = if (isCorrect) "Question $number  ✓ Correct" else "Question $number  ✗ Incorrect",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (isCorrect) CorrectBorder else WrongBorder
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = question.text,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Your answer: " + optionLabel(question, selectedIndex),
                style = MaterialTheme.typography.bodyMedium,
                color = if (isCorrect) CorrectBorder else WrongBorder
            )
            if (!isCorrect) {
                Text(
                    text = "Correct answer: " + optionLabel(question, question.correctIndex),
                    style = MaterialTheme.typography.bodyMedium,
                    color = CorrectBorder
                )
            }

            if (question.explanation.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Explanation",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = question.explanation,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

// "B. Some option text", or "Not answered"
private fun optionLabel(question: QuizQuestion, index: Int?): String {
    if (index == null || index !in question.options.indices) return "Not answered"
    return "${'A' + index}. ${question.options[index]}"
}