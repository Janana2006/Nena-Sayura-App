package lk.nenasayura.app.ui.student.progress



import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import lk.nenasayura.app.core.progress.AttemptPoint
import lk.nenasayura.app.core.progress.Level
import lk.nenasayura.app.core.progress.OverallProgress
import lk.nenasayura.app.core.progress.ProgressReport
import lk.nenasayura.app.core.progress.QuizProgress
import lk.nenasayura.app.core.progress.Recommendation
import lk.nenasayura.app.core.progress.SubjectProgress
import lk.nenasayura.app.core.progress.Trend
import lk.nenasayura.app.ui.common.EmptyView
import lk.nenasayura.app.ui.common.ErrorView
import lk.nenasayura.app.ui.common.LoadingView
import lk.nenasayura.app.ui.common.NenaTopBar
import lk.nenasayura.app.ui.theme.NenaAccent
import lk.nenasayura.app.ui.theme.NenaPrimary
import java.util.concurrent.TimeUnit

// Level colours (same green / red as the quiz answer feedback)
private val StrongColor = Color(0xFF2E7D32)
private val MediumColor = NenaAccent
private val WeakColor = Color(0xFFC62828)
private val IdleColor = Color(0xFF9AA0B4)

private fun levelColor(level: Level): Color = when (level) {
    Level.STRONG -> StrongColor
    Level.GETTING_THERE -> MediumColor
    Level.NEEDS_WORK -> WeakColor
    Level.NOT_STARTED -> IdleColor
}

private fun percentColor(percent: Int): Color = when {
    percent >= 80 -> StrongColor
    percent >= 55 -> MediumColor
    else -> WeakColor
}

/** "today", "yesterday" or "3 days ago". */
private fun ago(millis: Long): String {
    if (millis <= 0L) return "never"
    val days = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - millis).toInt()
    return when {
        days <= 0 -> "today"
        days == 1 -> "yesterday"
        else -> "$days days ago"
    }
}

/** Study progress, worked out from the student's quiz answers. */
@Composable
fun StudyProgressScreen(
    onBack: () -> Unit,
    onOpenQuizzes: () -> Unit,
    onOpenQuiz: (String) -> Unit,
    viewModel: StudyProgressViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val report = state.report
    val errorMessage = state.errorMessage

    // Load when the screen opens and again when the student comes back from a quiz
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.load() }

    Scaffold(
        topBar = { NenaTopBar(title = "Study Progress", onBack = onBack) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        val modifier = Modifier.padding(innerPadding)
        when {
            state.isLoading -> LoadingView(modifier)

            errorMessage != null && report == null -> ErrorView(
                message = errorMessage,
                onRetry = viewModel::load,
                modifier = modifier
            )

            report == null -> LoadingView(modifier)

            !report.hasData -> EmptyView(
                message = "No quiz results yet. Finish a quiz and Nena Sayura will show " +
                        "how you are doing in each subject.",
                modifier = modifier,
                actionLabel = "Go to quizzes",
                onAction = onOpenQuizzes
            )

            else -> ProgressContent(
                report = report,
                onOpenQuizzes = onOpenQuizzes,
                onOpenQuiz = onOpenQuiz,
                modifier = modifier
            )
        }
    }
}

@Composable
private fun ProgressContent(
    report: ProgressReport,
    onOpenQuizzes: () -> Unit,
    onOpenQuiz: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "summary") { SummaryCard(report.overall) }

        if (report.recentAttempts.size >= 2) {
            item(key = "chart") { RecentScoresCard(report.recentAttempts) }
        }

        item(key = "tips_title") { SectionTitle("What to do next") }
        items(report.recommendations, key = { "tip_" + it.title }) { tip ->
            TipCard(
                tip = tip,
                onClick = { if (tip.quizId != null) onOpenQuiz(tip.quizId) else onOpenQuizzes() }
            )
        }

        item(key = "subjects_title") { SectionTitle("Your subjects") }
        items(report.subjects, key = { "subject_" + it.subjectId }) { subject ->
            SubjectCard(subject = subject, onOpenQuiz = onOpenQuiz)
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 4.dp)
    )
}

// ---------- summary ----------

