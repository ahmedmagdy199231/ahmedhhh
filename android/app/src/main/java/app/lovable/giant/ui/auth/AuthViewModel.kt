package app.lovable.giant.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.lovable.giant.GiantApplication
import app.lovable.giant.data.models.UserSession
import app.lovable.giant.data.remote.SupabaseAuthClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class Success(val session: UserSession) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

class AuthViewModel : ViewModel() {
    private val authClient = SupabaseAuthClient()
    private val sessionRepo = GiantApplication.instance.sessionRepository

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun login(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _uiState.value = AuthUiState.Error("يرجى إدخال البريد الإلكتروني وكلمة المرور")
            return
        }
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val res = authClient.signIn(email, pass)
            res.onSuccess { session ->
                sessionRepo.saveSession(session)
                _uiState.value = AuthUiState.Success(session)
            }.onFailure { err ->
                _uiState.value = AuthUiState.Error(err.message ?: "حدث خطأ أثناء تسجيل الدخول")
            }
        }
    }

    fun register(email: String, pass: String, username: String) {
        if (email.isBlank() || pass.isBlank() || username.isBlank()) {
            _uiState.value = AuthUiState.Error("يرجى ملء جميع الحقول")
            return
        }
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val res = authClient.signUp(email, pass, username)
            res.onSuccess { session ->
                sessionRepo.saveSession(session)
                _uiState.value = AuthUiState.Success(session)
            }.onFailure { err ->
                _uiState.value = AuthUiState.Error(err.message ?: "حدث خطأ أثناء إنشاء الحساب")
            }
        }
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }
}
