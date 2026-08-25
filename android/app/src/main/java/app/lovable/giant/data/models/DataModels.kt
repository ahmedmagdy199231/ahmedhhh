package app.lovable.giant.data.models

data class UserSession(
    val userId: String,
    val email: String? = null,
    val username: String? = null,
    val avatarUrl: String? = null,
    val accessToken: String? = null
)

data class Room(
    val id: String,
    val name: String,
    val description: String? = null,
    val ownerId: String? = null,
    val memberCount: Int = 0,
    val isPrivate: Boolean = false,
    val category: String = "general"
)

data class ChatMessage(
    val id: String,
    val roomId: String? = null,
    val senderId: String,
    val senderName: String? = null,
    val content: String,
    val createdAt: String,
    val messageType: String = "text"
)

data class DirectMessage(
    val id: String,
    val senderId: String,
    val receiverId: String,
    val content: String,
    val createdAt: String,
    val messageType: String = "text",
    val mediaUrl: String? = null,
    val readAt: String? = null,
    val deliveredAt: String? = null,
    val replyToId: String? = null
)

data class ConversationItem(
    val otherId: String,
    val username: String,
    val avatarUrl: String? = null,
    val lastMessage: String,
    val createdAt: String,
    val unreadCount: Int = 0
)

data class SearchUserItem(
    val id: String,
    val username: String,
    val avatarUrl: String? = null
)

data class UserBadge(
    val id: String,
    val code: String,
    val nameAr: String,
    val color: String
)

data class UserProfile(
    val id: String,
    val username: String,
    val bio: String? = null,
    val avatarUrl: String? = null,
    val coverUrl: String? = null,
    val coverType: String? = null,
    val level: Int = 1,
    val points: Long = 0,
    val isVip: Boolean = false,
    val gender: String? = null,
    val country: String? = null,
    val hideLastSeen: Boolean = false,
    val dmLocked: Boolean = false,
    val profileViews: Long = 0,
    val email: String? = null,
    val pendingEmail: String? = null,
    val badges: List<UserBadge> = emptyList()
)

data class ShopItemModel(
    val id: String,
    val kind: String, // badge, avatar_frame, name_color, chat_color, effect
    val code: String,
    val nameAr: String,
    val price: Long,
    val payload: String = "{}",
    val sortOrder: Int = 0,
    val genderTarget: String? = null,
    val colorHex: String? = null,
    val previewEmoji: String? = null
)

data class GiftCatalogModel(
    val id: String,
    val name: String,
    val emoji: String? = null,
    val costPoints: Long,
    val scope: String = "room", // room or global
    val category: String? = null
)

data class DailyTaskModel(
    val kind: String,
    val label: String,
    val target: Int,
    val reward: Long,
    val progress: Int,
    val claimed: Boolean
)

data class LevelThresholdModel(
    val level: Int,
    val minPoints: Long,
    val name: String
)

data class RoomMemberItem(
    val userId: String,
    val username: String,
    val avatarUrl: String? = null
)
