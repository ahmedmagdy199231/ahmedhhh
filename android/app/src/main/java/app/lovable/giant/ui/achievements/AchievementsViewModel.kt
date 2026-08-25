package app.lovable.giant.ui.achievements

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.lovable.giant.data.models.LeaderboardDataModel
import app.lovable.giant.data.models.LeaderboardRowModel
import app.lovable.giant.data.models.TopGameWinnerModel
import app.lovable.giant.data.remote.SupabaseRestClient
import app.lovable.giant.data.repository.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AchievementsUiState {
    object Loading : AchievementsUiState()
    data class Success(
        val board: LeaderboardDataModel,
        val winners: List<TopGameWinnerModel>,
        val currentUserId: String? = null
    ) : AchievementsUiState()
    data class Error(val message: String) : AchievementsUiState()
}

enum class LeaderboardTab(val title: String, val description: String) {
    OVERALL("الأكثر تفاعلًا", "غرف • منشورات • تفاعل • إنفاق"),
    POSTERS("نجوم المنشورات", "نشر • تعليقات • إعجابات على منشوراتك"),
    SPENDERS("أكبر المشترين", "النقاط المنفقة في المتجر هذا الأسبوع"),
    WINNERS("أبطال الألعاب", "الأكثر فوزاً في ألعاب التطبيق")
}

class AchievementsViewModel(
    private val context: Context,
    private val restClient: SupabaseRestClient = SupabaseRestClient(),
    private val sessionRepository: SessionRepository = SessionRepository.getInstance(context)
) : ViewModel() {

    private val _uiState = MutableStateFlow<AchievementsUiState>(AchievementsUiState.Loading)
    val uiState: StateFlow<AchievementsUiState> = _uiState.asStateFlow()

    private val _selectedTab = MutableStateFlow(LeaderboardTab.OVERALL)
    val selectedTab: StateFlow<LeaderboardTab> = _selectedTab.asStateFlow()

    init {
        loadData()
    }

    fun selectTab(tab: LeaderboardTab) {
        _selectedTab.value = tab
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = AchievementsUiState.Loading
            val session = sessionRepository.session.value
            val currentUserId = session?.userId

            val boardResult = restClient.getWeeklyLeaderboards(limit = 20, token = session?.accessToken)
            val winnersResult = restClient.getTopGameWinners(limit = 20, token = session?.accessToken)

            if (boardResult.isSuccess || winnersResult.isSuccess) {
                _uiState.value = AchievementsUiState.Success(
                    board = boardResult.getOrDefault(LeaderboardDataModel()),
                    winners = winnersResult.getOrDefault(emptyList()),
                    currentUserId = currentUserId
                )
            } else {
                _uiState.value = AchievementsUiState.Error("فشل تحميل قائمة المتصدرين والإنجازات")
            }
        }
    }
}
