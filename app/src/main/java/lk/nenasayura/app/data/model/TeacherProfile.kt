package lk.nenasayura.app.data.model

/**
 * A teacher's profile, read from the Firestore "users" document (role = "teacher").
 * name, email, role, isFree, fee, streamId/Name and subjectIds/Names already exist
 * (written by the Register and Setup screens). bio and phone are added by the Teacher module.
 */
data class TeacherProfile(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val bio: String = "",
    val phone: String = "",
    val streamId: String = "",
    val streamName: String = "",
    val subjectIds: List<String> = emptyList(),
    val subjectNames: List<String> = emptyList(),
    /** true = the teacher's service is free. false = students pay [fee]. */
    val isFree: Boolean = true,
    val fee: Int = 0,
    /** Where the admin sends the teacher's earnings. */
    val bankName: String = "",
    val accountNumber: String = "",
    val bankBranch: String = "",
    /** Profile photo link (Cloudinary). Empty = the first letter of the name shows. */
    val photoUrl: String = ""
) {
    val hasBankDetails: Boolean
        get() = bankName.isNotBlank() && accountNumber.isNotBlank() && bankBranch.isNotBlank()

    /** The subjects as Subject objects (ids and names are saved as two parallel lists). */
    val subjects: List<Subject>
        get() = subjectIds.mapIndexed { i, id ->
            Subject(id = id, name = subjectNames.getOrNull(i) ?: id, stream = streamId)
        }
}