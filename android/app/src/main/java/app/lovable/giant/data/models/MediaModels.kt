package app.lovable.giant.data.models

data class MusicTrackModel(
    val videoId: String? = null,
    val title: String,
    val artist: String,
    val artwork: String,
    val previewUrl: String,
    val durationMs: Long = 0,
    val requesterName: String? = null,
    val requesterId: String? = null
)

data class RoomMusicModel(
    val current: MusicTrackModel? = null,
    val queue: List<MusicQueueItem> = emptyList(),
    val startedAt: String? = null,
    val paused: Boolean = false,
    val pausedPosMs: Long = 0,
    val volume: Int = 70
)

data class MusicQueueItem(
    val title: String,
    val artist: String
)

data class TrackResultModel(
    val videoId: String? = null,
    val title: String,
    val artist: String,
    val artwork: String,
    val previewUrl: String,
    val durationMs: Long = 0
)
