package app.lovable.giant.ui.stories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.lovable.giant.data.SessionManager
import app.lovable.giant.data.models.StoryItemModel
import app.lovable.giant.data.models.StoryReactionItem
import app.lovable.giant.data.models.StoryUserModel
import app.lovable.giant.data.models.StoryViewItem
import app.lovable.giant.data.remote.SupabaseRestClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class StoriesUiState(
    val isLoading: Boolean = false,
    val activeStoryUsers: List<StoryUserModel> = emptyList(),
    val currentViewerUser: StoryUserModel? = null,
    val userStories: List<StoryItemModel> = emptyList(),
    val currentStoryIndex: Int = 0,
    val isStoryViewerOpen: Boolean = false,
    val isCreatingStory: Boolean = false,
    val isPublishing: Boolean = false,
    val storyViews: List<StoryViewItem> = emptyList(),
    val storyReactions: List<StoryReactionItem> = emptyList(),
    val isViewsSheetOpen: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

class StoriesViewModel : ViewModel() {
    private val restClient = SupabaseRestClient()
    private val _uiState = MutableStateFlow(StoriesUiState())
    val uiState: StateFlow<StoriesUiState> = _uiState.asStateFlow()

    init {
        loadActiveStories()
    }

    fun loadActiveStories() {
        val token = SessionManager.currentUser?.accessToken ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val res = restClient.getActiveStories(token)
            if (res.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    activeStoryUsers = res.getOrDefault(emptyList())
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "فشل تحميل القصص"
                )
            }
        }
    }

    fun openStoryViewer(user: StoryUserModel) {
        val token = SessionManager.currentUser?.accessToken ?: return
        _uiState.value = _uiState.value.copy(
            currentViewerUser = user,
            isStoryViewerOpen = true,
            currentStoryIndex = 0,
            userStories = emptyList()
        )
        loadStoriesForUser(user.userId, token)
    }

    private fun loadStoriesForUser(userId: String, token: String) {
        viewModelScope.launch {
            val res = restClient.getUserStories(userId, token)
            if (res.isSuccess) {
                val list = res.getOrDefault(emptyList())
                _uiState.value = _uiState.value.copy(userStories = list)
                if (list.isNotEmpty()) {
                    val firstStory = list[0]
                    recordView(firstStory.id, token)
                    loadStoryReactions(firstStory.id, token)
                    if (userId == SessionManager.currentUser?.userId) {
                        loadStoryViews(firstStory.id, token)
                    }
                }
            }
        }
    }

    fun nextStory() {
        val stories = _uiState.value.userStories
        val currentIdx = _uiState.value.currentStoryIndex
        val token = SessionManager.currentUser?.accessToken ?: return

        if (currentIdx + 1 < stories.size) {
            val nextIdx = currentIdx + 1
            _uiState.value = _uiState.value.copy(currentStoryIndex = nextIdx)
            val nextStory = stories[nextIdx]
            recordView(nextStory.id, token)
            loadStoryReactions(nextStory.id, token)
            if (_uiState.value.currentViewerUser?.userId == SessionManager.currentUser?.userId) {
                loadStoryViews(nextStory.id, token)
            }
        } else {
            // Move to next user
            val users = _uiState.value.activeStoryUsers
            val currentUserIdx = users.indexOfFirst { it.userId == _uiState.value.currentViewerUser?.userId }
            if (currentUserIdx != -1 && currentUserIdx + 1 < users.size) {
                openStoryViewer(users[currentUserIdx + 1])
            } else {
                closeStoryViewer()
            }
        }
    }

    fun previousStory() {
        val currentIdx = _uiState.value.currentStoryIndex
        val token = SessionManager.currentUser?.accessToken ?: return

        if (currentIdx > 0) {
            val prevIdx = currentIdx - 1
            _uiState.value = _uiState.value.copy(currentStoryIndex = prevIdx)
            val prevStory = _uiState.value.userStories[prevIdx]
            recordView(prevStory.id, token)
            loadStoryReactions(prevStory.id, token)
        } else {
            // Move to previous user
            val users = _uiState.value.activeStoryUsers
            val currentUserIdx = users.indexOfFirst { it.userId == _uiState.value.currentViewerUser?.userId }
            if (currentUserIdx > 0) {
                openStoryViewer(users[currentUserIdx - 1])
            }
        }
    }

    fun closeStoryViewer() {
        _uiState.value = _uiState.value.copy(
            isStoryViewerOpen = false,
            currentViewerUser = null,
            userStories = emptyList(),
            currentStoryIndex = 0,
            isViewsSheetOpen = false
        )
        loadActiveStories()
    }

    private fun recordView(storyId: String, token: String) {
        viewModelScope.launch {
            restClient.viewStory(storyId, token)
        }
    }

    private fun loadStoryViews(storyId: String, token: String) {
        viewModelScope.launch {
            val res = restClient.getStoryViews(storyId, token)
            if (res.isSuccess) {
                _uiState.value = _uiState.value.copy(storyViews = res.getOrDefault(emptyList()))
            }
        }
    }

    private fun loadStoryReactions(storyId: String, token: String) {
        viewModelScope.launch {
            val res = restClient.getStoryReactions(storyId, token)
            if (res.isSuccess) {
                _uiState.value = _uiState.value.copy(storyReactions = res.getOrDefault(emptyList()))
            }
        }
    }

    fun reactToStory(emoji: String) {
        val story = currentStory() ?: return
        val token = SessionManager.currentUser?.accessToken ?: return
        viewModelScope.launch {
            val res = restClient.reactToStory(story.id, emoji, token)
            if (res.isSuccess) {
                loadStoryReactions(story.id, token)
            }
        }
    }

    fun commentOnStory(message: String) {
        val story = currentStory() ?: return
        val token = SessionManager.currentUser?.accessToken ?: return
        if (message.isBlank()) return
        viewModelScope.launch {
            val res = restClient.commentOnStory(story.id, message, token)
            if (res.isSuccess) {
                _uiState.value = _uiState.value.copy(successMessage = "تم إرسال الرد في المحادثة الخاصة")
            }
        }
    }

    fun deleteCurrentStory() {
        val story = currentStory() ?: return
        val token = SessionManager.currentUser?.accessToken ?: return
        viewModelScope.launch {
            val res = restClient.deleteStory(story.id, token)
            if (res.isSuccess) {
                val updated = _uiState.value.userStories.filter { it.id != story.id }
                if (updated.isEmpty()) {
                    closeStoryViewer()
                } else {
                    _uiState.value = _uiState.value.copy(
                        userStories = updated,
                        currentStoryIndex = 0
                    )
                }
            }
        }
    }

    fun openCreateStory() {
        _uiState.value = _uiState.value.copy(isCreatingStory = true)
    }

    fun closeCreateStory() {
        _uiState.value = _uiState.value.copy(isCreatingStory = false)
    }

    fun toggleViewsSheet() {
        _uiState.value = _uiState.value.copy(isViewsSheetOpen = !_uiState.value.isViewsSheetOpen)
    }

    fun publishStory(content: String?, mediaUrl: String?, mediaType: String?, background: String?) {
        val token = SessionManager.currentUser?.accessToken ?: return
        if (content.isNullOrBlank() && mediaUrl.isNullOrBlank()) {
            _uiState.value = _uiState.value.copy(error = "اكتب نصاً أو اختر وسائط")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isPublishing = true)
            val res = restClient.publishStory(content, mediaUrl, mediaType, background, token)
            _uiState.value = _uiState.value.copy(isPublishing = false)
            if (res.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    isCreatingStory = false,
                    successMessage = "تم نشر القصة بنجاح 🎉"
                )
                loadActiveStories()
            } else {
                _uiState.value = _uiState.value.copy(
                    error = res.exceptionOrNull()?.message ?: "فشل نشر القصة"
                )
            }
        }
    }

    private fun currentStory(): StoryItemModel? {
        val stories = _uiState.value.userStories
        val idx = _uiState.value.currentStoryIndex
        return if (idx in stories.indices) stories[idx] else null
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(error = null, successMessage = null)
    }
}
