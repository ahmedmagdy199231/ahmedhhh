package app.lovable.giant.ui.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.lovable.giant.data.SessionManager
import app.lovable.giant.data.models.CommunityCommentModel
import app.lovable.giant.data.models.CommunityPostModel
import app.lovable.giant.data.remote.SupabaseRestClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CommunityUiState(
    val isLoading: Boolean = false,
    val isPosting: Boolean = false,
    val posts: List<CommunityPostModel> = emptyList(),
    val currentUserId: String = "",
    val sortKey: String = "newest", // newest, trending, oldest
    val mediaFilter: String = "all", // all, text, image, video
    val searchQuery: String = "",
    val showOnlyMine: Boolean = false,
    val savedPostIds: Set<String> = emptySet(),
    val error: String? = null,
    val successMessage: String? = null,
    // Active post comments state
    val commentsPostId: String? = null,
    val comments: List<CommunityCommentModel> = emptyList(),
    val isCommentsLoading: Boolean = false,
    val isSubmittingComment: Boolean = false
)

class CommunityViewModel : ViewModel() {
    private val restClient = SupabaseRestClient()
    private val _uiState = MutableStateFlow(CommunityUiState())
    val uiState: StateFlow<CommunityUiState> = _uiState.asStateFlow()

    init {
        loadCurrentUser()
        loadPosts()
    }

    private fun loadCurrentUser() {
        val user = SessionManager.currentUser
        if (user != null) {
            _uiState.value = _uiState.value.copy(currentUserId = user.userId)
        }
    }

    fun loadPosts() {
        val token = SessionManager.currentUser?.accessToken ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val res = restClient.getCommunityPosts(token)
            if (res.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    posts = res.getOrDefault(emptyList())
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = res.exceptionOrNull()?.message ?: "فشل تحميل المنشورات"
                )
            }
        }
    }

    fun setSort(sort: String) {
        _uiState.value = _uiState.value.copy(sortKey = sort)
    }

    fun setMediaFilter(filter: String) {
        _uiState.value = _uiState.value.copy(mediaFilter = filter)
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun toggleShowOnlyMine() {
        _uiState.value = _uiState.value.copy(showOnlyMine = !_uiState.value.showOnlyMine)
    }

    fun toggleSavePost(postId: String) {
        val current = _uiState.value.savedPostIds
        val updated = if (current.contains(postId)) current - postId else current + postId
        _uiState.value = _uiState.value.copy(savedPostIds = updated)
    }

    fun createPost(content: String, mediaUrl: String? = null, mediaType: String? = null, kind: String = "text") {
        val user = SessionManager.currentUser ?: return
        val token = user.accessToken ?: return
        if (content.isBlank() && mediaUrl.isNullOrBlank()) {
            _uiState.value = _uiState.value.copy(error = "اكتب شيئاً أو أرفق وسائط")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isPosting = true, error = null)
            val res = restClient.createCommunityPost(
                userId = user.userId,
                content = content,
                mediaUrl = mediaUrl,
                mediaType = mediaType,
                kind = kind,
                token = token
            )
            if (res.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    isPosting = false,
                    successMessage = "تم نشر المنشور بنجاح ✨"
                )
                loadPosts()
            } else {
                _uiState.value = _uiState.value.copy(
                    isPosting = false,
                    error = res.exceptionOrNull()?.message ?: "فشل النشر"
                )
            }
        }
    }

    fun editPost(postId: String, content: String) {
        val token = SessionManager.currentUser?.accessToken ?: return
        viewModelScope.launch {
            val res = restClient.updateCommunityPost(postId, content, token)
            if (res.isSuccess) {
                _uiState.value = _uiState.value.copy(successMessage = "تم تعديل المنشور")
                loadPosts()
            } else {
                _uiState.value = _uiState.value.copy(error = res.exceptionOrNull()?.message ?: "فشل التعديل")
            }
        }
    }

    fun deletePost(postId: String, isAdmin: Boolean = false) {
        val token = SessionManager.currentUser?.accessToken ?: return
        viewModelScope.launch {
            val res = restClient.deleteCommunityPost(postId, isAdmin, token)
            if (res.isSuccess) {
                _uiState.value = _uiState.value.copy(successMessage = "تم حذف المنشور")
                loadPosts()
            } else {
                _uiState.value = _uiState.value.copy(error = res.exceptionOrNull()?.message ?: "فشل الحذف")
            }
        }
    }

    fun reportPost(postId: String, reason: String) {
        val user = SessionManager.currentUser ?: return
        val token = user.accessToken ?: return
        viewModelScope.launch {
            val res = restClient.reportCommunityPost(postId, user.userId, reason, token)
            if (res.isSuccess) {
                _uiState.value = _uiState.value.copy(successMessage = "تم إرسال البلاغ للادارة")
            } else {
                _uiState.value = _uiState.value.copy(error = res.exceptionOrNull()?.message ?: "فشل الإبلاغ")
            }
        }
    }

    fun reactToPost(postId: String, reaction: String) {
        val user = SessionManager.currentUser ?: return
        val token = user.accessToken ?: return

        val post = _uiState.value.posts.find { it.id == postId }
        val currentReaction = post?.myReaction

        viewModelScope.launch {
            if (currentReaction == reaction) {
                // Remove reaction
                restClient.removeReactionFromPost(postId, user.userId, token)
            } else {
                restClient.reactToPost(postId, user.userId, reaction, token)
            }
            loadPosts()
        }
    }

    fun openComments(postId: String) {
        val token = SessionManager.currentUser?.accessToken ?: return
        _uiState.value = _uiState.value.copy(
            commentsPostId = postId,
            isCommentsLoading = true,
            comments = emptyList()
        )
        viewModelScope.launch {
            val res = restClient.getCommunityComments(postId, token)
            if (res.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    isCommentsLoading = false,
                    comments = res.getOrDefault(emptyList())
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isCommentsLoading = false,
                    error = "فشل تحميل التعليقات"
                )
            }
        }
    }

    fun closeComments() {
        _uiState.value = _uiState.value.copy(commentsPostId = null, comments = emptyList())
    }

    fun addComment(postId: String, content: String) {
        val user = SessionManager.currentUser ?: return
        val token = user.accessToken ?: return
        if (content.isBlank()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmittingComment = true)
            val res = restClient.addCommunityComment(postId, user.userId, content, token)
            _uiState.value = _uiState.value.copy(isSubmittingComment = false)
            if (res.isSuccess) {
                openComments(postId)
                loadPosts()
            } else {
                _uiState.value = _uiState.value.copy(error = "فشل إرسال التعليق")
            }
        }
    }

    fun deleteComment(commentId: String, postId: String) {
        val token = SessionManager.currentUser?.accessToken ?: return
        viewModelScope.launch {
            val res = restClient.deleteCommunityComment(commentId, token)
            if (res.isSuccess) {
                openComments(postId)
                loadPosts()
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(error = null, successMessage = null)
    }
}
