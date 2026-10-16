package lk.nenasayura.app.ui.student.teachercontent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import lk.nenasayura.app.data.model.TeacherProfile
import lk.nenasayura.app.data.repository.PaymentRepository
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
    val paidTeacherIds: Set<String> = emptySet()
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
    private val paymentRepository: PaymentRepository = PaymentRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(TeacherListUiState())
    val state: StateFlow<TeacherListUiState> = _state.asStateFlow()

    fun load() {
        _state.update { it.copy(loading = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                val uid = FirebaseAuth.getInstance().currentUser?.uid
                // A student who is also a teacher should not see themselves in the list
                val teachers = teacherRepository.getAllTeachers().filter { it.uid != uid }
                val paid = if (uid != null) paymentRepository.getPaidTeacherIds(uid) else emptySet()
                _state.update {
                    it.copy(loading = false, teachers = teachers, paidTeacherIds = paid)
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