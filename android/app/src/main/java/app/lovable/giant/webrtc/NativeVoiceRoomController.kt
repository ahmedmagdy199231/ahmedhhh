package app.lovable.giant.webrtc

import android.content.Context
import android.util.Log
import app.lovable.giant.webrtc.audio.AndroidAudioDeviceManager
import app.lovable.giant.webrtc.engine.NativeWebRtcAudioEngine
import app.lovable.giant.webrtc.models.AudioRoute
import app.lovable.giant.webrtc.models.MyVoiceState
import app.lovable.giant.webrtc.models.RaisedHand
import app.lovable.giant.webrtc.models.RoomSpeaker
import app.lovable.giant.webrtc.models.SpeakerInvite
import app.lovable.giant.webrtc.models.VoiceConnectionState
import app.lovable.giant.webrtc.models.VoiceRoomState
import app.lovable.giant.webrtc.signaling.SupabaseVoiceSignaling
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class NativeVoiceRoomController private constructor(private val context: Context) {

    companion object {
        @Volatile
        private var INSTANCE: NativeVoiceRoomController? = null

        fun getInstance(context: Context): NativeVoiceRoomController {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: NativeVoiceRoomController(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val controllerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    val audioDeviceManager = AndroidAudioDeviceManager(context)
    val signaling = SupabaseVoiceSignaling()
    val audioEngine = NativeWebRtcAudioEngine(context, audioDeviceManager, signaling)

    private val _roomState = MutableStateFlow(VoiceRoomState())
    val roomState: StateFlow<VoiceRoomState> = _roomState.asStateFlow()

    private var metaPollingJob: Job? = null
    private var vadCollectorJob: Job? = null

    private var currentRoomId: String? = null
    private var currentUserId: String? = null
    private var currentAuthToken: String? = null

    init {
        // Observe audio device route changes
        controllerScope.launch {
            audioDeviceManager.currentRoute.collect { route ->
                _roomState.value = _roomState.value.copy(
                    audioRoute = route,
                    isSpeakerOn = route == AudioRoute.SPEAKER
                )
            }
        }

        // Observe WebRTC engine connection state
        controllerScope.launch {
            audioEngine.connectionState.collect { connState ->
                _roomState.value = _roomState.value.copy(connectionState = connState)
            }
        }

        // Observe local speaking activity
        controllerScope.launch {
            audioDeviceManager.isLocalSpeaking.collect { isSpeaking ->
                val myUid = currentUserId
                if (myUid != null) {
                    val currentSpeaking = _roomState.value.speakingUsers.toMutableSet()
                    if (isSpeaking && _roomState.value.myState.isOnStage && !_roomState.value.myState.isMuted) {
                        currentSpeaking.add(myUid)
                    } else {
                        currentSpeaking.remove(myUid)
                    }
                    _roomState.value = _roomState.value.copy(
                        speakingUsers = currentSpeaking,
                        myState = _roomState.value.myState.copy(isSpeaking = isSpeaking)
                    )
                }
            }
        }
    }

    fun enterRoom(roomId: String, roomName: String, userId: String, token: String?) {
        if (currentRoomId == roomId) return // Already in this room

        if (currentRoomId != null) {
            leaveRoom()
        }

        currentRoomId = roomId
        currentUserId = userId
        currentAuthToken = token

        _roomState.value = VoiceRoomState(
            roomId = roomId,
            roomName = roomName,
            connectionState = VoiceConnectionState.CONNECTING,
            isSpeakerOn = true
        )

        // Start Foreground Service to keep voice alive
        RoomVoiceForegroundService.startService(
            context = context,
            roomId = roomId,
            roomName = roomName,
            isSpeaker = false,
            isMuted = true
        )

        // Start WebRTC engine as listener initially
        audioEngine.startEngine(
            scope = controllerScope,
            roomId = roomId,
            userId = userId,
            token = token,
            asSpeaker = false
        )

        startMetaPolling(roomId, userId, token)
    }

    fun leaveRoom() {
        val roomId = currentRoomId
        val userId = currentUserId
        val token = currentAuthToken

        metaPollingJob?.cancel()
        metaPollingJob = null

        if (roomId != null && userId != null) {
            controllerScope.launch(Dispatchers.IO) {
                signaling.leaveStage(roomId, userId, token)
                signaling.lowerHand(roomId, userId, token)
            }
        }

        audioEngine.stopEngine()
        RoomVoiceForegroundService.stopService(context)

        currentRoomId = null
        currentUserId = null
        currentAuthToken = null

        _roomState.value = VoiceRoomState(connectionState = VoiceConnectionState.DISCONNECTED)
    }

    private fun startMetaPolling(roomId: String, userId: String, token: String?) {
        metaPollingJob?.cancel()
        metaPollingJob = controllerScope.launch(Dispatchers.IO) {
            while (isActive && currentRoomId == roomId) {
                try {
                    val speakersRes = signaling.getSpeakers(roomId, token)
                    val handsRes = signaling.getRaisedHands(roomId, token)
                    val invitesRes = signaling.getInvites(roomId, token)

                    var speakersList = emptyList<RoomSpeaker>()
                    var handsList = emptyList<RaisedHand>()
                    var invitesList = emptyList<SpeakerInvite>()

                    speakersRes.onSuccess { speakersList = it }
                    handsRes.onSuccess { handsList = it }
                    invitesRes.onSuccess { invitesList = it }

                    val amSpeaker = speakersList.any { it.userId == userId }
                    val mySpeakerEntry = speakersList.find { it.userId == userId }
                    val myHandRaised = handsList.any { it.userId == userId }
                    val myInvite = invitesList.find { it.userId == userId }

                    // Sync WebRTC engine with the new speakers list
                    audioEngine.syncSpeakers(speakersList.map { it.userId })

                    // If my stage status changed
                    val wasOnStage = _roomState.value.myState.isOnStage
                    if (!wasOnStage && amSpeaker) {
                        audioEngine.promoteToSpeaker()
                    } else if (wasOnStage && !amSpeaker) {
                        audioEngine.demoteToListener()
                    }

                    val isMuted = mySpeakerEntry?.isMuted ?: true

                    _roomState.value = _roomState.value.copy(
                        speakers = speakersList,
                        raisedHands = handsList,
                        allInvites = invitesList,
                        myState = MyVoiceState(
                            isOnStage = amSpeaker,
                            isMuted = isMuted,
                            hasRaisedHand = myHandRaised,
                            pendingInvite = myInvite,
                            isJoiningStage = false
                        )
                    )

                    // Update Notification
                    val rName = _roomState.value.roomName ?: "الغرفة الصوتية"
                    RoomVoiceForegroundService.updateNotification(context, rName, amSpeaker, isMuted)

                } catch (e: Exception) {
                    Log.e("VoiceController", "Error in meta polling: ${e.message}")
                }
                delay(2500)
            }
        }
    }

    fun toggleMic() {
        val myState = _roomState.value.myState
        if (!myState.isOnStage) return

        val newMuted = !myState.isMuted
        audioEngine.setMute(newMuted)

        _roomState.value = _roomState.value.copy(
            myState = myState.copy(isMuted = newMuted)
        )

        val rName = _roomState.value.roomName ?: "الغرفة الصوتية"
        RoomVoiceForegroundService.updateNotification(context, rName, true, newMuted)
    }

    fun toggleSpeaker() {
        val newState = audioDeviceManager.toggleSpeakerphone()
        _roomState.value = _roomState.value.copy(
            isSpeakerOn = newState,
            audioRoute = if (newState) AudioRoute.SPEAKER else AudioRoute.EARPIECE
        )
    }

    fun joinStage() {
        val roomId = currentRoomId ?: return
        val userId = currentUserId ?: return
        val token = currentAuthToken

        _roomState.value = _roomState.value.copy(
            myState = _roomState.value.myState.copy(isJoiningStage = true)
        )

        controllerScope.launch(Dispatchers.IO) {
            val res = signaling.joinStage(roomId, userId, token)
            res.onSuccess {
                audioEngine.promoteToSpeaker()
                _roomState.value = _roomState.value.copy(
                    myState = _roomState.value.myState.copy(isOnStage = true, isMuted = false, isJoiningStage = false)
                )
            }.onFailure { err ->
                _roomState.value = _roomState.value.copy(
                    myState = _roomState.value.myState.copy(isJoiningStage = false),
                    errorMessage = "فشل في الصعود للمنصة: ${err.message}"
                )
            }
        }
    }

    fun leaveStage() {
        val roomId = currentRoomId ?: return
        val userId = currentUserId ?: return
        val token = currentAuthToken

        audioEngine.demoteToListener()

        controllerScope.launch(Dispatchers.IO) {
            signaling.leaveStage(roomId, userId, token)
            _roomState.value = _roomState.value.copy(
                myState = _roomState.value.myState.copy(isOnStage = false, isMuted = true)
            )
        }
    }

    fun raiseHand() {
        val roomId = currentRoomId ?: return
        val userId = currentUserId ?: return
        val token = currentAuthToken

        controllerScope.launch(Dispatchers.IO) {
            signaling.raiseHand(roomId, userId, token)
            _roomState.value = _roomState.value.copy(
                myState = _roomState.value.myState.copy(hasRaisedHand = true)
            )
        }
    }

    fun lowerHand() {
        val roomId = currentRoomId ?: return
        val userId = currentUserId ?: return
        val token = currentAuthToken

        controllerScope.launch(Dispatchers.IO) {
            signaling.lowerHand(roomId, userId, token)
            _roomState.value = _roomState.value.copy(
                myState = _roomState.value.myState.copy(hasRaisedHand = false)
            )
        }
    }

    fun acceptInvite() {
        val roomId = currentRoomId ?: return
        val userId = currentUserId ?: return
        val token = currentAuthToken

        controllerScope.launch(Dispatchers.IO) {
            signaling.deleteInvite(roomId, userId, token)
            joinStage()
        }
    }

    fun rejectInvite() {
        val roomId = currentRoomId ?: return
        val userId = currentUserId ?: return
        val token = currentAuthToken

        controllerScope.launch(Dispatchers.IO) {
            signaling.deleteInvite(roomId, userId, token)
            _roomState.value = _roomState.value.copy(
                myState = _roomState.value.myState.copy(pendingInvite = null)
            )
        }
    }

    fun inviteUser(targetUserId: String) {
        val roomId = currentRoomId ?: return
        val myUid = currentUserId ?: return
        val token = currentAuthToken

        controllerScope.launch(Dispatchers.IO) {
            signaling.inviteSpeaker(roomId, targetUserId, myUid, token)
        }
    }

    fun muteSpeaker(targetUserId: String, muted: Boolean) {
        val roomId = currentRoomId ?: return
        val token = currentAuthToken

        controllerScope.launch(Dispatchers.IO) {
            signaling.updateMuteState(roomId, targetUserId, muted, token)
        }
    }

    fun kickSpeaker(targetUserId: String) {
        val roomId = currentRoomId ?: return
        val token = currentAuthToken

        controllerScope.launch(Dispatchers.IO) {
            signaling.kickSpeaker(roomId, targetUserId, token)
        }
    }

    fun retryConnection() {
        audioEngine.triggerReconnection()
    }
}
