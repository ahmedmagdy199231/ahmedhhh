package app.lovable.giant.media

import android.content.Context
import android.util.Log
import app.lovable.giant.GiantApplication
import app.lovable.giant.data.models.MusicTrackModel
import app.lovable.giant.data.models.RoomMusicModel
import app.lovable.giant.data.models.TrackResultModel
import app.lovable.giant.data.remote.SupabaseRestClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class RoomMusicUiState(
    val music: RoomMusicModel? = null,
    val isSearching: Boolean = false,
    val isMuted: Boolean = false,
    val isLocked: Boolean = false,
    val localPlaybackPosMs: Long = 0,
    val error: String? = null
)

class NativeRoomMusicController(
    private val roomId: String,
    private val context: Context
) {
    private val restClient = SupabaseRestClient()
    private val sessionRepo = GiantApplication.instance.sessionRepository
    private val audioPlayer = GiantAudioPlayer.getInstance(context)
    private val scope = CoroutineScope(Dispatchers.Main)

    private val _uiState = MutableStateFlow(RoomMusicUiState())
    val uiState: StateFlow<RoomMusicUiState> = _uiState.asStateFlow()

    private var pollJob: Job? = null
    private var syncJob: Job? = null
    private var lastLoadedUrl: String? = null

    init {
        startPollingMusic()
        observeAudioPlayer()
    }

    private fun observeAudioPlayer() {
        scope.launch {
            audioPlayer.playbackState.collect { playState ->
                _uiState.value = _uiState.value.copy(
                    localPlaybackPosMs = playState.currentPositionMs
                )
            }
        }
    }

    private fun startPollingMusic() {
        pollJob?.cancel()
        pollJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                fetchRoomMusic()
                delay(3000)
            }
        }
    }

    private suspend fun fetchRoomMusic() {
        val session = sessionRepo.loadSession()
        val token = session?.accessToken
        val res = restClient.getRoomMusic(roomId, token)
        res.onSuccess { m ->
            _uiState.value = _uiState.value.copy(music = m)
            syncAudioPlayback(m)
        }.onFailure { err ->
            Log.e("RoomMusicController", "Failed to fetch room music", err)
        }
    }

    private fun syncAudioPlayback(music: RoomMusicModel?) {
        val track = music?.current
        if (track == null || track.previewUrl.isBlank()) {
            if (lastLoadedUrl != null) {
                audioPlayer.stop()
                lastLoadedUrl = null
            }
            return
        }

        val url = track.previewUrl
        val isPaused = music.paused
        val pausedPos = music.pausedPosMs

        if (lastLoadedUrl != url) {
            lastLoadedUrl = url
            if (!isPaused) {
                audioPlayer.play(url, pausedPos)
            }
        } else {
            if (isPaused) {
                audioPlayer.pause()
            } else {
                if (!audioPlayer.playbackState.value.isPlaying) {
                    audioPlayer.resume()
                }
            }
        }

        // Adjust volume
        val targetVolume = if (_uiState.value.isMuted) 0f else (music.volume / 100f).coerceIn(0f, 1f)
        audioPlayer.setVolume(targetVolume)
    }

    fun searchAndPlay(query: String, onComplete: (Boolean) -> Unit) {
        if (query.isBlank()) return
        val session = sessionRepo.loadSession() ?: return
        val token = session.accessToken ?: return

        scope.launch {
            _uiState.value = _uiState.value.copy(isSearching = true)
            val searchRes = restClient.searchTrack(query)
            _uiState.value = _uiState.value.copy(isSearching = false)

            searchRes.onSuccess { trackResult ->
                if (trackResult != null) {
                    val track = MusicTrackModel(
                        videoId = trackResult.videoId,
                        title = trackResult.title,
                        artist = trackResult.artist,
                        artwork = trackResult.artwork,
                        previewUrl = trackResult.previewUrl,
                        durationMs = trackResult.durationMs,
                        requesterName = session.username ?: "عضو",
                        requesterId = session.userId
                    )
                    val playRes = restClient.musicPlay(roomId, track, token)
                    playRes.onSuccess {
                        fetchRoomMusic()
                        onComplete(true)
                    }.onFailure {
                        onComplete(false)
                    }
                } else {
                    onComplete(false)
                }
            }.onFailure {
                onComplete(false)
            }
        }
    }

    fun togglePlayPause() {
        val session = sessionRepo.loadSession() ?: return
        val token = session.accessToken ?: return
        val cur = _uiState.value.music ?: return

        scope.launch {
            if (cur.paused) {
                restClient.musicResume(roomId, token)
            } else {
                restClient.musicPause(roomId, token)
            }
            fetchRoomMusic()
        }
    }

    fun seekTo(posMs: Long) {
        val session = sessionRepo.loadSession() ?: return
        val token = session.accessToken ?: return

        audioPlayer.seekTo(posMs)
        scope.launch {
            restClient.musicSeek(roomId, posMs, token)
            fetchRoomMusic()
        }
    }

    fun skipTrack() {
        val session = sessionRepo.loadSession() ?: return
        val token = session.accessToken ?: return

        scope.launch {
            restClient.musicSkip(roomId, token)
            fetchRoomMusic()
        }
    }

    fun stopTrack() {
        val session = sessionRepo.loadSession() ?: return
        val token = session.accessToken ?: return

        audioPlayer.stop()
        lastLoadedUrl = null
        scope.launch {
            restClient.musicStop(roomId, token)
            fetchRoomMusic()
        }
    }

    fun setVolume(volumePercent: Int) {
        val session = sessionRepo.loadSession() ?: return
        val token = session.accessToken ?: return

        val clamped = volumePercent.coerceIn(0, 100)
        audioPlayer.setVolume(clamped / 100f)
        scope.launch {
            restClient.musicSetVolume(roomId, clamped, token)
        }
    }

    fun toggleMute() {
        val newMute = !_uiState.value.isMuted
        _uiState.value = _uiState.value.copy(isMuted = newMute)
        val vol = if (newMute) 0f else ((_uiState.value.music?.volume ?: 70) / 100f)
        audioPlayer.setVolume(vol)
    }

    fun toggleLock() {
        _uiState.value = _uiState.value.copy(isLocked = !_uiState.value.isLocked)
    }

    fun release() {
        pollJob?.cancel()
        syncJob?.cancel()
        audioPlayer.stop()
    }
}
