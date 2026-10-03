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
    const val MARKING_SCHEMES = "marking_schemes"
    const val GUIDE_BOOKS = "guide_books"
    const val QUIZZES = "quizzes"
    const val SEARCH = "search"
}