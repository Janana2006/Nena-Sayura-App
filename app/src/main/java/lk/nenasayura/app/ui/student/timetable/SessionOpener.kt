package lk.nenasayura.app.ui.student.timetable



import android.content.Context
import android.widget.Toast
import lk.nenasayura.app.data.model.ResourceTypes
import lk.nenasayura.app.data.model.StudySession
import lk.nenasayura.app.util.FileActions

/** Opens the resource a study session points to. Quizzes open through [onOpenQuiz]. */
fun openSessionResource(context: Context, session: StudySession, onOpenQuiz: (quizId: String) -> Unit) {
    when (session.resourceType) {
        ResourceTypes.QUIZ -> onOpenQuiz(session.resourceId)
        ResourceTypes.TEACHER_VIDEO -> FileActions.openExternally(context, session.resourceUrl)
        ResourceTypes.PAPER,
        ResourceTypes.MARKING_SCHEME,
        ResourceTypes.GUIDE_BOOK,
        ResourceTypes.TEACHER_FILE -> FileActions.openFile(context, session.resourceUrl, session.resourceTitle)
        else -> Toast.makeText(
            context,
            "No resource has been added for this session yet.",
            Toast.LENGTH_SHORT
        ).show()
    }
}