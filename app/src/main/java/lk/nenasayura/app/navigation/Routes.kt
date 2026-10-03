package lk.nenasayura.app.navigation

/** Navigation route names. Each task adds its own routes below. */
object Routes {
    // Provided by the authentication teammate. The Guest/Student modules only navigate to it.
    const val LOGIN = "login"

    // Home destinations (this module)
    const val GUEST_HOME = "guest_home"
    const val STUDENT_HOME = "student_home"

    // Public resources (available to guests and students)
    const val PAPERS = "papers"
    const val MARKING_SCHEMES = "marking_schemes"
    const val GUIDE_BOOKS = "guide_books"
    const val QUIZZES = "quizzes"
    const val QUIZ_PLAY = "quiz/{quizId}"
    fun quizRoute(quizId: String) = "quiz/$quizId"

    // Student-only screens (reached from Student Home)
    const val TEACHERS = "teachers"
    const val TIMETABLE = "timetable"
    const val REMINDERS = "reminders"
    const val ANALYTICS = "analytics"
    const val SEARCH = "search"
}