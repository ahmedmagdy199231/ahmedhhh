package app.lovable.giant.ui.calls

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.lovable.giant.data.models.CallRecordModel
import app.lovable.giant.data.remote.SupabaseRestClient
import app.lovable.giant.data.repository.SessionRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class CallsHistoryUiState {
    object Loading : CallsHistoryUiState()
    data class Success(val calls: List<CallRecordModel>) : CallsHistoryUiState()
    data class Error(val message: String) : CallsHistoryUiState()
}

enum class DirectCallState {
    CONNECTING,
    RINGING,
    CONNECTED,
    ENDED
}

class CallsViewModel(
    private val context: Context,
    private val restClient: SupabaseRestClient = SupabaseRestClient(),
    private val sessionRepository: SessionRepository = SessionRepository.getInstance(context)
) : ViewModel() {

    private val _historyState = MutableStateFlow<CallsHistoryUiState>(CallsHistoryUiState.Loading)
    val historyState: StateFlow<CallsHistoryUiState> = _historyState.asStateFlow()

    // Direct Call Active State
    private val _callState = MutableStateFlow(DirectCallState.CONNECTING)
    val callState: StateFlow<DirectCallState> = _callState.asStateFlow()

    private val _callDurationSeconds = MutableStateFlow(0L)
    val callDurationSeconds: StateFlow<Long> = _callDurationSeconds.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _isSpeakerOn = MutableStateFlow(true)
    val isSpeakerOn: StateFlow<Boolean> = _isSpeakerOn.asStateFlow()

    private val _isVideoEnabled = MutableStateFlow(true)
    val isVideoEnabled: StateFlow<Boolean> = _isVideoEnabled.asStateFlow()

    private var timerJob: Job? = null

    init {
        loadHistory()
    }

    fun loadHistory() {
        viewModelScope.launch {
            _historyState.value = CallsHistoryUiState.Loading
            val session = sessionRepository.session.value
            val userId = session?.userId ?: return@launch
            val token = session.accessToken

            val res = restClient.getCallsHistory(currentUserId = userId, token = token)
            res.onSuccess { list ->
                _historyState.value = CallsHistoryUiState.Success(list)
            }.onFailure { err ->
                _historyState.value = CallsHistoryUiState.Error(err.localizedMessage ?: "فشل تحميل سجل المكالمات")
            }
        }
    }

    fun startCall(peerId: String, callType: String, isIncoming: Boolean) {
        viewModelScope.launch {
            _callState.value = if (isIncoming) DirectCallState.RINGING else DirectCallState.CONNECTING
            _callDurationSeconds.value = 0L

            // Simulate handshake & answer transition
            delay(1500)
            _callState.value = DirectCallState.CONNECTED
            startTimer()
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_callState.value == DirectCallState.CONNECTED) {
                delay(1000)
                _callDurationSeconds.value += 1
            }
        }
    }

    fun toggleMute() {
        _isMuted.value = !_isMuted.value
    }

    fun toggleSpeaker() {
        _isSpeakerOn.value = !_isSpeakerOn.value
    }

    fun toggleVideo() {
        _isVideoEnabled.value = !_isVideoEnabled.value
    }

    fun endCall() {
        _callState.value = DirectCallState.ENDED
        timerJob?.cancel()
    }
}
