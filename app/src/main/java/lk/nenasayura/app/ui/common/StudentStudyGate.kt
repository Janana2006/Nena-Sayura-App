package lk.nenasayura.app.ui.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.CancellationException
import lk.nenasayura.app.core.browse.GuideBrowseSelection
import lk.nenasayura.app.core.browse.PaperBrowseSelection
import lk.nenasayura.app.core.browse.StudentStudySync

private enum class GateStatus { Loading, NoStudy, Error }

/**
 * Wraps the student screens of Papers, Marking Schemes and Guide Books.
 *
 * It makes sure the student's saved stream + subjects are loaded before [content] is shown.
 * If the student has no subjects yet, or the profile cannot be loaded, it shows a message
 * with a button instead.
 */
@Composable
fun StudentStudyGate(
    title: String,
    onBack: () -> Unit,
    onChange: () -> Unit,
    content: @Composable () -> Unit
) {
    val papers by PaperBrowseSelection.state.collectAsStateWithLifecycle()
    val guides by GuideBrowseSelection.state.collectAsStateWithLifecycle()
    val ready = papers.hasSelection && guides.hasSelection

    var status by remember { mutableStateOf(GateStatus.Loading) }
    var attempt by remember { mutableIntStateOf(0) }

    // Runs when the screen opens, after "Retry", and again when the choice was cleared (after a change)
    LaunchedEffect(ready, attempt) {
        if (ready) return@LaunchedEffect
        status = GateStatus.Loading
        status = try {
            if (StudentStudySync.load()) GateStatus.Loading else GateStatus.NoStudy
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            GateStatus.Error
        }
    }

    if (ready) {
        content()
        return
    }

    Scaffold(
        topBar = { NenaTopBar(title = title, onBack = onBack) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        val modifier = Modifier.padding(innerPadding)
        when (status) {
            GateStatus.Loading -> LoadingView(modifier)

            GateStatus.NoStudy -> EmptyView(
                message = "You have not chosen your stream and subjects yet.",
                modifier = modifier,
                actionLabel = "Choose subjects",
                onAction = onChange
            )

            GateStatus.Error -> ErrorView(
                message = "Could not load your subjects. Check your internet connection and try again.",
                onRetry = { attempt++ },
                modifier = modifier
            )
        }
    }
}