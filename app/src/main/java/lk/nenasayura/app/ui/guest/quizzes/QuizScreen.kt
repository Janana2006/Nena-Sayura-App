package lk.nenasayura.app.ui.guest.quizzes

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import lk.nenasayura.app.core.session.currentSession
import lk.nenasayura.app.core.session.isStudent
import lk.nenasayura.app.data.model.QuizQuestion
import lk.nenasayura.app.ui.common.EmptyView
import lk.nenasayura.app.ui.common.ErrorView
import lk.nenasayura.app.ui.common.LoadingView
import lk.nenasayura.app.ui.common.NenaTopBar
import lk.nenasayura.app.ui.theme.NenaAccent
import lk.nenasayura.app.ui.theme.NenaPrimary
import lk.nenasayura.app.ui.theme.NenaPrimaryContainer
import lk.nenasayura.app.ui.theme.NenaSecondary

// Colours for answer feedback. QuizResultContent.kt also uses these, so keep the names.
// Text on these is always dark, so it is readable in dark mode too.
internal val CorrectFill = Color(0xFFDFF5E3)
internal val CorrectBorder = Color(0xFF2E7D32)
internal val WrongFill = Color(0xFFFDE0DD)
internal val WrongBorder = Color(0xFFC62828)
private val DarkText = Color(0xFF1B1B1F)

private enum class OptionState { Normal, Selected, Correct, Wrong }

