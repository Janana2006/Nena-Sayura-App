package lk.nenasayura.app.data.model

/** A student's profile, read from the Firestore "users" document (role = "student"). */
data class StudentProfile(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val school: String = "",
    val district: String = "",
    /** The year the student sits the A/L exam. 0 = not set. */
    val examYear: Int = 0,
    val streamId: String = "",
    val streamName: String = "",
    val subjectIds: List<String> = emptyList(),
    val subjectNames: List<String> = emptyList(),
    /** True when the student signs in with Google. Email and password are then managed by Google. */
    val isGoogleAccount: Boolean = false,
    /** Link of the profile photo (Cloudinary). Empty = no photo, the first letter of the name shows. */
    val photoUrl: String = ""
) {
    val hasStudyDetails: Boolean
        get() = streamName.isNotBlank() && subjectNames.isNotEmpty()
}