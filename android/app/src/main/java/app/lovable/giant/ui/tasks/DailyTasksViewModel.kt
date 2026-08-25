package app.lovable.giant.ui.tasks

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.lovable.giant.data.models.DailyTaskModel
import app.lovable.giant.data.models.LevelThresholdModel
import app.lovable.giant.data.remote.SupabaseRestClient
import app.lovable.giant.data.repository.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class DailyTasksUiState {
    object Loading : DailyTasksUiState()
    data class Success(
        val tasks: List<DailyTaskModel>,
        val levels: List<LevelThresholdModel>,
        val points: Long
    ) : DailyTasksUiState()
    data class Error(val message: String) : DailyTasksUiState()
}

class DailyTasksViewModel(application: Application) : AndroidViewModel(application) {
    private val sessionRepo = SessionRepository(application)

    private val _uiState = MutableStateFlow<DailyTasksUiState>(DailyTasksUiState.Loading)
    val uiState: StateFlow<DailyTasksUiState> = _uiState.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    private val _claimingKind = MutableStateFlow<String?>(null)
    val claimingKind: StateFlow<String?> = _claimingKind.asStateFlow()

    init {
        loadTasks()
    }

    fun clearActionMessage() {
        _actionMessage.value = null
    }

    fun loadTasks() {
        val token = sessionRepo.getToken()
        val userId = sessionRepo.getUserId()

        if (token.isNullOrEmpty() || userId.isNullOrEmpty()) {
            _uiState.value = DailyTasksUiState.Error("يرجى تسجيل الدخول أولاً")
            return
        }

        viewModelScope.launch {
            _uiState.value = DailyTasksUiState.Loading

            val tasksResult = SupabaseRestClient.getDailyTasks(token)
            val levelsResult = SupabaseRestClient.getLevelThresholds(token)
            val profileResult = SupabaseRestClient.getUserProfile(userId, token)

            if (tasksResult.isSuccess && levelsResult.isSuccess) {
                val tasks = tasksResult.getOrDefault(emptyList())
                val levels = levelsResult.getOrDefault(emptyList())
                val points = profileResult.getOrNull()?.points ?: 0L

                _uiState.value = DailyTasksUiState.Success(
                    tasks = tasks,
                    levels = levels,
                    points = points
                )
            } else {
                val err = tasksResult.exceptionOrNull()?.message ?: levelsResult.exceptionOrNull()?.message ?: "تعذر تحميل المهام اليومية"
                _uiState.value = DailyTasksUiState.Error(err)
            }
        }
    }

    fun claimReward(kind: String) {
        val token = sessionRepo.getToken() ?: return

        viewModelScope.launch {
            _claimingKind.value = kind
            val result = SupabaseRestClient.claimDailyReward(kind, token)
            _claimingKind.value = null

            if (result.isSuccess) {
                val json = result.getOrNull()
                val rewardPts = json?.optLong("reward_points", 0L) ?: 0L
                val giftName = json?.optString("gift_name", null)
                val isLevelUp = json?.optBoolean("level_up", false) ?: false
                val newLevel = json?.optInt("new_level", 1) ?: 1
                val levelName = json?.optString("level_name", "") ?: ""

                val sb = StringBuilder("+$rewardPts نقطة")
                if (!giftName.isNullOrEmpty()) {
                    sb.append(" و $giftName")
                }
                if (isLevelUp) {
                    sb.append(" · ترقّيت للمستوى $newLevel ($levelName) 🎉")
                }

                _actionMessage.value = sb.toString()
                loadTasks()
            } else {
                _actionMessage.value = result.exceptionOrNull()?.message ?: "تعذر استلام المكافأة"
            }
        }
    }
}
