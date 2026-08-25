package app.lovable.giant.ui.gifts

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.lovable.giant.data.models.GiftCatalogModel
import app.lovable.giant.data.models.RoomMemberItem
import app.lovable.giant.data.remote.SupabaseRestClient
import app.lovable.giant.data.repository.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class GiftPickerUiState {
    object Loading : GiftPickerUiState()
    data class Success(
        val gifts: List<GiftCatalogModel>,
        val members: List<RoomMemberItem>,
        val points: Long
    ) : GiftPickerUiState()
    data class Error(val message: String) : GiftPickerUiState()
}

class GiftPickerViewModel(
    application: Application,
    private val roomId: String
) : AndroidViewModel(application) {
    private val sessionRepo = SessionRepository(application)

    private val _uiState = MutableStateFlow<GiftPickerUiState>(GiftPickerUiState.Loading)
    val uiState: StateFlow<GiftPickerUiState> = _uiState.asStateFlow()

    private val _selectedMemberId = MutableStateFlow<String?>(null)
    val selectedMemberId: StateFlow<String?> = _selectedMemberId.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    init {
        loadData()
    }

    fun selectMember(userId: String) {
        _selectedMemberId.value = userId
    }

    fun clearActionMessage() {
        _actionMessage.value = null
    }

    fun loadData() {
        val token = sessionRepo.getToken()
        val currentUserId = sessionRepo.getUserId()

        if (token.isNullOrEmpty() || currentUserId.isNullOrEmpty()) {
            _uiState.value = GiftPickerUiState.Error("يرجى تسجيل الدخول")
            return
        }

        viewModelScope.launch {
            _uiState.value = GiftPickerUiState.Loading

            val giftsResult = SupabaseRestClient.getGiftsCatalog(token)
            val membersResult = SupabaseRestClient.getRoomMembersWithProfiles(roomId, currentUserId, token)
            val profileResult = SupabaseRestClient.getUserProfile(currentUserId, token)

            if (giftsResult.isSuccess) {
                val gifts = giftsResult.getOrDefault(emptyList())
                val members = membersResult.getOrDefault(emptyList())
                val points = profileResult.getOrNull()?.points ?: 0L

                _uiState.value = GiftPickerUiState.Success(
                    gifts = gifts,
                    members = members,
                    points = points
                )

                if (members.isNotEmpty() && _selectedMemberId.value == null) {
                    _selectedMemberId.value = members.first().userId
                }
            } else {
                _uiState.value = GiftPickerUiState.Error(giftsResult.exceptionOrNull()?.message ?: "تعذر تحميل الهدايا")
            }
        }
    }

    fun sendGift(gift: GiftCatalogModel, onSuccess: () -> Unit) {
        val token = sessionRepo.getToken() ?: return
        val receiver = _selectedMemberId.value

        if (receiver.isNullOrEmpty()) {
            _actionMessage.value = "يرجى اختيار العضو المستلم أولاً"
            return
        }

        val currentState = _uiState.value as? GiftPickerUiState.Success ?: return
        if (currentState.points < gift.costPoints) {
            _actionMessage.value = "نقاطك غير كافية لإرسال هذه الهدية"
            return
        }

        viewModelScope.launch {
            _isSending.value = true
            val result = SupabaseRestClient.sendGift(
                receiverId = receiver,
                giftId = gift.id,
                roomId = roomId,
                token = token
            )
            _isSending.value = false

            if (result.isSuccess) {
                _actionMessage.value = "تم إرسال هدية «${gift.name}» بنجاح 🎁"
                onSuccess()
            } else {
                _actionMessage.value = result.exceptionOrNull()?.message ?: "تعذر إرسال الهدية"
            }
        }
    }
}
