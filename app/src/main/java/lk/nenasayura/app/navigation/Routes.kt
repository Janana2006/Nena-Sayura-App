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
    // Student flow: Marking Schemes -> choose stream -> choose subjects -> categories
    const val MARKING_SCHEME_STREAMS = "marking_scheme_streams"
    const val MARKING_SCHEME_SUBJECTS = "marking_scheme_subjects/{streamId}"
    fun markingSchemeSubjectsRoute(streamId: String) = "marking_scheme_subjects/$streamId"
    const val MARKING_SCHEMES_LIST = "marking_schemes/{category}"
    fun markingSchemesRoute(category: String) = "marking_schemes/$category"

    const val GUIDE_BOOKS = "guide_books"
    // Student flow: Guide Books -> choose stream -> category -> subject -> books
    const val GUIDE_BOOK_STREAMS = "guide_book_streams"
    const val GUIDE_BOOK_SUBJECTS = "guide_book_subjects/{category}"
    fun guideBookSubjectsRoute(category: String) = "guide_book_subjects/$category"
    // subjectId is optional: guests open the list without it
    const val GUIDE_BOOKS_LIST = "guide_books/{category}?subjectId={subjectId}"
    fun guideBooksRoute(category: String, subjectId: String? = null) =
        if (subjectId.isNullOrBlank()) "guide_books/$category"
        else "guide_books/$category?subjectId=$subjectId"

    const val QUIZZES = "quizzes"
    // Student flow: Quizzes -> choose stream -> choose subject -> quizzes of that subject
    const val QUIZ_STREAMS = "quiz_streams"
    const val QUIZZES_BY_SUBJECT = "quizzes/subject/{subjectId}"
    fun quizzesBySubjectRoute(subjectId: String) = "quizzes/subject/$subjectId"
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