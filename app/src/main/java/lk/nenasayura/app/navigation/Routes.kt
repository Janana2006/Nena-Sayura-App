package lk.nenasayura.app.navigation

/** Navigation route names. Each task adds its own routes below. */
object Routes {
    // Provided by the authentication teammate. The Guest/Student modules only navigate to it.
    const val LOGIN = "login"

    // Home destinations (this module)
    const val GUEST_HOME = "guest_home"
    const val STUDENT_HOME = "student_home"
    const val TEACHER_HOME = "teacher_home"

    // Public resources (available to guests and students)
    const val PAPERS = "papers"
    // Student flow: Papers -> choose stream -> choose subjects -> categories
    const val PAPER_STREAMS = "paper_streams"
    const val PAPER_SUBJECTS = "paper_subjects/{streamId}"
    fun paperSubjectsRoute(streamId: String) = "paper_subjects/$streamId"
    const val PAPERS_LIST = "papers/{category}"
    fun papersRoute(category: String) = "papers/$category"
    const val MARKING_SCHEMES = "marking_schemes"
    const val MARKING_SCHEMES_LIST = "marking_schemes/{category}"
    fun markingSchemesRoute(category: String) = "marking_schemes/$category"
    const val GUIDE_BOOKS = "guide_books"
    const val GUIDE_BOOKS_LIST = "guide_books/{category}"
    fun guideBooksRoute(category: String) = "guide_books/$category"
    // Students: Guide Books -> stream -> category -> subjects -> books of that subject
    const val GUIDE_BOOKS_SUBJECT = "guide_books/{category}/{subjectId}"
    fun guideBooksSubjectRoute(category: String, subjectId: String) =
        "guide_books/$category/$subjectId"
    const val QUIZZES = "quizzes"
    const val QUIZ_PLAY = "quiz/{quizId}"
    fun quizRoute(quizId: String) = "quiz/$quizId"

    // Student-only screens (reached from Student Home)
    const val TEACHERS = "teachers"
    const val TIMETABLE = "timetable"
    const val REMINDERS = "reminders"
    const val ANALYTICS = "analytics"
    const val QUICK_ACCESS = "quick_access"
    const val REVIEWS = "reviews"
    const val WRITE_REVIEW = "write_review"
    const val SEARCH = "search"
}