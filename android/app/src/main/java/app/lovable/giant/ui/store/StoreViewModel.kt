package app.lovable.giant.ui.store

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.lovable.giant.data.models.GiftCatalogModel
import app.lovable.giant.data.models.ShopItemModel
import app.lovable.giant.data.remote.SupabaseRestClient
import app.lovable.giant.data.repository.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class StoreUiState {
    object Loading : StoreUiState()
    data class Success(
        val items: List<ShopItemModel>,
        val gifts: List<GiftCatalogModel>,
        val ownedItemIds: Set<String>,
        val equippedMap: Map<String, String?>,
        val points: Long,
        val myGender: String?,
        val adminUsername: String
    ) : StoreUiState()
    data class Error(val message: String) : StoreUiState()
}

class StoreViewModel(application: Application) : AndroidViewModel(application) {
    private val sessionRepo = SessionRepository(application)

    private val _uiState = MutableStateFlow<StoreUiState>(StoreUiState.Loading)
    val uiState: StateFlow<StoreUiState> = _uiState.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    private val _busyItemId = MutableStateFlow<String?>(null)
    val busyItemId: StateFlow<String?> = _busyItemId.asStateFlow()

    init {
        loadStoreData()
    }

    fun clearActionMessage() {
        _actionMessage.value = null
    }

    fun loadStoreData() {
        val token = sessionRepo.getToken()
        val userId = sessionRepo.getUserId()

        if (token.isNullOrEmpty() || userId.isNullOrEmpty()) {
            _uiState.value = StoreUiState.Error("يرجى تسجيل الدخول أولاً")
            return
        }

        viewModelScope.launch {
            _uiState.value = StoreUiState.Loading

            val itemsResult = SupabaseRestClient.getShopItems(token)
            val giftsResult = SupabaseRestClient.getGiftsCatalog(token)
            val inventoryResult = SupabaseRestClient.getUserInventory(userId, token)
            val equippedResult = SupabaseRestClient.getEquippedItems(userId, token)
            val adminUsernameResult = SupabaseRestClient.getPointsSellerUsername(token)

            if (itemsResult.isSuccess && giftsResult.isSuccess) {
                val items = itemsResult.getOrDefault(emptyList())
                val gifts = giftsResult.getOrDefault(emptyList())
                val owned = inventoryResult.getOrDefault(emptySet())
                val equipped = equippedResult.getOrDefault(emptyMap())
                val pointsStr = equipped["points"] ?: "0"
                val points = pointsStr.toLongOrNull() ?: 0L
                val myGender = equipped["gender"]
                val adminUsername = adminUsernameResult.getOrDefault("admin")

                _uiState.value = StoreUiState.Success(
                    items = items,
                    gifts = gifts,
                    ownedItemIds = owned,
                    equippedMap = equipped,
                    points = points,
                    myGender = myGender,
                    adminUsername = adminUsername
                )
            } else {
                val err = itemsResult.exceptionOrNull()?.message ?: giftsResult.exceptionOrNull()?.message ?: "تعذر تحميل المتجر"
                _uiState.value = StoreUiState.Error(err)
            }
        }
    }

    fun buyItem(item: ShopItemModel) {
        val token = sessionRepo.getToken() ?: return
        val currentState = _uiState.value as? StoreUiState.Success ?: return

        if (currentState.points < item.price) {
            _actionMessage.value = "نقاطك غير كافية لشراء هذا العنصر"
            return
        }

        viewModelScope.launch {
            _busyItemId.value = item.id
            val buyResult = SupabaseRestClient.purchaseShopItem(item.id, token)

            if (buyResult.isSuccess) {
                // Auto equip on purchase
                val equipResult = SupabaseRestClient.equipShopItem(item.id, token)
                val newEquipped = currentState.equippedMap.toMutableMap()
                if (equipResult.isSuccess) {
                    newEquipped[item.kind] = item.id
                }

                val newOwned = currentState.ownedItemIds.toMutableSet().apply { add(item.id) }
                val newPoints = currentState.points - item.price

                _uiState.value = currentState.copy(
                    ownedItemIds = newOwned,
                    equippedMap = newEquipped,
                    points = newPoints
                )
                _actionMessage.value = "تم شراء «${item.nameAr}» وتفعيله بنجاح 🎉"
            } else {
                _actionMessage.value = buyResult.exceptionOrNull()?.message ?: "تعذر إتمام الشراء"
            }
            _busyItemId.value = null
        }
    }

    fun toggleEquip(item: ShopItemModel) {
        val token = sessionRepo.getToken() ?: return
        val currentState = _uiState.value as? StoreUiState.Success ?: return

        val isCurrentlyEquipped = currentState.equippedMap[item.kind] == item.id

        viewModelScope.launch {
            _busyItemId.value = item.id
            val result = if (isCurrentlyEquipped) {
                SupabaseRestClient.unequipShopKind(item.kind, token)
            } else {
                SupabaseRestClient.equipShopItem(item.id, token)
            }

            if (result.isSuccess) {
                val newEquipped = currentState.equippedMap.toMutableMap()
                if (isCurrentlyEquipped) {
                    newEquipped[item.kind] = null
                    _actionMessage.value = "تم إلغاء التفعيل"
                } else {
                    newEquipped[item.kind] = item.id
                    _actionMessage.value = "تم التفعيل بنجاح"
                }
                _uiState.value = currentState.copy(equippedMap = newEquipped)
            } else {
                _actionMessage.value = result.exceptionOrNull()?.message ?: "تعذر تغيير التفعيل"
            }
            _busyItemId.value = null
        }
    }
}
