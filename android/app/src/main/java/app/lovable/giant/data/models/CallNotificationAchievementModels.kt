package app.lovable.giant.data.models

data class NotificationItemModel(
    val otherId: String,
    val username: String,
    val avatarUrl: String? = null,
    val lastMessage: String,
    val messageType: String,
    val createdAt: String,
    val unreadCount: Int = 0
)

data class LeaderboardRowModel(
    val userId: String,
    val username: String,
    val avatarUrl: String? = null,
    val score: Long,
    val breakdown: Map<String, Long> = emptyMap()
)

data class LeaderboardDataModel(
    val posters: List<LeaderboardRowModel> = emptyList(),
    val spenders: List<LeaderboardRowModel> = emptyList(),
    val overall: List<LeaderboardRowModel> = emptyList(),
    val weekStart: String? = null,
    val weekEnd: String? = null
)

data class TopGameWinnerModel(
    val userId: String,
    val username: String,
    val avatarUrl: String? = null,
    val wins: Long = 0
)

data class CallRecordModel(
    val id: String,
    val callerId: String,
    val calleeId: String,
    val otherUserId: String,
    val otherUsername: String = "مستخدم",
    val otherAvatarUrl: String? = null,
    val callType: String = "audio", // "audio" | "video"
    val status: String = "ended", // "ringing", "accepted", "rejected", "ended", "missed"
    val startedAt: String,
    val durationSeconds: Long = 0,
    val endReason: String? = null,
    val isIncoming: Boolean = false
)
