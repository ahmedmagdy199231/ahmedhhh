package app.lovable.giant.ui.chats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.lovable.giant.GiantApplication
import app.lovable.giant.data.models.ConversationItem
import app.lovable.giant.data.models.SearchUserItem
import app.lovable.giant.data.remote.SupabaseRestClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ChatsListUiState {
    object Loading : ChatsListUiState()
    data class Success(val conversations: List<ConversationItem>) : ChatsListUiState()
    data class Error(val message: String) : ChatsListUiState()
}

class ChatsViewModel : ViewModel() {
    private val restClient = SupabaseRestClient()
    private val sessionRepo = GiantApplication.instance.sessionRepository

    private val _uiState = MutableStateFlow<ChatsListUiState>(ChatsListUiState.Loading)
    val uiState: StateFlow<ChatsListUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _searchResults = MutableStateFlow<List<SearchUserItem>>(emptyList())
    val searchResults: StateFlow<List<SearchUserItem>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadConversations()
    }

    fun loadConversations() {
        viewModelScope.launch {
            _uiState.value = ChatsListUiState.Loading
            val session = sessionRepo.loadSession()
            if (session == null || session.accessToken.isNullOrEmpty()) {
                _uiState.value = ChatsListUiState.Error("يجب تسجيل الدخول أولاً")
                return@launch
            }

            val res = restClient.getDirectMessagesList(session.userId, session.accessToken)
            res.onSuccess { list ->
                _uiState.value = ChatsListUiState.Success(list)
            }.onFailure { err ->
                _uiState.value = ChatsListUiState.Error(err.message ?: "فشل في تحميل المحادثات")
            }
        }
    }

    fun refreshConversations() {
        viewModelScope.launch {
            _isRefreshing.value = true
            val session = sessionRepo.loadSession()
            if (session != null && !session.accessToken.isNullOrEmpty()) {
                val res = restClient.getDirectMessagesList(session.userId, session.accessToken)
                res.onSuccess { list ->
                    _uiState.value = ChatsListUiState.Success(list)
                }
            }
            _isRefreshing.value = false
        }
    }

    fun searchUsers(query: String) {
        searchJob?.cancel()
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            _isSearching.value = false
            return
        }

        searchJob = viewModelScope.launch {
            delay(300) // Debounce 300ms
            _isSearching.value = true
            val session = sessionRepo.loadSession()
            if (session != null && !session.accessToken.isNullOrEmpty()) {
                val res = restClient.searchUsers(query.trim(), session.userId, session.accessToken)
                res.onSuccess { users ->
                    _searchResults.value = users
                }.onFailure {
                    _searchResults.value = emptyList()
                }
            }
            _isSearching.value = false
        }
    }

    fun clearSearch() {
        _searchResults.value = emptyList()
        _isSearching.value = false
    }
}