private val CorrectTitles = listOf("Correct!", "Spot on!", "Well done!", "Excellent!")

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
    val selected = state.selectedAnswers[state.currentIndex]
    val checked = state.currentIndex in state.checkedQuestions
    val currentQuestion = state.currentQuestion ?: return
    val isCorrect = checked && selected == currentQuestion.correctIndex
    val scrollState = rememberScrollState()

    // New question -> back to the top. Answer checked -> scroll down to the explanation.
    LaunchedEffect(state.currentIndex) { scrollState.scrollTo(0) }
    LaunchedEffect(checked) {
        if (checked) {
            delay(250)
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            QuizHeader(state = state)
            Spacer(modifier = Modifier.height(16.dp))

            AnimatedContent(
                targetState = state.currentIndex,
                transitionSpec = {
                    (slideInHorizontally(tween(300)) { it / 4 } + fadeIn(tween(300))) togetherWith
                            fadeOut(tween(150))
                },
                label = "question"
            ) { index ->
                val question = state.questions.getOrNull(index) ?: return@AnimatedContent
                val questionSelected = state.selectedAnswers[index]
                val questionChecked = index in state.checkedQuestions

                Column {
                    QuestionCard(number = index + 1, total = state.questions.size, text = question.text)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (questionChecked) "Your result" else "Choose the best answer",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    question.options.forEachIndexed { optionIndex, optionText ->
                        val optionState = when {
                            questionChecked && optionIndex == question.correctIndex -> OptionState.Correct
                            questionChecked && optionIndex == questionSelected -> OptionState.Wrong
                            !questionChecked && optionIndex == questionSelected -> OptionState.Selected
                            else -> OptionState.Normal
                        }
                        OptionCard(
                            letter = ('A' + optionIndex).toString(),
                            text = optionText,
                            optionState = optionState,
                            dimmed = questionChecked && optionState == OptionState.Normal,
                            enabled = !questionChecked,
                            onClick = { onSelect(optionIndex) }
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    AnimatedVisibility(
                        visible = questionChecked,
                        enter = slideInVertically(tween(300)) { it / 2 } + fadeIn(tween(300))
                    ) {
                        FeedbackCard(
                            question = question,
                            selectedIndex = questionSelected,
                            titleSeed = index
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Sticky bottom action bar so the button is always reachable
        Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 10.dp) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                if (!checked) {
                    Button(
                        onClick = onCheck,
                        enabled = selected != null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Check answer", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = onNext,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isCorrect) CorrectBorder else NenaPrimary,
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = if (state.isLastQuestion) "Finish quiz" else "Next question",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/** "Question 3 of 10", the live score, and one progress capsule per question. */
@Composable
private fun QuizHeader(state: QuizUiState) {
    val total = state.questions.size
    // Only count answers that have already been checked
    val liveScore = state.checkedQuestions.count { index ->
        state.selectedAnswers[index] == state.questions.getOrNull(index)?.correctIndex
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Question ${state.currentIndex + 1} of $total",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier
                .background(Color(0xFFFFE9B8), RoundedCornerShape(50))
                .padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = NenaAccent,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "$liveScore",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF3E2A00)
            )
        }
    }

    Spacer(modifier = Modifier.height(10.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        for (index in 0 until total) {
            val target = when {
                index in state.checkedQuestions ->
                    if (state.selectedAnswers[index] == state.questions[index].correctIndex) {
                        CorrectBorder
                    } else {
                        WrongBorder
                    }

                index == state.currentIndex -> NenaPrimary
                else -> MaterialTheme.colorScheme.outlineVariant
            }
            val color by animateColorAsState(target, tween(300), label = "segment")
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(color)
            )
        }
    }
}

@Composable
private fun QuestionCard(number: Int, total: Int, text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        // Brand gradient strip along the top, same colours as the score card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(Brush.horizontalGradient(listOf(NenaPrimary, NenaSecondary)))
        )
        Column(modifier = Modifier.padding(18.dp)) {
            Box(
                modifier = Modifier
                    .background(NenaPrimaryContainer, RoundedCornerShape(50))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Q$number / $total",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = NenaPrimary
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                lineHeight = MaterialTheme.typography.titleMedium.lineHeight * 1.15f
            )
        }
    }
}

@Composable
private fun OptionCard(
    letter: String,
    text: String,
    optionState: OptionState,
    dimmed: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val surface = MaterialTheme.colorScheme.surface
    val outline = MaterialTheme.colorScheme.outlineVariant

    val targetFill: Color
    val targetBorder: Color
    when (optionState) {
        OptionState.Correct -> { targetFill = CorrectFill; targetBorder = CorrectBorder }
        OptionState.Wrong -> { targetFill = WrongFill; targetBorder = WrongBorder }
        OptionState.Selected -> { targetFill = NenaPrimaryContainer; targetBorder = NenaPrimary }
        OptionState.Normal -> { targetFill = surface; targetBorder = outline }
    }
    val fill by animateColorAsState(targetFill, tween(300), label = "fill")
    val border by animateColorAsState(targetBorder, tween(300), label = "border")
    val textColor =
        if (optionState == OptionState.Normal) MaterialTheme.colorScheme.onSurface else DarkText

    // Small pop when selected or when the right answer is revealed
    val scale by animateFloatAsState(
        targetValue = when (optionState) {
            OptionState.Selected, OptionState.Correct -> 1.02f
            else -> 1f
        },
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    // Shake sideways when the chosen answer turns out to be wrong
    val shake = remember { Animatable(0f) }
    LaunchedEffect(optionState == OptionState.Wrong) {
        if (optionState == OptionState.Wrong) {
            repeat(3) {
                shake.animateTo(10f, tween(50))
                shake.animateTo(-10f, tween(50))
            }
            shake.animateTo(0f, tween(50))
        }
    }

    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .offset { IntOffset(shake.value.dp.roundToPx(), 0) }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = if (dimmed) 0.55f else 1f
            },
        shape = RoundedCornerShape(16.dp),
        color = fill,
        border = BorderStroke(width = 1.5.dp, color = border),
        shadowElevation = if (optionState == OptionState.Normal) 0.dp else 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Letter badge turns into a tick / cross once the answer is checked
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(border.copy(alpha = 0.18f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                when (optionState) {
                    OptionState.Correct -> Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = "Correct",
                        tint = CorrectBorder,
                        modifier = Modifier.size(26.dp)
                    )

                    OptionState.Wrong -> Icon(
                        Icons.Filled.Cancel,
                        contentDescription = "Wrong",
                        tint = WrongBorder,
                        modifier = Modifier.size(26.dp)
                    )

                    else -> Text(
                        text = letter,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = text,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (optionState == OptionState.Correct || optionState == OptionState.Wrong) {
                    FontWeight.SemiBold
                } else {
                    FontWeight.Normal
                },
                color = textColor
            )
        }
    }
}

/** Green when right. Red when wrong, with the correct answer and the explanation. */
@Composable
private fun FeedbackCard(question: QuizQuestion, selectedIndex: Int?, titleSeed: Int) {
    val isCorrect = selectedIndex == question.correctIndex
    val accent = if (isCorrect) CorrectBorder else WrongBorder

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = if (isCorrect) CorrectFill else WrongFill,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isCorrect) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (isCorrect) CorrectTitles[titleSeed % CorrectTitles.size] else "Not quite",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = accent
                )
            }

            if (!isCorrect) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Correct answer",
                    style = MaterialTheme.typography.labelMedium,
                    color = DarkText.copy(alpha = 0.7f)
                )
                Text(
                    text = "${'A' + question.correctIndex}. ${question.options.getOrElse(question.correctIndex) { "" }}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = CorrectBorder
                )
            }

            if (question.explanation.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lightbulb,
                        contentDescription = null,
                        tint = NenaAccent,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Why?",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = DarkText
                        )
                        Text(
                            text = question.explanation,
                            style = MaterialTheme.typography.bodyMedium,
                            color = DarkText
                        )
                    }
                }
            }
        }
    }
}