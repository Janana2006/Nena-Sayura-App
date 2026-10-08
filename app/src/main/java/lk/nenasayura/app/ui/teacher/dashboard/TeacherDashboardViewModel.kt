package lk.nenasayura.app.ui.teacher.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import lk.nenasayura.app.data.model.Payment
import lk.nenasayura.app.data.repository.PaymentRepository

data class EarningsUiState(
    val loading: Boolean = true,
    val errorMessage: String? = null,
    /** Different students who have paid, so they have access. */
    val paidStudents: Int = 0,
    val pendingCount: Int = 0,
    val gross: Double = 0.0,
    val adminCommission: Double = 0.0,
    val teacherNet: Double = 0.0,
    val currentPercent: Double = 0.0,
    val paidPayments: List<Payment> = emptyList()
)

/** Works out how many students paid a teacher and what the teacher receives after the admin's cut. */
class TeacherDashboardViewModel(
    private val repository: PaymentRepository = PaymentRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(EarningsUiState())
    val state: StateFlow<EarningsUiState> = _state.asStateFlow()

    fun load(teacherId: String) {
        _state.update { it.copy(loading = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                val defaultPercent = repository.getDefaultCommissionPercent()
                val all = repository.getTeacherPayments(teacherId)
                val paid = all.filter { it.isPaid }

                val gross = paid.sumOf { it.amount }
                // Each payment uses the % saved when it was made (or the current default).
                val commission = paid.sumOf { p ->
                    p.amount * (p.commissionPercent ?: defaultPercent) / 100.0
                }

                _state.value = EarningsUiState(
                    loading = false,
                    paidStudents = paid.map { it.studentId }.distinct().size,
                    pendingCount = all.count { it.isPending },
                    gross = gross,
                    adminCommission = commission,
                    teacherNet = gross - commission,
                    currentPercent = defaultPercent,
                    paidPayments = paid
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        loading = false,
                        errorMessage = "Could not load earnings: ${e.localizedMessage}"
                    )
                }
            }
        }
    }
}