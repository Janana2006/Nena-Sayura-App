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
import lk.nenasayura.app.data.model.AccessType
import lk.nenasayura.app.data.model.ContentType
import lk.nenasayura.app.data.model.TeacherContent
import lk.nenasayura.app.data.model.TeacherProfile
import lk.nenasayura.app.data.repository.PaymentRepository
import lk.nenasayura.app.data.repository.TeacherContentRepository
import lk.nenasayura.app.data.repository.TeacherProfileRepository

data class TeacherMaterialsUiState(
    val loading: Boolean = true,
    val errorMessage: String? = null,
    /** The teacher whose page this is. */
    val teacher: TeacherProfile? = null,
    /** Every upload of this teacher (no approval step). */
    val items: List<TeacherContent> = emptyList(),
    /** true once this student has paid this teacher. */
    val hasPaid: Boolean = false,
    val typeFilter: ContentType? = null
) {
    val shown: List<TeacherContent>
        get() = items.filter { typeFilter == null || it.type == typeFilter }

    /** The student still has to pay this teacher before PAID material opens. */
    val needsPayment: Boolean
        get() = teacher?.isPaidTeacher == true && !hasPaid

    fun isUnlocked(item: TeacherContent): Boolean =
        item.access == AccessType.FREE || !needsPayment
}

/** What a student sees on ONE teacher's page: the material this teacher uploaded. */
class TeacherMaterialsViewModel(
    private val contentRepository: TeacherContentRepository = TeacherContentRepository(),
    private val paymentRepository: PaymentRepository = PaymentRepository(),
    private val teacherRepository: TeacherProfileRepository = TeacherProfileRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(TeacherMaterialsUiState())
    val state: StateFlow<TeacherMaterialsUiState> = _state.asStateFlow()

    fun load(teacherId: String) {
        _state.update { it.copy(loading = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                val teacher = teacherRepository.getProfile(teacherId, "", "")
                val items = contentRepository.getVisibleContents(teacherId)
                val uid = FirebaseAuth.getInstance().currentUser?.uid
                val paid = if (uid != null) teacherId in paymentRepository.getPaidTeacherIds(uid) else false
                _state.update {
                    it.copy(loading = false, teacher = teacher, items = items, hasPaid = paid)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(loading = false, errorMessage = "Could not load materials: ${e.localizedMessage}")
                }
            }
        }
    }

    fun onTypeFilter(type: ContentType?) = _state.update { it.copy(typeFilter = type) }
}