package lk.nenasayura.app.ui.teacher.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import lk.nenasayura.app.data.model.AppNotification
import lk.nenasayura.app.data.repository.NotificationRepository

data class NotificationsUiState(
    val loading: Boolean = true,
    val errorMessage: String? = null,
    val items: List<AppNotification> = emptyList(),
    /** Number shown on the bell. Goes to 0 as soon as the notifications screen is opened. */
    val badge: Int = 0
)

/** Loads the teacher's notifications (for example "Payment received") and the bell number. */
class TeacherNotificationsViewModel(
    private val repository: NotificationRepository = NotificationRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(NotificationsUiState())
    val state: StateFlow<NotificationsUiState> = _state.asStateFlow()

    fun load(userId: String) {
        _state.update { it.copy(errorMessage = null) }
        viewModelScope.launch {
            try {
                val items = repository.getNotifications(userId)
                _state.update {
                    it.copy(loading = false, items = items, badge = items.count { n -> !n.read })
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(loading = false, errorMessage = "Could not load notifications: ${e.localizedMessage}")
                }
            }
        }
    }

    /**
     * Clears the bell and saves "read" in Firestore. The list keeps its unread dots
     * until the screen is closed, so the teacher can still see which ones are new.
     */
    fun markAllRead() {
        val ids = _state.value.items.filter { !it.read }.map { it.id }
        if (ids.isEmpty()) return
        _state.update { it.copy(badge = 0) }
        viewModelScope.launch {
            try {
                repository.markRead(ids)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // The bell comes back on the next load if saving failed
            }
        }
    }
}