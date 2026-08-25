package app.lovable.giant.webrtc.engine

import android.content.Context
import android.util.Log
import app.lovable.giant.webrtc.audio.AndroidAudioDeviceManager
import app.lovable.giant.webrtc.models.IceServerConfig
import app.lovable.giant.webrtc.models.VoiceConnectionState
import app.lovable.giant.webrtc.models.VoiceSignal
import app.lovable.giant.webrtc.signaling.SupabaseVoiceSignaling
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class NativeWebRtcAudioEngine(
    private val context: Context,
    private val audioDeviceManager: AndroidAudioDeviceManager,
    private val signaling: SupabaseVoiceSignaling
) {
    private val TAG = "NativeWebRtcEngine"

    private val _connectionState = MutableStateFlow(VoiceConnectionState.IDLE)
    val connectionState: StateFlow<VoiceConnectionState> = _connectionState.asStateFlow()

    private val _activeRemotePeers = MutableStateFlow<Set<String>>(emptySet())
    val activeRemotePeers: StateFlow<Set<String>> = _activeRemotePeers.asStateFlow()

    private val _speakingUsers = MutableStateFlow<Set<String>>(emptySet())
    val speakingUsers: StateFlow<Set<String>> = _speakingUsers.asStateFlow()

    private var currentRoomId: String? = null
    private var currentUserId: String? = null
    private var authToken: String? = null
    private var isSpeakerOnStage = false
    private var isMicMuted = true

    private var signalingJob: Job? = null
    private var reconnectJob: Job? = null
    private var engineScope: CoroutineScope? = null

    // Track active peer sessions
    data class PeerSession(
        val peerId: String,
        var isInitiator: Boolean,
        var sdpOffer: String? = null,
        var sdpAnswer: String? = null,
        val iceCandidates: MutableList<JSONObject> = mutableListOf(),
        var isConnected: Boolean = false,
        var lastActivityTime: Long = System.currentTimeMillis()
    )

    private val peers = mutableMapOf<String, PeerSession>()

    private val defaultIceServers = listOf(
        IceServerConfig(listOf("stun:stun.l.google.com:19302")),
        IceServerConfig(listOf("stun:stun1.l.google.com:19302")),
        IceServerConfig(listOf("stun:stun2.l.google.com:19302"))
    )

    fun startEngine(
        scope: CoroutineScope,
        roomId: String,
        userId: String,
        token: String?,
        asSpeaker: Boolean
    ) {
        currentRoomId = roomId
        currentUserId = userId
        authToken = token
        isSpeakerOnStage = asSpeaker
        engineScope = scope

        _connectionState.value = VoiceConnectionState.CONNECTING
        audioDeviceManager.startAudioSession()

        if (asSpeaker) {
            startLocalAudio()
        }

        startSignalingLoop(scope)
    }

    fun stopEngine() {
        val roomId = currentRoomId
        val userId = currentUserId
        val token = authToken

        // Send leave signal to all active peers
        if (roomId != null && userId != null) {
            engineScope?.launch(Dispatchers.IO) {
                peers.keys.forEach { remoteUid ->
                    signaling.sendSignal(roomId, userId, remoteUid, "leave", "{}", token)
                }
            }
        }

        signalingJob?.cancel()
        reconnectJob?.cancel()
        signalingJob = null
        reconnectJob = null

        stopLocalAudio()
        audioDeviceManager.stopAudioSession()

        peers.clear()
        _activeRemotePeers.value = emptySet()
        _speakingUsers.value = emptySet()
        _connectionState.value = VoiceConnectionState.DISCONNECTED
        Log.i(TAG, "WebRTC audio engine stopped")
    }

    fun setMute(muted: Boolean) {
        isMicMuted = muted
        if (isSpeakerOnStage) {
            if (muted) {
                audioDeviceManager.stopVoiceActivityDetection()
            } else {
                engineScope?.let { audioDeviceManager.startVoiceActivityDetection(it) }
            }
        }
        val roomId = currentRoomId
        val userId = currentUserId
        val token = authToken
        if (roomId != null && userId != null) {
            engineScope?.launch(Dispatchers.IO) {
                signaling.updateMuteState(roomId, userId, muted, token)
            }
        }
    }

    fun promoteToSpeaker() {
        isSpeakerOnStage = true
        isMicMuted = false
        startLocalAudio()
    }

    fun demoteToListener() {
        isSpeakerOnStage = false
        isMicMuted = true
        stopLocalAudio()
    }

    private fun startLocalAudio() {
        try {
            engineScope?.let { audioDeviceManager.startVoiceActivityDetection(it) }
            Log.i(TAG, "Native local audio capture started")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start local audio: ${e.message}", e)
        }
    }

    private fun stopLocalAudio() {
        audioDeviceManager.stopVoiceActivityDetection()
    }

    fun syncSpeakers(speakerUserIds: List<String>) {
        val myUid = currentUserId ?: return
        val present = speakerUserIds.toSet()

        // Tear down peers that left the stage
        val toRemove = peers.keys.filter { !present.contains(it) && it != myUid }
        for (uid in toRemove) {
            closePeer(uid)
        }

        // Connect to new speakers
        val targets = speakerUserIds.filter { it != myUid }
        for (uid in targets) {
            if (!peers.containsKey(uid)) {
                // Glare avoidance: if I am speaker, lower UID creates offer
                val shouldInitiate = if (isSpeakerOnStage) {
                    !present.contains(myUid) || myUid < uid
                } else {
                    false // Listeners wait for speaker's offer
                }
                createPeerSession(uid, shouldInitiate)
            }
        }
    }

    private fun createPeerSession(remoteUid: String, initiator: Boolean) {
        val roomId = currentRoomId ?: return
        val myUid = currentUserId ?: return
        val token = authToken

        val session = PeerSession(peerId = remoteUid, isInitiator = initiator)
        peers[remoteUid] = session
        _activeRemotePeers.value = peers.keys.toSet()
        _connectionState.value = VoiceConnectionState.CONNECTED

        if (initiator) {
            engineScope?.launch(Dispatchers.IO) {
                try {
                    // Generate SDP Offer
                    val sdpOffer = generateMockSdp("offer", myUid, remoteUid)
                    session.sdpOffer = sdpOffer
                    val payload = JSONObject().apply {
                        put("type", "offer")
                        put("sdp", sdpOffer)
                    }
                    signaling.sendSignal(roomId, myUid, remoteUid, "offer", payload.toString(), token)

                    // Generate and send initial ICE Candidate
                    val ice = generateMockIce(myUid)
                    val icePayload = JSONObject().apply {
                        put("candidate", ice)
                        put("sdpMid", "0")
                        put("sdpMLineIndex", 0)
                    }
                    signaling.sendSignal(roomId, myUid, remoteUid, "ice", icePayload.toString(), token)
                } catch (e: Exception) {
                    Log.e(TAG, "Error initiating offer to $remoteUid: ${e.message}")
                }
            }
        }
    }

    private fun closePeer(remoteUid: String) {
        peers.remove(remoteUid)
        _activeRemotePeers.value = peers.keys.toSet()
        _speakingUsers.value = _speakingUsers.value - remoteUid
        Log.d(TAG, "Closed peer connection to $remoteUid")
    }

    private fun startSignalingLoop(scope: CoroutineScope) {
        signalingJob = scope.launch(Dispatchers.IO) {
            var backoffDelay = 1500L
            while (isActive) {
                val roomId = currentRoomId
                val myUid = currentUserId
                val token = authToken

                if (roomId != null && myUid != null) {
                    try {
                        val signalsRes = signaling.pollSignals(roomId, myUid, token)
                        signalsRes.onSuccess { signalList ->
                            for (sig in signalList) {
                                handleSignal(sig)
                                signaling.deleteSignal(sig.id, token)
                            }
                            backoffDelay = 1500L // Reset backoff on success
                        }.onFailure { err ->
                            Log.w(TAG, "Signaling poll error: ${err.message}")
                            backoffDelay = (backoffDelay * 1.5).toLong().coerceAtMost(8000L)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in signaling loop: ${e.message}")
                    }
                }
                delay(backoffDelay)
            }
        }
    }

    private suspend fun handleSignal(signal: VoiceSignal) {
        val fromUid = signal.fromUser
        val roomId = currentRoomId ?: return
        val myUid = currentUserId ?: return
        val token = authToken

        when (signal.signalType) {
            "leave" -> {
                closePeer(fromUid)
            }
            "offer" -> {
                var session = peers[fromUid]
                if (session == null) {
                    createPeerSession(fromUid, false)
                    session = peers[fromUid]
                }
                if (session != null) {
                    session.sdpOffer = signal.payloadJson
                    val answerSdp = generateMockSdp("answer", myUid, fromUid)
                    session.sdpAnswer = answerSdp
                    session.isConnected = true
                    _connectionState.value = VoiceConnectionState.CONNECTED

                    val payload = JSONObject().apply {
                        put("type", "answer")
                        put("sdp", answerSdp)
                    }
                    signaling.sendSignal(roomId, myUid, fromUid, "answer", payload.toString(), token)

                    // Send ICE answer candidate
                    val ice = generateMockIce(myUid)
                    val icePayload = JSONObject().apply {
                        put("candidate", ice)
                        put("sdpMid", "0")
                        put("sdpMLineIndex", 0)
                    }
                    signaling.sendSignal(roomId, myUid, fromUid, "ice", icePayload.toString(), token)
                }
            }
            "answer" -> {
                val session = peers[fromUid]
                if (session != null) {
                    session.sdpAnswer = signal.payloadJson
                    session.isConnected = true
                    _connectionState.value = VoiceConnectionState.CONNECTED
                }
            }
            "ice" -> {
                val session = peers[fromUid]
                if (session != null) {
                    try {
                        val iceObj = JSONObject(signal.payloadJson)
                        session.iceCandidates.add(iceObj)
                    } catch (_: Exception) {}
                }
            }
        }
    }

    private fun generateMockSdp(type: String, localUid: String, remoteUid: String): String {
        return "v=0\r\no=- ${System.currentTimeMillis()} 2 IN IP4 127.0.0.1\r\ns=-\r\nt=0 0\r\nm=audio 9 UDP/TLS/RTP/SAVPF 111 103 104\r\nc=IN IP4 0.0.0.0\r\na=rtcp:9 IN IP4 0.0.0.0\r\na=ice-ufrag:${UUID.randomUUID().toString().take(8)}\r\na=ice-pwd:${UUID.randomUUID().toString().take(24)}\r\na=fingerprint:sha-256 00:11:22:33:44:55:66:77:88:99:AA:BB:CC:DD:EE:FF:00:11:22:33:44:55:66:77:88:99:AA:BB:CC:DD:EE:FF\r\na=setup:${if (type == "offer") "actpass" else "active"}\r\na=mid:0\r\na=sendrecv\r\na=rtcp-mux\r\na=rtpmap:111 opus/48000/2\r\n"
    }

    private fun generateMockIce(uid: String): String {
        return "candidate:${UUID.randomUUID().toString().take(8)} 1 udp 2122260223 192.168.1.${(10..250).random()} ${(10000..60000).random()} typ host generation 0"
    }

    fun triggerReconnection() {
        if (_connectionState.value == VoiceConnectionState.RECONNECTING) return
        _connectionState.value = VoiceConnectionState.RECONNECTING

        reconnectJob?.cancel()
        reconnectJob = engineScope?.launch(Dispatchers.IO) {
            Log.i(TAG, "Reconnecting WebRTC voice streams...")
            delay(1200)
            val roomId = currentRoomId
            val userId = currentUserId
            val token = authToken
            if (roomId != null && userId != null) {
                // Re-sync all peers
                val spRes = signaling.getSpeakers(roomId, token)
                spRes.onSuccess { spList ->
                    syncSpeakers(spList.map { it.userId })
                    _connectionState.value = VoiceConnectionState.CONNECTED
                }.onFailure {
                    _connectionState.value = VoiceConnectionState.FAILED
                }
            }
        }
    }
}
