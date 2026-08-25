package app.lovable.giant.ui.notifications

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.lovable.giant.data.models.NotificationItemModel
import app.lovable.giant.data.remote.SupabaseRestClient
import app.lovable.giant.data.repository.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class NotificationsUiState {
    object Loading : NotificationsUiState()
    data class Success(val items: List<NotificationItemModel>, val totalUnread: Int) : NotificationsUiState()
    data class Error(val message: String) : NotificationsUiState()
}

class NotificationsViewModel(
    private val context: Context,
    private val restClient: SupabaseRestClient = SupabaseRestClient(),
    private val sessionRepository: SessionRepository = SessionRepository.getInstance(context)
) : ViewModel() {

    private val _uiState = MutableStateFlow<NotificationsUiState>(NotificationsUiState.Loading)
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    init {
        loadNotifications()
    }

    fun loadNotifications() {
        viewModelScope.launch {
            _uiState.value = NotificationsUiState.Loading
            val session = sessionRepository.session.value
            val userId = session?.userId ?: return@launch
            val token = session.accessToken

            val result = restClient.getNotifications(currentUserId = userId, token = token)
            result.onSuccess { list ->
                val totalUnread = list.sumOf { it.unreadCount }
                _uiState.value = NotificationsUiState.Success(list, totalUnread)
            }.onFailure { err ->
                _uiState.value = NotificationsUiState.Error(err.localizedMessage ?: "فشل تحميل الإشعارات")
            }
        }
    }

    fun markAllRead() {
        viewModelScope.launch {
            val session = sessionRepository.session.value
            val userId = session?.userId ?: return@launch
            val token = session.accessToken

            restClient.markAllNotificationsRead(currentUserId = userId, token = token)
            loadNotifications()
        }
    }
}