@Composable
private fun SummaryCard(overall: OverallProgress) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = NenaPrimary)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Recent average",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.8f)
            )
            Text(
                text = "${overall.recentAveragePercent}%",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Average of your last 5 quizzes",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                SummaryStat(
                    value = "${overall.quizzesAttempted}/${overall.quizzesAvailable}",
                    label = "Quizzes tried",
                    modifier = Modifier.weight(1f)
                )
                SummaryStat(
                    value = "${overall.accuracy}%",
                    label = "Accuracy",
                    modifier = Modifier.weight(1f)
                )
                SummaryStat(
                    value = "${overall.streakDays}",
                    label = "Day streak",
                    modifier = Modifier.weight(1f)
                )
                SummaryStat(
                    value = "${overall.bestPercent}%",
                    label = "Best",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "${overall.attemptsLast7Days} quiz attempts in the last 7 days " +
                        "(${overall.correctAnswers} of ${overall.questionsAnswered} answers correct overall)",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun SummaryStat(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.8f)
        )
    }
}

// ---------- recent scores chart ----------

private const val CHART_SLOTS = 10

@Composable
private fun RecentScoresCard(points: List<AttemptPoint>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Your last ${points.size} quizzes",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Oldest on the left, newest on the right",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                // Empty slots on the left keep every bar slim when there are only a few attempts
                repeat(CHART_SLOTS - points.size) { Spacer(modifier = Modifier.weight(1f)) }

                points.forEach { point ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Text(
                            text = "${point.percent}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height((84 * point.percent / 100f).coerceAtLeast(4f).dp)
                                .background(
                                    percentColor(point.percent),
                                    RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                                )
                        )
                    }
                }
            }
        }
    }
}

// ---------- tips ----------

@Composable
private fun TipCard(tip: Recommendation, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(44.dp)
                    .background(NenaAccent, RoundedCornerShape(2.dp))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tip.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = tip.detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ---------- subjects ----------

@Composable
private fun SubjectCard(subject: SubjectProgress, onOpenQuiz: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val color = levelColor(subject.level)
    val canExpand = subject.quizzes.isNotEmpty()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .then(if (canExpand) Modifier.clickable { expanded = !expanded } else Modifier)
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = subject.name,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subject.level.label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = color,
                    modifier = Modifier
                        .background(color.copy(alpha = 0.14f), RoundedCornerShape(50))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            if (subject.level == Level.NOT_STARTED) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (subject.quizzesAvailable > 0) {
                        "No quizzes tried yet (${subject.quizzesAvailable} available)"
                    } else {
                        "No quizzes available yet"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                return@Column
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                LinearProgressIndicator(
                    progress = { subject.currentPercent / 100f },
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp),
                    color = color,
                    trackColor = color.copy(alpha = 0.15f)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "${subject.currentPercent}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${subject.attempts} attempts - ${subject.quizzesAttempted} of " +
                        "${subject.quizzesAvailable} quizzes tried - last ${ago(subject.lastAttemptMillis)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = trendText(subject) + if (!subject.isReliable) " (try a few more quizzes for a clearer picture)" else "",
                style = MaterialTheme.typography.bodySmall,
                color = trendColor(subject.trend)
            )

            if (canExpand) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (expanded) "Hide lessons" else "Show lessons (weakest first)",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = NenaPrimary
                )
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                subject.quizzes.forEach { quiz ->
                    QuizRow(quiz = quiz, onClick = { onOpenQuiz(quiz.quizId) })
                }
            }
        }
    }
}

private fun trendText(subject: SubjectProgress): String = when (subject.trend) {
    Trend.UP -> "Improving: +${subject.trendDelta} points compared with before"
    Trend.DOWN -> "Slipping: ${subject.trendDelta} points compared with before"
    Trend.STEADY -> "Steady compared with before"
    Trend.NEW -> "Not enough attempts yet to see a trend"
}

private fun trendColor(trend: Trend): Color = when (trend) {
    Trend.UP -> StrongColor
    Trend.DOWN -> WeakColor
    else -> IdleColor
}

@Composable
private fun QuizRow(quiz: QuizProgress, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = quiz.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = buildString {
                    append("${quiz.attempts} ${if (quiz.attempts == 1) "attempt" else "attempts"} - best ${quiz.bestPercent}%")
                    if (quiz.attempts > 1) {
                        val change = quiz.improvement
                        append(" - ${if (change >= 0) "+" else ""}$change since first try")
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${quiz.latestPercent}%",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = percentColor(quiz.latestPercent)
            )
            if (quiz.needsRevision) {
                Text(
                    text = "Revise",
                    style = MaterialTheme.typography.labelSmall,
                    color = WeakColor
                )
            }
        }
    }
}