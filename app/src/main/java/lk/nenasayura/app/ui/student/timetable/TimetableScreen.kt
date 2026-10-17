package lk.nenasayura.app.ui.student.timetable

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import lk.nenasayura.app.data.model.SessionStatus
import lk.nenasayura.app.data.model.StudySession
import lk.nenasayura.app.ui.common.EmptyView
import lk.nenasayura.app.ui.common.ErrorView
import lk.nenasayura.app.ui.common.LoadingView
import lk.nenasayura.app.ui.common.NenaTopBar
import java.time.LocalDate
import lk.nenasayura.app.core.reminders.ReminderSyncWorker

/**
 * The student's Timetable: Day and Week views, progress, and the session actions.
 * Before the first timetable exists it shows the generate card instead.
 */
@Composable
fun TimetableScreen(
    onBack: () -> Unit,
    onSetup: () -> Unit,
    onOpenQuiz: (quizId: String) -> Unit,
    viewModel: TimetableViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val errorMessage = state.errorMessage
    val settings = state.settings

    var openSessionId by remember { mutableStateOf<String?>(null) }
    var movingSession by remember { mutableStateOf<StudySession?>(null) }
    var showOptions by remember { mutableStateOf(false) }
    var showAdd by remember { mutableStateOf(false) }
    var generateRequested by remember { mutableStateOf(false) }

    // Loads when the screen opens, and again after coming back from the setup or a resource
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.load() }
    // When the student leaves the Timetable, refresh the reminders (does nothing if reminders are off)
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { ReminderSyncWorker.syncNow(context) }

    // Short messages ("Moved to Tuesday...") appear once as a toast
    LaunchedEffect(state.actionMessage) {
        state.actionMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearMessage()
        }
    }

    // Close the plan options after a successful Regenerate
    LaunchedEffect(state.isGenerating) {
        if (generateRequested && !state.isGenerating) {
            generateRequested = false
            if (state.generateError == null) showOptions = false
        }
    }

    val onGenerate = {
        generateRequested = true
        viewModel.generate()
    }

    Scaffold(
        topBar = {
            NenaTopBar(
                title = "Timetable",
                onBack = onBack,
                actions = {
                    if (settings?.hasPlan == true) {
                        IconButton(onClick = { showAdd = true }) {
                            Icon(Icons.Filled.Add, contentDescription = "Add my own session")
                        }
                        IconButton(onClick = { showOptions = true }) {
                            Icon(Icons.Filled.Settings, contentDescription = "Plan options")
                        }
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        val modifier = Modifier.padding(innerPadding)
        when {
            state.isLoading -> LoadingView(modifier)

            errorMessage != null && settings == null -> ErrorView(
                message = errorMessage,
                onRetry = viewModel::load,
                modifier = modifier
            )

            settings == null -> EmptyView(
                message = "You do not have a study plan yet. Answer a few questions and " +
                        "Nena Sayura will build a timetable around your subjects and free time.",
                modifier = modifier,
                actionLabel = "Create my plan",
                onAction = onSetup
            )

            // Settings saved, but no timetable generated yet
            !settings.hasPlan -> PlanOptionsContent(
                settings = settings,
                subjectNames = state.subjectNames,
                isGenerating = state.isGenerating,
                error = state.generateError,
                onGenerate = onGenerate,
                onEdit = onSetup,
                modifier = modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            )

            else -> TimetableBody(
                state = state,
                onViewMode = viewModel::setViewMode,
                onShiftWeek = viewModel::shiftWeek,
                onToday = viewModel::goToToday,
                onSelectDate = viewModel::selectDate,
                onOpenDay = { date ->
                    viewModel.selectDate(date)
                    viewModel.setViewMode(TimetableView.DAY)
                },
                onOpenSession = { openSessionId = it.id },
                onToggleDone = { session ->
                    val next = if (session.status == SessionStatus.DONE) SessionStatus.PLANNED else SessionStatus.DONE
                    viewModel.setStatus(session, next)
                },
                onOpenResource = { session -> openSessionResource(context, session, onOpenQuiz) },
                onAddSession = { showAdd = true },
                modifier = modifier
            )
        }
    }

    // ---------- sheets and dialogs ----------

    val openSession = state.weekSessions.firstOrNull { it.id == openSessionId }
    if (openSession != null) {
        SessionSheet(
            session = openSession,
            onDismiss = { openSessionId = null },
            onOpenResource = { openSessionResource(context, openSession, onOpenQuiz) },
            onSetStatus = { status ->
                viewModel.setStatus(openSession, status)
                openSessionId = null
            },
            onMove = {
                movingSession = openSession
                openSessionId = null
            },
            onDelete = {
                viewModel.deleteSession(openSession)
                openSessionId = null
            }
        )
    }

    movingSession?.let { session ->
        val sessionDate = LocalDate.parse(session.date)
        PlannerDatePickerDialog(
            initialDate = if (sessionDate.isBefore(LocalDate.now())) LocalDate.now() else sessionDate,
            onDismiss = { movingSession = null },
            onDateSelected = { date ->
                viewModel.moveSession(session, date)
                movingSession = null
            }
        )
    }

    if (showOptions && settings != null) {
        PlanOptionsSheet(
            settings = settings,
            subjectNames = state.subjectNames,
            isGenerating = state.isGenerating,
            error = state.generateError,
            onGenerate = onGenerate,
            onEdit = {
                showOptions = false
                onSetup()
            },
            onDismiss = { showOptions = false }
        )
    }

    if (showAdd && settings != null) {
        AddSessionDialog(
            subjects = state.subjects,
            initialDate = state.selectedDate,
            onDismiss = { showAdd = false },
            onAdd = { title, subject, date, minutes ->
                viewModel.addCustomSession(title, subject, date, minutes)
                showAdd = false
            }
        )
    }
}

@Composable
private fun TimetableBody(
    state: TimetableUiState,
    onViewMode: (TimetableView) -> Unit,
    onShiftWeek: (Int) -> Unit,
    onToday: () -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    onOpenDay: (LocalDate) -> Unit,
    onOpenSession: (StudySession) -> Unit,
    onToggleDone: (StudySession) -> Unit,
    onOpenResource: (StudySession) -> Unit,
    onAddSession: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        ViewToggle(mode = state.viewMode, onChange = onViewMode)
        WeekNavigator(
            days = state.weekDays,
            showToday = state.selectedDate != LocalDate.now(),
            onPrevious = { onShiftWeek(-1) },
            onNext = { onShiftWeek(1) },
            onToday = onToday
        )
        WeekProgress(
            done = state.doneCount,
            missed = state.missedCount,
            total = state.weekSessions.size
        )

        when {
            state.isWeekLoading -> LoadingView()

            state.viewMode == TimetableView.DAY -> DayView(
                state = state,
                onSelectDate = onSelectDate,
                onOpenSession = onOpenSession,
                onToggleDone = onToggleDone,
                onOpenResource = onOpenResource,
                onAddSession = onAddSession,
                modifier = Modifier.weight(1f)
            )

            else -> WeekView(
                state = state,
                onOpenDay = onOpenDay,
                onOpenSession = onOpenSession,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ViewToggle(mode: TimetableView, onChange: (TimetableView) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        FilterChip(
            selected = mode == TimetableView.DAY,
            onClick = { onChange(TimetableView.DAY) },
            label = { Text("Day") }
        )
        Spacer(modifier = Modifier.padding(horizontal = 4.dp))
        FilterChip(
            selected = mode == TimetableView.WEEK,
            onClick = { onChange(TimetableView.WEEK) },
            label = { Text("Week") }
        )
    }
}

@Composable
private fun WeekNavigator(
    days: List<LocalDate>,
    showToday: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous week")
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${formatShortDate(days.first())} - ${formatShortDate(days.last())}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            if (showToday) {
                TextButton(onClick = onToday) { Text("Go to today") }
            } else {
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
        IconButton(onClick = onNext) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next week")
        }
    }
}

@Composable
private fun WeekProgress(done: Int, missed: Int, total: Int) {
    if (total == 0) return
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        Text(
            text = buildString {
                append("This week: $done of $total done")
                if (missed > 0) append("  •  $missed missed")
            },
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { done.toFloat() / total },
            modifier = Modifier.fillMaxWidth()
        )
    }
}