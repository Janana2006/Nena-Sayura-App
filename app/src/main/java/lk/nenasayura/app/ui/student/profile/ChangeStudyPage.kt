package lk.nenasayura.app.ui.student.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import lk.nenasayura.app.core.browse.PaperBrowseSelection
import lk.nenasayura.app.core.browse.StudentStudySync
import lk.nenasayura.app.screens.SetupScreen
import lk.nenasayura.app.viewmodel.SetupViewModel

/**
 * Opened by the "Change" button on Papers, Marking Schemes and Guide Books.
 *
 * It uses the same creative screen as the first setup (stream cards, then Subject 1/2/3),
 * with the student's current stream and subjects already ticked. The new choice is saved
 * in the profile, so it is used everywhere and stays after the app is closed.
 */
@Composable
fun ChangeStudyPage(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: SetupViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // The current choice, read once when this page opens (empty if nothing was chosen yet)
    val currentStreamId = remember { PaperBrowseSelection.state.value.streamId }
    val currentSubjectIds = remember {
        PaperBrowseSelection.state.value.subjects.map { it.id }
    }

    // When the streams are loaded, tick the student's current stream and subjects
    var preselected by remember { mutableStateOf(false) }
    LaunchedEffect(state.loading, state.streams) {
        if (!preselected && !state.loading && state.streams.isNotEmpty()) {
            preselected = true
            if (state.streams.any { it.id == currentStreamId }) {
                viewModel.selectStream(currentStreamId)
                currentSubjectIds.forEach { viewModel.toggleSubject(it) }
            }
        }
    }

    SetupScreen(
        modifier = Modifier,
        viewModel = viewModel,
        role = "student",
        onDone = {
            // The new choice is saved: drop the old one so every screen reloads it
            StudentStudySync.onStudyChanged()
            onSaved()
        },
        onLogout = onBack,            // the bottom button works as "Cancel" here
        bottomButtonLabel = "Cancel",
        blockBack = false             // the phone's back button can leave this page
    )
}