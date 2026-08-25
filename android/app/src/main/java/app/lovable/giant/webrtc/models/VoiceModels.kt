package app.lovable.giant.webrtc.models

enum class VoiceConnectionState {
    IDLE,
    CONNECTING,
    CONNECTED,
    RECONNECTING,
    FAILED,
    DISCONNECTED
}

enum class AudioRoute {
    SPEAKER,
    EARPIECE,
    BLUETOOTH,
    WIRED_HEADSET
}

data class RoomSpeaker(
    val id: String,
    val roomId: String,
    val userId: String,
    val username: String? = null,
    val avatarUrl: String? = null,
    val isMuted: Boolean = false,
    val isSpeaking: Boolean = false,
    val addedBy: String? = null,
    val joinedAt: String? = null
)

data class RaisedHand(
    val id: String,
    val roomId: String,
    val userId: String,
    val username: String? = null,
    val avatarUrl: String? = null,
    val createdAt: String? = null
)

data class SpeakerInvite(
    val id: String,
    val roomId: String,
    val userId: String,
    val invitedBy: String,
    val createdAt: String? = null
)

data class VoiceSignal(
    val id: String,
    val roomId: String,
    val fromUser: String,
    val toUser: String,
    val signalType: String, // "offer", "answer", "ice", "leave"
    val payloadJson: String,
    val createdAt: String? = null
)

data class IceServerConfig(
    val urls: List<String>,
    val username: String? = null,
    val credential: String? = null
)

data class MyVoiceState(
    val isOnStage: Boolean = false,
    val isMuted: Boolean = true,
    val isSpeaking: Boolean = false,
    val hasRaisedHand: Boolean = false,
    val pendingInvite: SpeakerInvite? = null,
    val isJoiningStage: Boolean = false
)

data class VoiceRoomState(
    val roomId: String? = null,
    val roomName: String? = null,
    val connectionState: VoiceConnectionState = VoiceConnectionState.IDLE,
    val audioRoute: AudioRoute = AudioRoute.SPEAKER,
    val isSpeakerOn: Boolean = true,
    val speakers: List<RoomSpeaker> = emptyList(),
    val raisedHands: List<RaisedHand> = emptyList(),
    val allInvites: List<SpeakerInvite> = emptyList(),
    val speakingUsers: Set<String> = emptySet(),
    val myState: MyVoiceState = MyVoiceState(),
    val errorMessage: String? = null
)
