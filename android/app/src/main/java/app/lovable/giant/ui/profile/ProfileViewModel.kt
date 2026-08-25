package app.lovable.giant.ui.profile

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.lovable.giant.GiantApplication
import app.lovable.giant.data.models.UserProfile
import app.lovable.giant.data.remote.SupabaseAuthClient
import app.lovable.giant.data.remote.SupabaseRestClient
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ProfileUiState {
    object Loading : ProfileUiState()
    data class Success(val profile: UserProfile) : ProfileUiState()
    data class Error(val message: String) : ProfileUiState()
}

class ProfileViewModel : ViewModel() {
    private val restClient = SupabaseRestClient()
    private val authClient = SupabaseAuthClient()
    private val sessionRepo = GiantApplication.instance.sessionRepository

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _isUploadingAvatar = MutableStateFlow(false)
    val isUploadingAvatar: StateFlow<Boolean> = _isUploadingAvatar.asStateFlow()

    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent: SharedFlow<String> = _toastEvent.asSharedFlow()

    private val _logoutEvent = MutableSharedFlow<Unit>()
    val logoutEvent: SharedFlow<Unit> = _logoutEvent.asSharedFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading
            val session = sessionRepo.loadSession()
            if (session == null || session.accessToken.isNullOrEmpty()) {
                _uiState.value = ProfileUiState.Error("يرجى تسجيل الدخول أولاً")
                return@launch
            }

            val profResult = restClient.getUserProfile(session.userId, session.accessToken)
            profResult.onSuccess { baseProfile ->
                // Also fetch auth user email if available
                var updatedProf = baseProfile.copy(email = session.email)
                val authRes = restClient.getAuthUser(session.accessToken)
                authRes.onSuccess { authObj ->
                    val email = authObj.optString("email", session.email ?: "")
                    val pendingEmail = authObj.optString("new_email", null)
                    updatedProf = updatedProf.copy(email = email, pendingEmail = pendingEmail)
                }
                _uiState.value = ProfileUiState.Success(updatedProf)
            }.onFailure { err ->
                _uiState.value = ProfileUiState.Error(err.message ?: "فشل في تحميل الملف الشخصي")
            }
        }
    }

    fun updateProfileInfo(bio: String, gender: String?, country: String?) {
        val session = sessionRepo.loadSession() ?: return
        val token = session.accessToken ?: return
        val currentState = _uiState.value as? ProfileUiState.Success ?: return

        viewModelScope.launch {
            _isSaving.value = true
            val res = restClient.updateProfile(
                userId = session.userId,
                token = token,
                bio = bio.trim().ifEmpty { null },
                gender = gender,
                country = country?.ifEmpty { null },
                hideLastSeen = currentState.profile.hideLastSeen,
                dmLocked = currentState.profile.dmLocked
            )

            res.onSuccess {
                val newProfile = currentState.profile.copy(
                    bio = bio.trim(),
                    gender = gender,
                    country = country
                )
                _uiState.value = ProfileUiState.Success(newProfile)
                _toastEvent.emit("تم حفظ التعديلات بنجاح")
            }.onFailure { err ->
                _toastEvent.emit(err.message ?: "فشل في حفظ التعديلات")
            }
            _isSaving.value = false
        }
    }

    fun togglePrivacy(hideLastSeen: Boolean? = null, dmLocked: Boolean? = null) {
        val session = sessionRepo.loadSession() ?: return
        val token = session.accessToken ?: return
        val currentState = _uiState.value as? ProfileUiState.Success ?: return

        val newHideLastSeen = hideLastSeen ?: currentState.profile.hideLastSeen
        val newDmLocked = dmLocked ?: currentState.profile.dmLocked

        viewModelScope.launch {
            val res = restClient.updateProfile(
                userId = session.userId,
                token = token,
                bio = currentState.profile.bio,
                gender = currentState.profile.gender,
                country = currentState.profile.country,
                hideLastSeen = newHideLastSeen,
                dmLocked = newDmLocked
            )

            res.onSuccess {
                val newProfile = currentState.profile.copy(
                    hideLastSeen = newHideLastSeen,
                    dmLocked = newDmLocked
                )
                _uiState.value = ProfileUiState.Success(newProfile)
                _toastEvent.emit("تم تحديث إعدادات الخصوصية")
            }.onFailure {
                _toastEvent.emit("فشل في تحديث الخصوصية")
            }
        }
    }

    fun uploadAvatarFromUri(context: Context, uri: Uri) {
        val session = sessionRepo.loadSession() ?: return
        val token = session.accessToken ?: return
        val currentState = _uiState.value as? ProfileUiState.Success ?: return

        viewModelScope.launch {
            _isUploadingAvatar.value = true
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bytes = inputStream?.readBytes()
                inputStream?.close()

                if (bytes == null || bytes.isEmpty()) {
                    _toastEvent.emit("تعذر قراءة الصورة")
                    _isUploadingAvatar.value = false
                    return@launch
                }

                if (bytes.size > 5 * 1024 * 1024) {
                    _toastEvent.emit("الحد الأقصى لحجم الصورة هو 5 ميجابايت")
                    _isUploadingAvatar.value = false
                    return@launch
                }

                val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
                val uploadRes = restClient.uploadAvatar(session.userId, token, bytes, mimeType)

                uploadRes.onSuccess { publicUrl ->
                    val updatedProf = currentState.profile.copy(avatarUrl = publicUrl)
                    _uiState.value = ProfileUiState.Success(updatedProf)
                    sessionRepo.saveSession(session.copy(avatarUrl = publicUrl))
                    _toastEvent.emit("تم تحديث الصورة الشخصية بنجاح")
                }.onFailure { err ->
                    _toastEvent.emit(err.message ?: "فشل في رفع الصورة")
                }
            } catch (e: Exception) {
                _toastEvent.emit("حدث خطأ أثناء رفع الصورة: ${e.message}")
            } finally {
                _isUploadingAvatar.value = false
            }
        }
    }

    fun changeEmail(newEmail: String) {
        val session = sessionRepo.loadSession() ?: return
        val token = session.accessToken ?: return

        viewModelScope.launch {
            _isSaving.value = true
            val res = restClient.updateAccountEmail(token, newEmail.trim())
            res.onSuccess {
                val currentState = _uiState.value as? ProfileUiState.Success
                if (currentState != null) {
                    _uiState.value = ProfileUiState.Success(currentState.profile.copy(pendingEmail = newEmail.trim()))
                }
                _toastEvent.emit("تم إرسال رابط التأكيد إلى بريدك الجديد")
            }.onFailure { err ->
                _toastEvent.emit(err.message ?: "فشل في تحديث البريد الإلكتروني")
            }
            _isSaving.value = false
        }
    }

    fun changePassword(newPassword: String) {
        val session = sessionRepo.loadSession() ?: return
        val token = session.accessToken ?: return

        viewModelScope.launch {
            _isSaving.value = true
            val res = restClient.updateAccountPassword(token, newPassword)
            res.onSuccess {
                _toastEvent.emit("تم تغيير كلمة المرور بنجاح")
            }.onFailure { err ->
                _toastEvent.emit(err.message ?: "فشل في تغيير كلمة المرور")
            }
            _isSaving.value = false
        }
    }

    fun logout() {
        viewModelScope.launch {
            sessionRepo.clearSession()
            _logoutEvent.emit(Unit)
        }
    }
}
