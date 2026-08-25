package app.lovable.giant.media

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class AudioPlaybackState(
    val currentUrl: String? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val currentPositionMs: Long = 0,
    val durationMs: Long = 0,
    val error: String? = null
)

class GiantAudioPlayer private constructor(private val context: Context) {

    companion object {
        @Volatile
        private var instance: GiantAudioPlayer? = null

        fun getInstance(context: Context): GiantAudioPlayer {
            return instance ?: synchronized(this) {
                instance ?: GiantAudioPlayer(context.applicationContext).also { instance = it }
            }
        }
    }

    private var exoPlayer: ExoPlayer? = null
    private val scope = CoroutineScope(Dispatchers.Main)
    private var progressJob: Job? = null

    private val _playbackState = MutableStateFlow(AudioPlaybackState())
    val playbackState: StateFlow<AudioPlaybackState> = _playbackState.asStateFlow()

    init {
        initPlayer()
    }

    private fun initPlayer() {
        try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build()

            exoPlayer = ExoPlayer.Builder(context)
                .setAudioAttributes(audioAttributes, true)
                .setHandleAudioBecomingNoisy(true)
                .build().apply {
                    addListener(object : Player.Listener {
                        override fun onIsPlayingChanged(isPlaying: Boolean) {
                            _playbackState.value = _playbackState.value.copy(isPlaying = isPlaying)
                            if (isPlaying) startProgressTracker() else stopProgressTracker()
                        }

                        override fun onPlaybackStateChanged(playbackState: Int) {
                            when (playbackState) {
                                Player.STATE_BUFFERING -> {
                                    _playbackState.value = _playbackState.value.copy(isBuffering = true)
                                }
                                Player.STATE_READY -> {
                                    val dur = exoPlayer?.duration?.takeIf { it > 0 } ?: 0L
                                    _playbackState.value = _playbackState.value.copy(
                                        isBuffering = false,
                                        durationMs = dur
                                    )
                                }
                                Player.STATE_ENDED -> {
                                    _playbackState.value = _playbackState.value.copy(
                                        isPlaying = false,
                                        isBuffering = false,
                                        currentPositionMs = 0
                                    )
                                    stopProgressTracker()
                                }
                                Player.STATE_IDLE -> {
                                    _playbackState.value = _playbackState.value.copy(isBuffering = false)
                                }
                            }
                        }

                        override fun onPlayerError(error: PlaybackException) {
                            Log.e("GiantAudioPlayer", "Playback error: ${error.message}", error)
                            _playbackState.value = _playbackState.value.copy(
                                isPlaying = false,
                                isBuffering = false,
                                error = error.message ?: "خطأ في تشغيل الصوت"
                            )
                            stopProgressTracker()
                        }
                    })
                }
        } catch (e: Exception) {
            Log.e("GiantAudioPlayer", "Failed to init ExoPlayer", e)
        }
    }

    fun play(url: String, startPositionMs: Long = 0) {
        if (url.isBlank()) return
        val player = exoPlayer ?: return

        try {
            if (_playbackState.value.currentUrl == url) {
                if (!player.isPlaying) {
                    player.play()
                }
                return
            }

            _playbackState.value = AudioPlaybackState(
                currentUrl = url,
                isPlaying = false,
                isBuffering = true,
                currentPositionMs = startPositionMs
            )

            val mediaItem = MediaItem.fromUri(Uri.parse(url))
            player.setMediaItem(mediaItem, startPositionMs)
            player.prepare()
            player.play()
        } catch (e: Exception) {
            Log.e("GiantAudioPlayer", "Error starting playback for $url", e)
        }
    }

    fun pause() {
        exoPlayer?.pause()
    }

    fun resume() {
        exoPlayer?.play()
    }

    fun togglePlayPause(url: String? = null) {
        val player = exoPlayer ?: return
        if (url != null && _playbackState.value.currentUrl != url) {
            play(url)
        } else {
            if (player.isPlaying) pause() else resume()
        }
    }

    fun seekTo(positionMs: Long) {
        val player = exoPlayer ?: return
        val validPos = positionMs.coerceAtLeast(0L)
        player.seekTo(validPos)
        _playbackState.value = _playbackState.value.copy(currentPositionMs = validPos)
    }

    fun stop() {
        exoPlayer?.stop()
        exoPlayer?.clearMediaItems()
        _playbackState.value = AudioPlaybackState()
        stopProgressTracker()
    }

    fun setVolume(volume: Float) {
        exoPlayer?.volume = volume.coerceIn(0f, 1f)
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                exoPlayer?.let { p ->
                    val pos = p.currentPosition
                    val dur = p.duration.takeIf { it > 0 } ?: _playbackState.value.durationMs
                    _playbackState.value = _playbackState.value.copy(
                        currentPositionMs = pos,
                        durationMs = dur
                    )
                }
                delay(250)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
    }

    fun release() {
        stopProgressTracker()
        exoPlayer?.release()
        exoPlayer = null
        instance = null
    }
}
