package lk.nenasayura.app.ui.student.teachercontent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import lk.nenasayura.app.data.model.TeacherProfile
import lk.nenasayura.app.data.repository.PaymentRepository
import lk.nenasayura.app.data.repository.TeacherProfileRepository

/** The ways a student can choose to pay on the payment dashboard. */
enum class PayMethod(val label: String, val hint: String) {
    CARD("Credit / Debit card", "Visa, Mastercard"),
    BANK("Online banking", "Pay from your bank account"),
    WALLET("Mobile wallet", "Pay with your mobile wallet")
}

/** REVIEW = payment dashboard, PROCESSING = waiting, SUCCESS = payment saved. */
enum class PayStep { REVIEW, PROCESSING, SUCCESS }

data class TeacherPaymentUiState(
    val loading: Boolean = true,
    val errorMessage: String? = null,
    val teacher: TeacherProfile? = null,
    /** The student already paid this teacher before opening this screen. */
    val alreadyPaid: Boolean = false,
    val method: PayMethod = PayMethod.CARD,
    val step: PayStep = PayStep.REVIEW,
    /** Shown on the dashboard if the payment failed. */
    val payError: String? = null
)

class TeacherPaymentViewModel(
    private val teacherRepository: TeacherProfileRepository = TeacherProfileRepository(),
    private val paymentRepository: PaymentRepository = PaymentRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(TeacherPaymentUiState())
    val state: StateFlow<TeacherPaymentUiState> = _state.asStateFlow()

    fun load(teacherId: String) {
        // Do not reset the screen while a payment is running or finished
        if (_state.value.step != PayStep.REVIEW) return
        _state.update { it.copy(loading = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                val teacher = teacherRepository.getProfile(teacherId, "", "")
                val uid = FirebaseAuth.getInstance().currentUser?.uid
                val paid = uid != null && teacherId in paymentRepository.getPaidTeacherIds(uid)
                _state.update { it.copy(loading = false, teacher = teacher, alreadyPaid = paid) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(loading = false, errorMessage = "Could not load payment details: ${e.localizedMessage}")
                }
            }
        }
    }

    fun onMethod(method: PayMethod) = _state.update { it.copy(method = method) }

    /** "Confirm payment" button. */
    fun confirm() {
        val current = _state.value
        val teacher = current.teacher ?: return
        if (current.step != PayStep.REVIEW) return

        _state.update { it.copy(step = PayStep.PROCESSING, payError = null) }
        viewModelScope.launch {
            try {
                val uid = FirebaseAuth.getInstance().currentUser?.uid
                    ?: throw IllegalStateException("Please log in again.")
                delay(1200) // short "processing" moment
                paymentRepository.payTeacher(uid, teacher, current.method.label)
                _state.update { it.copy(step = PayStep.SUCCESS) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        step = PayStep.REVIEW,
                        payError = "Payment failed: ${e.localizedMessage ?: "please try again."}"
                    )
                }
            }
        }
    }
}