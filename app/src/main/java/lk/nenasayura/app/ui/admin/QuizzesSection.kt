package lk.nenasayura.app.ui.admin

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import lk.nenasayura.app.data.model.AdminQuiz
import lk.nenasayura.app.data.model.NewQuestion
import lk.nenasayura.app.data.model.StudyStream
import lk.nenasayura.app.data.model.Subject
import lk.nenasayura.app.ui.theme.NenaBackground
import lk.nenasayura.app.ui.theme.NenaPrimary
import lk.nenasayura.app.ui.theme.NenaSecondary

private sealed interface BuilderMode {
    data object New : BuilderMode
    data class AddTo(val quiz: AdminQuiz) : BuilderMode
}

private data class QuestionDraft(
    val key: Int,
    val text: String = "",
    val options: List<String> = List(QUIZ_OPTION_COUNT) { "" },
    val correctIndex: Int = -1,
    val explanation: String = ""
)

/**
 * Quizzes: filter by stream & subject, view and search quizzes, add new ones with questions.
 */
@Composable
fun QuizzesSection(
    state: AdminUiState,
    quizVm: QuizAdminViewModel = viewModel()
) {
    val ui by quizVm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var builder by remember { mutableStateOf<BuilderMode?>(null) }
    var deleting by remember { mutableStateOf<AdminQuiz?>(null) }

    var selectedStream by remember { mutableStateOf<StudyStream?>(null) }
    var selectedSubject by remember { mutableStateOf<Subject?>(null) }
    var query by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { quizVm.loadOnce() }

    val mode = builder
    if (mode != null) {
        QuizBuilder(
            mode = mode,
            streams = state.streams,
            busy = ui.busy,
            quizVm = quizVm,
            onClose = { builder = null },
            onSaved = { message ->
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                builder = null
            }
        )
        return
    }

    // Helper functions
    fun streamForSubject(subjectId: String): StudyStream? {
        return state.streams.firstOrNull { stream -> stream.subjects.any { it.id == subjectId } }
    }

    fun countForStream(stream: StudyStream?): Int {
        if (stream == null) return ui.quizzes.size
        val subjectIds = stream.subjects.map { it.id }.toSet()
        return ui.quizzes.count { it.subjectId in subjectIds }
    }

    fun countForSubject(subject: Subject?): Int {
        if (subject == null) return countForStream(selectedStream)
        return ui.quizzes.count { it.subjectId == subject.id }
    }

    val streamFilteredQuizzes = if (selectedStream == null) {
        ui.quizzes
    } else {
        val streamSubjectIds = selectedStream!!.subjects.map { it.id }.toSet()
        ui.quizzes.filter { it.subjectId in streamSubjectIds }
    }

    val subjectFilteredQuizzes = if (selectedSubject == null) {
        streamFilteredQuizzes
    } else {
        streamFilteredQuizzes.filter { it.subjectId == selectedSubject!!.id }
    }

    val shown = subjectFilteredQuizzes.filter { quiz ->
        query.isBlank() ||
                quiz.title.contains(query, ignoreCase = true) ||
                quiz.description.contains(query, ignoreCase = true) ||
                state.subjectName(quiz.subjectId).contains(query, ignoreCase = true)
    }

    Column(modifier = Modifier.fillMaxSize().background(NenaBackground)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ---- Add Quiz Button ----
            item {
                Button(
                    onClick = { builder = BuilderMode.New },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Quiz", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            // ---- Streams Horizontal Filter Bar ----
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Filter by Stream",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B)
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            val isSelected = selectedStream == null
                            val count = ui.quizzes.size
                            FilterPill(
                                label = "All Streams",
                                count = count,
                                isSelected = isSelected,
                                onClick = {
                                    selectedStream = null
                                    selectedSubject = null
                                }
                            )
                        }
                        items(state.streams, key = { it.id }) { stream ->
                            val isSelected = selectedStream?.id == stream.id
                            val count = countForStream(stream)
                            FilterPill(
                                label = stream.name,
                                count = count,
                                isSelected = isSelected,
                                onClick = {
                                    if (selectedStream?.id == stream.id) {
                                        selectedStream = null
                                        selectedSubject = null
                                    } else {
                                        selectedStream = stream
                                        selectedSubject = null
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // ---- Subjects Horizontal Filter Bar (when a Stream is selected) ----
            if (selectedStream != null && selectedStream!!.subjects.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Subjects in ${selectedStream!!.name}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = NenaPrimary
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                val isSelected = selectedSubject == null
                                val count = countForStream(selectedStream)
                                FilterPill(
                                    label = "All Subjects",
                                    count = count,
                                    isSelected = isSelected,
                                    accent = true,
                                    onClick = { selectedSubject = null }
                                )
                            }
                            items(selectedStream!!.subjects, key = { it.id }) { subject ->
                                val isSelected = selectedSubject?.id == subject.id
                                val count = countForSubject(subject)
                                FilterPill(
                                    label = subject.name,
                                    count = count,
                                    isSelected = isSelected,
                                    accent = true,
                                    onClick = {
                                        selectedSubject = if (selectedSubject?.id == subject.id) null else subject
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // ---- Search Box ----
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = {
                        Text("Search quiz by title or lesson...", fontSize = 14.sp, color = Color(0xFF94A3B8))
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = NenaPrimary)
                    },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color(0xFF64748B))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = NenaPrimary,
                        unfocusedBorderColor = Color(0xFFE2E8F0)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // ---- Load Error or Spinner ----
            ui.loadError?.let { message ->
                item {
                    Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }
            }

            if (ui.loading && ui.quizzes.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator(color = NenaPrimary) }
                }
            } else if (shown.isEmpty() && ui.loadError == null) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEFF6FF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Quiz, contentDescription = null, tint = NenaPrimary, modifier = Modifier.size(30.dp))
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (query.isNotBlank()) "No quizzes match \"$query\"" else "No quizzes found in this selection.",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF334155),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tap Add quiz above to create a new quiz.",
                                fontSize = 13.sp,
                                color = Color(0xFF64748B),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(shown, key = { it.id }) { quiz ->
                    val stream = streamForSubject(quiz.subjectId)
                    val subjectName = state.subjectName(quiz.subjectId)

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        border = BorderStroke(1.dp, Color(0xFFF1F5F9))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Top Row: Stream/Subject Pill + Question Count Badge
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFE0F2FE)
                                ) {
                                    Text(
                                        text = listOfNotNull(stream?.name, subjectName).filter { it.isNotBlank() }.joinToString(" • "),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NenaPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                val qCount = if (quiz.questionCount >= 0) quiz.questionCount else 0
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFE6F4EA)
                                ) {
                                    Text(
                                        text = "$qCount Question${if (qCount == 1) "" else "s"}",
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF137333)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = quiz.title.ifBlank { "Untitled Quiz" },
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            if (quiz.description.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = quiz.description,
                                    fontSize = 13.sp,
                                    color = Color(0xFF64748B),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Bottom Actions: Add Questions + Delete
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = { builder = BuilderMode.AddTo(quiz) },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Add Questions", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                }

                                IconButton(onClick = { deleting = quiz }) {
                                    Icon(
                                        Icons.Filled.Delete,
                                        contentDescription = "Delete quiz",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    deleting?.let { quiz ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete this quiz?") },
            text = { Text("\"${quiz.title}\" and all its questions will be removed.") },
            confirmButton = {
                TextButton(onClick = {
                    quizVm.deleteQuiz(quiz) { err ->
                        Toast.makeText(context, err ?: "Quiz deleted", Toast.LENGTH_SHORT).show()
                    }
                    deleting = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun QuizBuilder(
    mode: BuilderMode,
    streams: List<StudyStream>,
    busy: Boolean,
    quizVm: QuizAdminViewModel,
    onClose: () -> Unit,
    onSaved: (String) -> Unit
) {
    var stream by remember { mutableStateOf<StudyStream?>(null) }
    var subject by remember { mutableStateOf<Subject?>(null) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var nextKey by remember { mutableIntStateOf(1) }
    val drafts = remember { mutableStateListOf(QuestionDraft(key = 0)) }
    var error by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ---- Quiz details ----
        item {
            AdminCard {
                if (mode is BuilderMode.AddTo) {
                    Text(
                        text = "Adding questions to",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = mode.quiz.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                } else {
                    Text(
                        text = "Quiz details",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Stream", style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.height(6.dp))
                    ChoiceDropdown(
                        items = streams,
                        selected = stream,
                        placeholder = if (streams.isEmpty()) "No streams yet. Add one in Streams." else "Select stream",
                        enabled = !busy,
                        label = { it.name },
                        onSelect = {
                            stream = it
                            subject = null
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Subject", style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.height(6.dp))
                    ChoiceDropdown(
                        items = stream?.subjects ?: emptyList(),
                        selected = subject,
                        placeholder = if (stream == null) "Choose a stream first" else "Select subject",
                        enabled = !busy && stream != null,
                        label = { it.name },
                        onSelect = { subject = it }
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Lesson name (for example Mechanics)") },
                        singleLine = true,
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description (optional)") },
                        minLines = 2,
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // ---- Questions ----
        itemsIndexed(drafts, key = { _, d -> d.key }) { index, draft ->
            QuestionCard(
                number = index + 1,
                draft = draft,
                enabled = !busy,
                canRemove = drafts.size > 1,
                onChange = { updated ->
                    val i = drafts.indexOfFirst { it.key == draft.key }
                    if (i >= 0) drafts[i] = updated
                },
                onRemove = { drafts.removeAll { it.key == draft.key } }
            )
        }

        item {
            OutlinedButton(
                onClick = {
                    drafts.add(QuestionDraft(key = nextKey))
                    nextKey++
                },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add question")
            }
        }

        item {
            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(12.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onClose,
                    enabled = !busy,
                    modifier = Modifier.weight(1f)
                ) { Text("Cancel") }

                Button(
                    onClick = {
                        error = null
                        val questions = drafts.map {
                            NewQuestion(it.text, it.options, it.correctIndex, it.explanation)
                        }
                        val done: (String?) -> Unit = { err ->
                            if (err == null) {
                                onSaved(if (mode is BuilderMode.AddTo) "Questions added" else "Quiz saved")
                            } else {
                                error = err
                            }
                        }
                        when (mode) {
                            is BuilderMode.AddTo -> quizVm.addQuestions(mode.quiz, questions, done)
                            BuilderMode.New -> quizVm.createQuiz(subject, title, description, questions, done)
                        }
                    },
                    enabled = !busy,
                    modifier = Modifier.weight(1f)
                ) {
                    if (busy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Text("Save quiz")
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun QuestionCard(
    number: Int,
    draft: QuestionDraft,
    enabled: Boolean,
    canRemove: Boolean,
    onChange: (QuestionDraft) -> Unit,
    onRemove: () -> Unit
) {
    AdminCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Question $number",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            if (canRemove) {
                IconButton(onClick = onRemove, enabled = enabled) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Remove question",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = draft.text,
            onValueChange = { onChange(draft.copy(text = it)) },
            label = { Text("Question") },
            minLines = 2,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Answers: tap the tick next to the correct one",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))

        draft.options.forEachIndexed { i, option ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { onChange(draft.copy(correctIndex = i)) },
                    enabled = enabled
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "Correct answer",
                        tint = if (draft.correctIndex == i) {
                            NenaSecondary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        }
                    )
                }
                OutlinedTextField(
                    value = option,
                    onValueChange = { value ->
                        val updated = draft.options.toMutableList()
                        updated[i] = value
                        onChange(draft.copy(options = updated))
                    },
                    label = { Text("Answer ${'A' + i}") },
                    singleLine = true,
                    enabled = enabled,
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = draft.explanation,
            onValueChange = { onChange(draft.copy(explanation = it)) },
            label = { Text("Explanation (optional)") },
            supportingText = { Text("Shown to the student after they answer") },
            minLines = 2,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun FilterPill(
    label: String,
    count: Int,
    isSelected: Boolean,
    accent: Boolean = false,
    onClick: () -> Unit
) {
    val activeBg = if (accent) NenaSecondary else NenaPrimary
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) activeBg else Color.White,
        label = "pillBg"
    )
    val textColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else Color(0xFF475569),
        label = "pillText"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = bgColor,
        border = BorderStroke(
            width = 1.dp,
            color = if (isSelected) activeBg else Color(0xFFE2E8F0)
        ),
        shadowElevation = if (isSelected) 2.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = textColor
            )
            Spacer(modifier = Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(
                        if (isSelected) Color.White.copy(alpha = 0.25f)
                        else Color(0xFFF1F5F9)
                    )
                    .padding(horizontal = 7.dp, vertical = 2.dp)
            ) {
                Text(
                    text = count.toString(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) Color.White else Color(0xFF64748B)
                )
            }
        }
    }
}

/** A drop-down for any list (streams, subjects ...). */
@Composable
private fun <T> ChoiceDropdown(
    items: List<T>,
    selected: T?,
    placeholder: String,
    enabled: Boolean,
    label: (T) -> String,
    onSelect: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Surface(
            onClick = { expanded = true },
            enabled = enabled,
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (selected != null) label(selected) else placeholder,
                    color = if (selected != null) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
            }
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(label(item)) },
                    onClick = {
                        onSelect(item)
                        expanded = false
                    }
                )
            }
        }
    }
}