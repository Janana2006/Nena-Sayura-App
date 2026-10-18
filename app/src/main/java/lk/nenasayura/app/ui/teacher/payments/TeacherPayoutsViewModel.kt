package lk.nenasayura.app.ui.teacher.payments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import lk.nenasayura.app.data.model.Payout
import lk.nenasayura.app.data.repository.PaymentRepository
import lk.nenasayura.app.data.repository.PayoutRepository
import lk.nenasayura.app.util.round2

data class TeacherPayoutsUiState(
    val loading: Boolean = true,
    val errorMessage: String? = null,
    /** The teacher's share of every student payment (after the admin's commission). */
    val earned: Double = 0.0,
    /** What the admin has already sent to the teacher's bank account. */
    val paidOut: Double = 0.0,
    val payouts: List<Payout> = emptyList()
) {
    /** Still waiting to be paid. Becomes 0 when the admin has paid everything. */
    val pending: Double get() = maxOf(0.0, round2(earned - paidOut))
}

/** Earnings, what was already paid out, and the history of payments the teacher received. */
class TeacherPayoutsViewModel(
    private val paymentRepository: PaymentRepository = PaymentRepository(),
    private val payoutRepository: PayoutRepository = PayoutRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(TeacherPayoutsUiState())
    val state: StateFlow<TeacherPayoutsUiState> = _state.asStateFlow()

    fun load(teacherId: String) {
        _state.update { it.copy(errorMessage = null) }
        viewModelScope.launch {
            try {
                val defaultPercent = paymentRepository.getDefaultCommissionPercent()
                val paid = paymentRepository.getTeacherPayments(teacherId).filter { it.isPaid }
                val payouts = payoutRepository.getTeacherPayouts(teacherId)

                // Each payment uses the % saved when it was made (or the current default)
                val earned = round2(
                    paid.sumOf { p -> p.amount - p.amount * (p.commissionPercent ?: defaultPercent) / 100.0 }
                )
                _state.value = TeacherPayoutsUiState(
                    loading = false,
                    earned = earned,
                    paidOut = round2(payouts.sumOf { it.amount }),
                    payouts = payouts
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(loading = false, errorMessage = "Could not load payments: ${e.localizedMessage}")
                }
            }
        }
    }
}