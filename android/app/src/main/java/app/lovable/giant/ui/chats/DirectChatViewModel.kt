package app.lovable.giant.ui.chats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.lovable.giant.GiantApplication
import app.lovable.giant.data.models.DirectMessage
import app.lovable.giant.data.models.UserProfile
import app.lovable.giant.data.remote.SupabaseRealtimeClient
import app.lovable.giant.data.remote.SupabaseRestClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

sealed class DirectChatUiState {
    object Loading : DirectChatUiState()
    data class Success(
        val otherUser: UserProfile,
        val messages: List<DirectMessage>,
        val currentUserId: String
    ) : DirectChatUiState()
    data class Error(val message: String) : DirectChatUiState()
}

class DirectChatViewModel(private val otherId: String) : ViewModel() {
    private val restClient = SupabaseRestClient()
    private val sessionRepo = GiantApplication.instance.sessionRepository
    private val realtimeClient = SupabaseRealtimeClient.instance

    private val _uiState = MutableStateFlow<DirectChatUiState>(DirectChatUiState.Loading)
    val uiState: StateFlow<DirectChatUiState> = _uiState.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    private val _replyTo = MutableStateFlow<DirectMessage?>(null)
    val replyTo: StateFlow<DirectMessage?> = _replyTo.asStateFlow()

    private var currentProfile: UserProfile? = null
    private val currentMessages = mutableListOf<DirectMessage>()

    init {
        loadChatData()
        startRealtimeListener()
        startPolling()
    }

    private fun startRealtimeListener() {
        realtimeClient.connect()
        viewModelScope.launch {
            val session = sessionRepo.loadSession()
            val myId = session?.userId ?: return@launch
            realtimeClient.incomingDirectMessages.collect { newMsg ->
                val isRelevant = (newMsg.senderId == myId && newMsg.receiverId == otherId) ||
                        (newMsg.senderId == otherId && newMsg.receiverId == myId)
                if (isRelevant) {
                    val existingIndex = currentMessages.indexOfFirst { it.id == newMsg.id }
                    if (existingIndex != -1) {
                        currentMessages[existingIndex] = newMsg
                    } else {
                        currentMessages.add(newMsg)
                    }
                    currentProfile?.let { prof ->
                        _uiState.value = DirectChatUiState.Success(
                            otherUser = prof,
                            messages = currentMessages.toList(),
                            currentUserId = myId
                        )
                    }
                }
            }
        }
    }

    private fun loadChatData() {
        viewModelScope.launch {
            _uiState.value = DirectChatUiState.Loading
            val session = sessionRepo.loadSession()
            if (session == null || session.accessToken.isNullOrEmpty()) {
                _uiState.value = DirectChatUiState.Error("يرجى تسجيل الدخول")
                return@launch
            }

            val profRes = restClient.getUserProfile(otherId, session.accessToken)
            val profile = profRes.getOrElse {
                UserProfile(id = otherId, username = "مستخدم")
            }
            currentProfile = profile

            val msgsRes = restClient.getConversationMessages(session.userId, otherId, session.accessToken)
            msgsRes.onSuccess { msgs ->
                currentMessages.clear()
                currentMessages.addAll(msgs)
                _uiState.value = DirectChatUiState.Success(
                    otherUser = profile,
                    messages = currentMessages.toList(),
                    currentUserId = session.userId
                )
            }.onFailure { err ->
                _uiState.value = DirectChatUiState.Error(err.message ?: "فشل في تحميل الرسائل")
            }
        }
    }

    private fun startPolling() {
        viewModelScope.launch {
            while (isActive) {
                delay(3000) // Poll for new messages every 3s
                val session = sessionRepo.loadSession()
                if (session != null && !session.accessToken.isNullOrEmpty() && currentProfile != null) {
                    val msgsRes = restClient.getConversationMessages(session.userId, otherId, session.accessToken)
                    msgsRes.onSuccess { msgs ->
                        if (msgs.size != currentMessages.size || (msgs.isNotEmpty() && msgs.last().id != currentMessages.lastOrNull()?.id)) {
                            currentMessages.clear()
                            currentMessages.addAll(msgs)
                            _uiState.value = DirectChatUiState.Success(
                                otherUser = currentProfile!!,
                                messages = currentMessages.toList(),
                                currentUserId = session.userId
                            )
                        }
                    }
                }
            }
        }
    }

    fun sendMessage(content: String) {
        if (content.isBlank()) return
        val session = sessionRepo.loadSession() ?: return
        val token = session.accessToken ?: return

        viewModelScope.launch {
            _isSending.value = true
            val replyId = _replyTo.value?.id
            _replyTo.value = null

            // Optimistic message
            val tempMsg = DirectMessage(
                id = "temp_${System.currentTimeMillis()}",
                senderId = session.userId,
                receiverId = otherId,
                content = content.trim(),
                createdAt = "",
                messageType = "text",
                replyToId = replyId
            )
            currentMessages.add(tempMsg)
            currentProfile?.let { prof ->
                _uiState.value = DirectChatUiState.Success(
                    otherUser = prof,
                    messages = currentMessages.toList(),
                    currentUserId = session.userId
                )
            }

            val res = restClient.sendDirectMessage(session.userId, otherId, content.trim(), replyId, token)
            res.onSuccess { actualMsg ->
                val index = currentMessages.indexOfFirst { it.id == tempMsg.id }
                if (index != -1) {
                    currentMessages[index] = actualMsg
                }
                currentProfile?.let { prof ->
                    _uiState.value = DirectChatUiState.Success(
                        otherUser = prof,
                        messages = currentMessages.toList(),
                        currentUserId = session.userId
                    )
                }
            }.onFailure {
                // Keep the optimistic message or handle error
            }
            _isSending.value = false
        }
    }

    fun setReplyTo(msg: DirectMessage?) {
        _replyTo.value = msg
    }
}
