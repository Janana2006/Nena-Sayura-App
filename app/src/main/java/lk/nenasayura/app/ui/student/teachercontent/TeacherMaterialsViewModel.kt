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
import lk.nenasayura.app.data.repository.PaymentRepository
import lk.nenasayura.app.data.repository.TeacherContentRepository

data class TeacherMaterialsUiState(
    val loading: Boolean = true,
    val errorMessage: String? = null,
    /** Only APPROVED uploads ever reach this list. */
    val items: List<TeacherContent> = emptyList(),
    /** Teachers this student has paid; their PAID material is unlocked. */
    val paidTeacherIds: Set<String> = emptySet(),
    val typeFilter: ContentType? = null
) {
    val shown: List<TeacherContent>
        get() = items.filter { typeFilter == null || it.type == typeFilter }

    fun isUnlocked(item: TeacherContent): Boolean =
        item.access == AccessType.FREE || item.teacherId in paidTeacherIds
}

/** What a student sees: material that the admin approved. */
class TeacherMaterialsViewModel(
    private val contentRepository: TeacherContentRepository = TeacherContentRepository(),
    private val paymentRepository: PaymentRepository = PaymentRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(TeacherMaterialsUiState())
    val state: StateFlow<TeacherMaterialsUiState> = _state.asStateFlow()

    fun load() {
        _state.update { it.copy(loading = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                val items = contentRepository.getApprovedContents()
                val uid = FirebaseAuth.getInstance().currentUser?.uid
                val paid = if (uid != null) paymentRepository.getPaidTeacherIds(uid) else emptySet()
                _state.update { it.copy(loading = false, items = items, paidTeacherIds = paid) }
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