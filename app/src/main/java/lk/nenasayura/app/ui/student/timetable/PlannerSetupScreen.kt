package lk.nenasayura.app.ui.student.timetable



import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import lk.nenasayura.app.ui.common.EmptyView
import lk.nenasayura.app.ui.common.ErrorView
import lk.nenasayura.app.ui.common.LoadingView
import lk.nenasayura.app.ui.common.NenaTopBar

private val STEP_TITLES = listOf(
    "Your subjects",
    "Study days and hours",
    "When you study",
    "Exam and goals"
)

/** The 4-step wizard where a student sets up (or edits) their study plan answers. */
@Composable
fun PlannerSetupScreen(
    onBack: () -> Unit,
    onChooseSubjects: () -> Unit,
    onFinished: () -> Unit,
    viewModel: PlannerSetupViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val loadError = state.loadError

    // After choosing subjects in the profile, reload them when this screen comes back
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onResume() }

    // System Back goes to the previous step first
    BackHandler(enabled = state.step > 0) { viewModel.back() }

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) {
            Toast.makeText(context, "Plan settings saved", Toast.LENGTH_SHORT).show()
            onFinished()
        }
    }

    Scaffold(
        topBar = {
            NenaTopBar(
                title = if (state.isEditing) "Edit your plan" else "Plan your study",
                onBack = { if (!viewModel.back()) onBack() }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        val modifier = Modifier.padding(innerPadding)
        when {
            state.isLoading -> LoadingView(modifier)

            loadError != null -> ErrorView(
                message = loadError,
                onRetry = viewModel::load,
                modifier = modifier
            )

            state.subjects.isEmpty() -> EmptyView(
                message = "You have not chosen your stream and subjects yet. " +
                        "The planner builds your timetable from them.",
                modifier = modifier,
                actionLabel = "Choose subjects",
                onAction = onChooseSubjects
            )

            else -> SetupWizard(state = state, viewModel = viewModel, modifier = modifier)
        }
    }
}

@Composable
private fun SetupWizard(
    state: PlannerSetupUiState,
    viewModel: PlannerSetupViewModel,
    modifier: Modifier = Modifier
) {
    val isLastStep = state.step == PLANNER_SETUP_STEPS - 1
    val problem = state.stepError ?: state.saveError

    Column(modifier = modifier.fillMaxSize()) {
        // Step header
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(
                text = "Step ${state.step + 1} of $PLANNER_SETUP_STEPS",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { (state.step + 1f) / PLANNER_SETUP_STEPS },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = STEP_TITLES[state.step],
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        // The questions of this step
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            when (state.step) {
                0 -> SubjectsStep(
                    subjects = state.subjects,
                    confidence = state.confidence,
                    onChange = viewModel::setConfidence
                )

                1 -> DaysStep(
                    days = state.availableDays,
                    minutes = state.dailyMinutes,
                    onToggleDay = viewModel::toggleDay,
                    onChangeMinutes = viewModel::changeMinutes
                )

                2 -> TimeStep(
                    window = state.preferredWindow,
                    sessionMinutes = state.sessionMinutes,
                    onWindowChange = viewModel::setWindow,
                    onSessionChange = viewModel::setSessionMinutes
                )

                else -> GoalStep(
                    examDateMillis = state.examDateMillis,
                    profileExamYear = state.profileExamYear,
                    learningStyle = state.learningStyle,
                    goal = state.goal,
                    onExamDateChange = viewModel::setExamDate,
                    onStyleChange = viewModel::setLearningStyle,
                    onGoalChange = viewModel::setGoal
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Problem message and buttons
        Column(modifier = Modifier.padding(16.dp)) {
            if (problem != null) {
                Text(
                    text = problem,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)) {
                if (state.step > 0) {
                    OutlinedButton(
                        onClick = { viewModel.back() },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Back")
                    }
                }
                Button(
                    onClick = { if (isLastStep) viewModel.save() else viewModel.next() },
                    enabled = !state.isSaving,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(if (isLastStep) "Save plan settings" else "Next")
                    }
                }
            }
        }
    }
}