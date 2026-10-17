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
    const val PAPERS_LIST = "papers/{category}"
    fun papersRoute(category: String) = "papers/$category"
    const val MARKING_SCHEMES = "marking_schemes"
    const val MARKING_SCHEMES_LIST = "marking_schemes/{category}"
    fun markingSchemesRoute(category: String) = "marking_schemes/$category"

    const val GUIDE_BOOKS = "guide_books"
    // Student flow: Guide Books -> category -> subject (of the student's own stream) -> books
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
    // My teacher: teacher list -> the teacher's materials -> payment
    const val TEACHER_MATERIALS = "teachers/{teacherId}"
    fun teacherMaterialsRoute(teacherId: String) = "teachers/$teacherId"
    const val TEACHER_PAYMENT = "teachers/{teacherId}/pay"
    fun teacherPaymentRoute(teacherId: String) = "teachers/$teacherId/pay"
    const val TIMETABLE = "timetable"
    const val TIMETABLE_SETUP = "timetable_setup"
    const val REMINDERS = "reminders"
    const val DOWNLOADS = "downloads"
    const val ANALYTICS = "analytics"
    const val QUICK_ACCESS = "quick_access"
    const val REVIEWS = "reviews"
    const val WRITE_REVIEW = "write_review"
    const val SEARCH = "search"

    // "Change" button on Papers / Marking Schemes / Guide Books: change the stream + subjects
    const val STUDY_EDIT = "study_edit"

    // Guests: Papers / Marking Schemes / Guide Books -> choose stream -> choose subjects -> categories.
    // [target] is "papers", "marking_schemes" or "guide_books" (the same as the route names above).
    const val GUEST_STREAMS = "guest_streams/{target}"
    fun guestStreamsRoute(target: String) = "guest_streams/$target"
    const val GUEST_SUBJECTS = "guest_subjects/{target}/{streamId}"
    fun guestSubjectsRoute(target: String, streamId: String) = "guest_subjects/$target/$streamId"
}