package app.lovable.giant.ui.rooms

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.lovable.giant.GiantApplication
import app.lovable.giant.data.models.ChatMessage
import app.lovable.giant.data.models.Room
import app.lovable.giant.data.remote.SupabaseRestClient
import app.lovable.giant.webrtc.NativeVoiceRoomController
import app.lovable.giant.webrtc.models.VoiceRoomState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

sealed class SingleRoomUiState {
    object Loading : SingleRoomUiState()
    data class Success(val room: Room, val messages: List<ChatMessage>) : SingleRoomUiState()
    data class Error(val message: String) : SingleRoomUiState()
}

class SingleRoomViewModel(private val roomId: String) : ViewModel() {
    private val restClient = SupabaseRestClient()
    private val sessionRepo = GiantApplication.instance.sessionRepository
    private val voiceController = NativeVoiceRoomController.getInstance(GiantApplication.instance)

    private val _uiState = MutableStateFlow<SingleRoomUiState>(SingleRoomUiState.Loading)
    val uiState: StateFlow<SingleRoomUiState> = _uiState.asStateFlow()

    val voiceState: StateFlow<VoiceRoomState> = voiceController.roomState

    private var currentRoom: Room? = null
    private val currentMessages = mutableListOf<ChatMessage>()

    init {
        enterRoom()
        startPollingMessages()
    }

    private fun enterRoom() {
        viewModelScope.launch {
            _uiState.value = SingleRoomUiState.Loading
            val session = sessionRepo.loadSession()
            val token = session?.accessToken

            val roomRes = restClient.getRoomDetails(roomId, token)
            roomRes.onSuccess { r ->
                currentRoom = r
                if (session != null && !token.isNullOrEmpty()) {
                    restClient.joinRoom(roomId, session.userId, token)
                    // Start Native WebRTC voice room session
                    voiceController.enterRoom(
                        roomId = roomId,
                        roomName = r.name,
                        userId = session.userId,
                        token = token
                    )
                }
                loadMessages()
            }.onFailure { err ->
                _uiState.value = SingleRoomUiState.Error(err.message ?: "فشل في تحميل تفاصيل الغرفة")
            }
        }
    }

    private fun loadMessages() {
        viewModelScope.launch {
            val session = sessionRepo.loadSession()
            val token = session?.accessToken
            val res = restClient.getRoomMessages(roomId, token)
            res.onSuccess { msgs ->
                currentMessages.clear()
                currentMessages.addAll(msgs)
                currentRoom?.let { r ->
                    _uiState.value = SingleRoomUiState.Success(r, currentMessages.toList())
                }
            }
        }
    }

    private fun startPollingMessages() {
        viewModelScope.launch {
            while (isActive) {
                delay(3000) // Poll for new room messages every 3s
                if (currentRoom != null) {
                    val session = sessionRepo.loadSession()
                    val token = session?.accessToken
                    val res = restClient.getRoomMessages(roomId, token)
                    res.onSuccess { msgs ->
                        if (msgs.size != currentMessages.size || (msgs.isNotEmpty() && msgs.last().id != currentMessages.lastOrNull()?.id)) {
                            currentMessages.clear()
                            currentMessages.addAll(msgs)
                            _uiState.value = SingleRoomUiState.Success(currentRoom!!, currentMessages.toList())
                        }
                    }
                }
            }
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        val session = sessionRepo.loadSession() ?: return
        val token = session.accessToken ?: return

        viewModelScope.launch {
            val res = restClient.sendRoomMessage(roomId, session.userId, text, token)
            res.onSuccess { msg ->
                currentMessages.add(msg)
                currentRoom?.let { r ->
                    _uiState.value = SingleRoomUiState.Success(r, currentMessages.toList())
                }
            }
        }
    }

    fun toggleMic() {
        voiceController.toggleMic()
    }

    fun toggleSpeaker() {
        voiceController.toggleSpeaker()
    }

    fun joinStage() {
        voiceController.joinStage()
    }

    fun leaveStage() {
        voiceController.leaveStage()
    }

    fun raiseHand() {
        voiceController.raiseHand()
    }

    fun lowerHand() {
        voiceController.lowerHand()
    }

    fun acceptInvite() {
        voiceController.acceptInvite()
    }

    fun rejectInvite() {
        voiceController.rejectInvite()
    }

    fun inviteUser(userId: String) {
        voiceController.inviteUser(userId)
    }

    fun muteSpeaker(userId: String, muted: Boolean) {
        voiceController.muteSpeaker(userId, muted)
    }

    fun kickSpeaker(userId: String) {
        voiceController.kickSpeaker(userId)
    }

    fun retryConnection() {
        voiceController.retryConnection()
    }

    fun leaveRoom(onLeft: () -> Unit) {
        viewModelScope.launch {
            val session = sessionRepo.loadSession()
            val token = session?.accessToken
            if (session != null && !token.isNullOrEmpty()) {
                restClient.leaveRoom(roomId, session.userId, token)
            }
            voiceController.leaveRoom()
            onLeft()
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Do not leave stage if just navigating back and user wants audio in background,
        // unless explicitly left via leaveRoom.
    }
}
