package lk.nenasayura.app.ui.admin

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import lk.nenasayura.app.data.model.AdminQuiz
import lk.nenasayura.app.data.model.NewQuestion
import lk.nenasayura.app.data.model.StudyStream
import lk.nenasayura.app.data.model.Subject
import lk.nenasayura.app.ui.theme.NenaSecondary

// What the admin is doing: making a new quiz, or adding questions to one that exists
private sealed interface BuilderMode {
    data object New : BuilderMode
    data class AddTo(val quiz: AdminQuiz) : BuilderMode
}

// One question while it is being typed. [key] keeps each card stable in the list.
private data class QuestionDraft(
    val key: Int,
    val text: String = "",
    val options: List<String> = List(QUIZ_OPTION_COUNT) { "" },
    val correctIndex: Int = -1,
    val explanation: String = ""
)

/**
 * Quizzes: pick the stream and subject, type the lesson name, then add as many
 * questions as you like. Every question has 5 answers and a tick on the right one.
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

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Button(onClick = { builder = BuilderMode.New }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add quiz")
            }
        }

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
                ) { CircularProgressIndicator() }
            }
        } else if (ui.quizzes.isEmpty() && ui.loadError == null) {
            item {
                Text(
                    text = "No quizzes yet. Tap Add quiz to make the first one.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        items(ui.quizzes, key = { it.id }) { quiz ->
            AdminCard {
                Text(
                    text = quiz.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                val count = if (quiz.questionCount >= 0) {
                    "${quiz.questionCount} question${if (quiz.questionCount == 1) "" else "s"}"
                } else ""
                val sub = listOf(state.subjectName(quiz.subjectId), count)
                    .filter { it.isNotBlank() }
                    .joinToString(" • ")
                Text(
                    text = sub,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { builder = BuilderMode.AddTo(quiz) }) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add questions")
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