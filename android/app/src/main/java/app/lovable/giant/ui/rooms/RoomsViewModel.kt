package app.lovable.giant.ui.rooms

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.lovable.giant.GiantApplication
import app.lovable.giant.data.models.Room
import app.lovable.giant.data.remote.SupabaseRestClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class RoomsUiState {
    object Loading : RoomsUiState()
    data class Success(val rooms: List<Room>) : RoomsUiState()
    data class Error(val message: String) : RoomsUiState()
}

class RoomsViewModel : ViewModel() {
    private val restClient = SupabaseRestClient()
    private val sessionRepo = GiantApplication.instance.sessionRepository

    private val _uiState = MutableStateFlow<RoomsUiState>(RoomsUiState.Loading)
    val uiState: StateFlow<RoomsUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        loadRooms()
    }

    fun loadRooms() {
        viewModelScope.launch {
            _uiState.value = RoomsUiState.Loading
            val token = sessionRepo.loadSession()?.accessToken
            val res = restClient.getRooms(token)
            res.onSuccess { list ->
                _uiState.value = RoomsUiState.Success(list)
            }.onFailure { err ->
                _uiState.value = RoomsUiState.Error(err.message ?: "فشل في تحميل الغرف")
            }
        }
    }

    fun refreshRooms() {
        viewModelScope.launch {
            _isRefreshing.value = true
            val token = sessionRepo.loadSession()?.accessToken
            val res = restClient.getRooms(token)
            res.onSuccess { list ->
                _uiState.value = RoomsUiState.Success(list)
            }
            _isRefreshing.value = false
        }
    }

    fun createRoom(name: String, desc: String?, isPrivate: Boolean, onCreated: (Room) -> Unit, onError: (String) -> Unit) {
        val session = sessionRepo.loadSession()
        val token = session?.accessToken
        if (token.isNullOrEmpty()) {
            onError("يجب تسجيل الدخول لإنشاء غرفة")
            return
        }

        viewModelScope.launch {
            val res = restClient.createRoom(name, desc, isPrivate, token)
            res.onSuccess { newRoom ->
                loadRooms()
                onCreated(newRoom)
            }.onFailure { err ->
                onError(err.message ?: "فشل إنشاء الغرفة")
            }
        }
    }
}
