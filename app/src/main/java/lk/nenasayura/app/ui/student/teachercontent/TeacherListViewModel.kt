package lk.nenasayura.app.ui.student.teachercontent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import lk.nenasayura.app.data.model.TeacherProfile
import lk.nenasayura.app.data.repository.PaymentRepository
import lk.nenasayura.app.data.repository.TeacherContentRepository
import lk.nenasayura.app.data.repository.TeacherProfileRepository

/** A teacher the student must pay: not free AND has a fee. */
val TeacherProfile.isPaidTeacher: Boolean
    get() = !isFree && fee > 0

/** 1500 -> "Rs. 1,500" */
fun formatFee(fee: Int): String = "Rs. " + "%,d".format(fee)

data class TeacherListUiState(
    val loading: Boolean = true,
    val errorMessage: String? = null,
    val teachers: List<TeacherProfile> = emptyList(),
    /** Teachers this student has already paid. */
    val paidTeacherIds: Set<String> = emptySet(),
    /** teacherId -> number of materials. Only teachers with at least one are listed. */
    val materialCounts: Map<String, Int> = emptyMap()
) {
    /** Free teachers are shown first. */
    val freeTeachers: List<TeacherProfile>
        get() = teachers.filter { !it.isPaidTeacher }.sortedBy { it.name.lowercase() }

    val paidTeachers: List<TeacherProfile>
        get() = teachers.filter { it.isPaidTeacher }.sortedBy { it.name.lowercase() }
}

/** Loads every teacher for the student's "My teacher" list. */
class TeacherListViewModel(
    private val teacherRepository: TeacherProfileRepository = TeacherProfileRepository(),
    private val paymentRepository: PaymentRepository = PaymentRepository(),
    private val contentRepository: TeacherContentRepository = TeacherContentRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(TeacherListUiState())
    val state: StateFlow<TeacherListUiState> = _state.asStateFlow()

    fun load() {
        _state.update { it.copy(loading = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                val uid = FirebaseAuth.getInstance().currentUser?.uid

                // The list is built from the materials themselves: a teacher is listed when he or she
                // has at least one live upload. Each teacher's profile is then read one by one, so a
                // teacher is found whatever role the account has open right now (an account that is
                // both student and teacher included).
                val contents = contentRepository.getVisibleContents()
                val counts = contents.groupingBy { it.teacherId }.eachCount()
                val teachers = coroutineScope {
                    contents.groupBy { it.teacherId }
                        .filterKeys { it.isNotBlank() }
                        .map { (teacherId, items) ->
                            async {
                                val profile = teacherRepository.getProfile(
                                    teacherId,
                                    items.first().teacherName,
                                    ""
                                )
                                // the subjects of the materials count as the teacher's subjects too
                                val subjects = profile.subjectIds
                                    .mapIndexed { i, id -> id to (profile.subjectNames.getOrNull(i) ?: id) }
                                    .plus(
                                        items.filter { it.subjectId.isNotBlank() }
                                            .map { it.subjectId to it.subjectName }
                                    )
                                    .distinctBy { it.first }
                                profile.copy(
                                    name = profile.name.ifBlank { items.first().teacherName },
                                    subjectIds = subjects.map { it.first },
                                    subjectNames = subjects.map { it.second }
                                )
                            }
                        }
                        .awaitAll()
                }
                // A student who is also a teacher can open their own materials without paying
                val paid = if (uid != null) {
                    paymentRepository.getPaidTeacherIds(uid) + uid
                } else {
                    emptySet()
                }
                _state.update {
                    it.copy(
                        loading = false,
                        teachers = teachers,
                        paidTeacherIds = paid,
                        materialCounts = counts
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(loading = false, errorMessage = "Could not load teachers: ${e.localizedMessage}")
                }
            }
        }
    }
}