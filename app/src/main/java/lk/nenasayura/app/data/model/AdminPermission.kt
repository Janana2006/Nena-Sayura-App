package lk.nenasayura.app.data.model

/**
 * One thing an admin / staff member can be allowed to see in the Admin app.
 * [key] is what is saved in Firestore (users/{uid}.permissions), so never rename a key.
 */
enum class AdminPermission(val key: String, val label: String, val hint: String) {
    USERS("users", "Students & Teachers", "See all students and teachers"),
    PAYMENTS("payments", "Payments", "Earnings, commission and payment history"),
    PAPERS("papers", "Papers", "Add and delete papers"),
    MARKING_SCHEMES("marking_schemes", "Marking Schemes", "Marking schemes section"),
    GUIDE_BOOKS("guide_books", "Guide Books", "Add and delete guide books"),
    QUIZZES("quizzes", "Quizzes", "Quizzes section"),
    TEACHER_UPLOADS("teacher_uploads", "Teacher Uploads", "Review, approve or decline teacher uploads"),
    STREAMS("streams", "Streams & Subjects", "Add streams, subjects and kandas"),
    STAFF("staff", "Manage Staff", "See staff and add new staff members");

    companion object {
        /** Permissions that live inside the Content tab. */
        val CONTENT: Set<AdminPermission> = setOf(PAPERS, MARKING_SCHEMES, GUIDE_BOOKS, QUIZZES, TEACHER_UPLOADS, STREAMS)

        /** null (old admin accounts with no "permissions" field) = full access. */
        fun fromKeys(keys: Collection<String>?): Set<AdminPermission> =
            if (keys == null) entries.toSet() else entries.filter { it.key in keys }.toSet()
    }
}

/** Ready-made role. Choosing one just pre-ticks the access boxes. */
enum class StaffRole(val key: String, val label: String, val permissions: Set<AdminPermission>) {
    FULL_ADMIN("full_admin", "Full Admin", AdminPermission.entries.toSet()),
    CONTENT_MANAGER("content_manager", "Content Manager", AdminPermission.CONTENT),
    FINANCE_MANAGER("finance_manager", "Finance Manager", setOf(AdminPermission.PAYMENTS, AdminPermission.USERS)),
    USER_SUPPORT("user_support", "User Support", setOf(AdminPermission.USERS)),
    CUSTOM("custom", "Custom", emptySet());

    companion object {
        fun fromKey(key: String?): StaffRole? = entries.firstOrNull { it.key == key }
    }
}