package app.lovable.giant.data.models

// Community & Posts Models
data class CommunityPostModel(
    val id: String,
    val authorId: String,
    val content: String? = null,
    val mediaUrl: String? = null,
    val mediaType: String? = null,
    val kind: String = "text", // text, image, video, mixed
    val createdAt: String,
    val edited: Boolean = false,
    val authorUsername: String? = null,
    val authorAvatarUrl: String? = null,
    val reactionsCount: Int = 0,
    val commentsCount: Int = 0,
    val myReaction: String? = null,
    val isSaved: Boolean = false
)

data class CommunityCommentModel(
    val id: String,
    val postId: String,
    val authorId: String,
    val content: String,
    val createdAt: String,
    val authorUsername: String? = null,
    val authorAvatarUrl: String? = null
)

data class CommunityReactionModel(
    val postId: String,
    val userId: String,
    val reaction: String
)

// Stories Models
data class StoryUserModel(
    val userId: String,
    val username: String? = null,
    val avatarUrl: String? = null,
    val equippedFrame: String? = null,
    val storyCount: Int = 0,
    val latestAt: String = "",
    val hasUnseen: Boolean = false
)

data class StoryItemModel(
    val id: String,
    val userId: String,
    val content: String? = null,
    val mediaUrl: String? = null,
    val mediaType: String? = null, // image, video
    val background: String? = null,
    val createdAt: String = "",
    val expiresAt: String = "",
    val isHidden: Boolean = false
)

data class StoryViewItem(
    val viewerId: String,
    val viewedAt: String,
    val username: String? = null,
    val avatarUrl: String? = null
)

data class StoryReactionItem(
    val emoji: String,
    val count: Int,
    val mine: Boolean = false
)
