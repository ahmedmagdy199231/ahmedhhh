package app.lovable.giant.ui.games

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.lovable.giant.GiantApplication
import app.lovable.giant.data.remote.SupabaseRestClient
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

data class GamesUiState(
    val points: Long = 0,
    val gameWins: Long = 0,
    val isDailyClaimed: Boolean = false,
    val isLoading: Boolean = false
)

sealed class GameEvent {
    data class ShowToast(val message: String) : GameEvent()
    data class WinAwarded(val game: String, val points: Int) : GameEvent()
}

class GamesViewModel : ViewModel() {
    private val restClient = SupabaseRestClient()
    private val sessionRepo = GiantApplication.instance.sessionRepository
    private val prefs = GiantApplication.instance.getSharedPreferences("giant_games_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(GamesUiState())
    val uiState: StateFlow<GamesUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<GameEvent>()
    val events: SharedFlow<GameEvent> = _events.asSharedFlow()

    init {
        loadUserGameStats()
        checkDailyRewardStatus()
    }

    fun loadUserGameStats() {
        viewModelScope.launch {
            val session = sessionRepo.loadSession() ?: return@launch
            val token = session.accessToken ?: return@launch

            val profileRes = restClient.getUserProfile(session.userId, token)
            profileRes.onSuccess { profile ->
                _uiState.value = _uiState.value.copy(
                    points = profile.points,
                    gameWins = profile.level.toLong() // or wins
                )
            }
        }
    }

    private fun checkDailyRewardStatus() {
        val session = sessionRepo.loadSession() ?: return
        val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val lastClaimed = prefs.getString("daily_${session.userId}", null)
        _uiState.value = _uiState.value.copy(isDailyClaimed = (lastClaimed == todayKey))
    }

    fun claimDailyReward() {
        if (_uiState.value.isDailyClaimed) return
        val session = sessionRepo.loadSession() ?: return
        val token = session.accessToken ?: return

        val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val randomPts = 10 + Random.nextInt(21) // 10..30 points

        viewModelScope.launch {
            val res = restClient.recordGameWin("daily", randomPts, token)
            res.onSuccess {
                prefs.edit().putString("daily_${session.userId}", todayKey).apply()
                _uiState.value = _uiState.value.copy(
                    isDailyClaimed = true,
                    points = _uiState.value.points + randomPts
                )
                _events.emit(GameEvent.ShowToast("🎁 جائزتك اليومية: +$randomPts نقطة!"))
                loadUserGameStats()
            }.onFailure { err ->
                _events.emit(GameEvent.ShowToast(err.message ?: "فشل استلام الجائزة اليومية"))
            }
        }
    }

    fun recordWin(gameKey: String, points: Int) {
        val session = sessionRepo.loadSession() ?: return
        val token = session.accessToken ?: return

        viewModelScope.launch {
            val res = restClient.recordGameWin(gameKey, points, token)
            res.onSuccess {
                _uiState.value = _uiState.value.copy(
                    points = _uiState.value.points + points,
                    gameWins = _uiState.value.gameWins + 1
                )
                _events.emit(GameEvent.ShowToast("🏆 فزت! +$points نقطة"))
                _events.emit(GameEvent.WinAwarded(gameKey, points))
                loadUserGameStats()
            }.onFailure { err ->
                _events.emit(GameEvent.ShowToast(err.message ?: "فشل تسجيل الفوز"))
            }
        }
    }
}
