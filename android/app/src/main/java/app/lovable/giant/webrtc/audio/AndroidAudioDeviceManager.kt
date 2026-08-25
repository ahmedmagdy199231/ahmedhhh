package app.lovable.giant.webrtc.audio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import app.lovable.giant.webrtc.models.AudioRoute
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sqrt

class AndroidAudioDeviceManager(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null
    private var previousAudioMode: Int = AudioManager.MODE_NORMAL
    private var previousSpeakerphoneState: Boolean = false

    private val _currentRoute = MutableStateFlow(AudioRoute.SPEAKER)
    val currentRoute: StateFlow<AudioRoute> = _currentRoute.asStateFlow()

    private val _isLocalSpeaking = MutableStateFlow(false)
    val isLocalSpeaking: StateFlow<Boolean> = _isLocalSpeaking.asStateFlow()

    private val _localAudioLevel = MutableStateFlow(0f)
    val localAudioLevel: StateFlow<Float> = _localAudioLevel.asStateFlow()

    private var vadJob: Job? = null
    private var audioRecord: AudioRecord? = null
    private var isRecording = false

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                Log.w("AudioDeviceManager", "Audio focus lost permanently")
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                Log.w("AudioDeviceManager", "Audio focus lost transiently")
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                Log.d("AudioDeviceManager", "Audio focus gained")
            }
        }
    }

    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                // Headset unplugged, fallback to earpiece/speaker
                setSpeakerphone(true)
            }
        }
    }

    private val headsetReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_HEADSET_PLUG) {
                val state = intent.getIntExtra("state", -1)
                if (state == 1) {
                    _currentRoute.value = AudioRoute.WIRED_HEADSET
                    audioManager.isSpeakerphoneOn = false
                } else if (state == 0) {
                    _currentRoute.value = if (audioManager.isSpeakerphoneOn) AudioRoute.SPEAKER else AudioRoute.EARPIECE
                }
            }
        }
    }

    fun startAudioSession() {
        try {
            previousAudioMode = audioManager.mode
            previousSpeakerphoneState = audioManager.isSpeakerphoneOn

            // Request Audio Focus
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val playbackAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()

                val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(playbackAttributes)
                    .setAcceptsDelayedFocusGain(true)
                    .setOnAudioFocusChangeListener(audioFocusChangeListener)
                    .build()

                audioFocusRequest = request
                audioManager.requestAudioFocus(request)
            } else {
                @Suppress("DEPRECATION")
                audioManager.requestAudioFocus(
                    audioFocusChangeListener,
                    AudioManager.STREAM_VOICE_CALL,
                    AudioManager.AUDIOFOCUS_GAIN
                )
            }

            // Set communication mode
            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
            setSpeakerphone(true)

            // Register receivers
            val noisyFilter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
            context.registerReceiver(noisyReceiver, noisyFilter)

            val headsetFilter = IntentFilter(Intent.ACTION_HEADSET_PLUG)
            context.registerReceiver(headsetReceiver, headsetFilter)

            Log.i("AudioDeviceManager", "Audio session started successfully in communication mode")
        } catch (e: Exception) {
            Log.e("AudioDeviceManager", "Failed to start audio session: ${e.message}", e)
        }
    }

    fun stopAudioSession() {
        stopVoiceActivityDetection()
        try {
            context.unregisterReceiver(noisyReceiver)
        } catch (_: Exception) {}

        try {
            context.unregisterReceiver(headsetReceiver)
        } catch (_: Exception) {}

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
            } else {
                @Suppress("DEPRECATION")
                audioManager.abandonAudioFocus(audioFocusChangeListener)
            }

            audioManager.isSpeakerphoneOn = previousSpeakerphoneState
            audioManager.mode = previousAudioMode
            Log.i("AudioDeviceManager", "Audio session stopped and restored")
        } catch (e: Exception) {
            Log.e("AudioDeviceManager", "Error stopping audio session: ${e.message}", e)
        }
    }

    fun setSpeakerphone(enable: Boolean) {
        try {
            audioManager.isSpeakerphoneOn = enable
            _currentRoute.value = if (enable) AudioRoute.SPEAKER else AudioRoute.EARPIECE
            Log.d("AudioDeviceManager", "Speakerphone set to: $enable (route: ${_currentRoute.value})")
        } catch (e: Exception) {
            Log.e("AudioDeviceManager", "Error toggling speaker: ${e.message}", e)
        }
    }

    fun toggleSpeakerphone(): Boolean {
        val newState = !audioManager.isSpeakerphoneOn
        setSpeakerphone(newState)
        return newState
    }

    fun startVoiceActivityDetection(scope: CoroutineScope) {
        if (isRecording) return
        val sampleRate = 16000
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

        if (bufferSize <= 0) {
            Log.w("AudioDeviceManager", "Invalid buffer size for VAD: $bufferSize")
            return
        }

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.w("AudioDeviceManager", "AudioRecord not initialized")
                return
            }

            audioRecord?.startRecording()
            isRecording = true

            vadJob = scope.launch(Dispatchers.IO) {
                val buffer = ShortArray(bufferSize / 2)
                var lastSpokeTime = 0L
                val holdMs = 450L
                val speakRmsThreshold = 600.0 // RMS threshold for voice

                while (isActive && isRecording) {
                    val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (read > 0) {
                        var sum = 0.0
                        for (i in 0 until read) {
                            val sample = buffer[i]
                            sum += sample * sample
                        }
                        val rms = sqrt(sum / read)
                        val normalized = (rms / 32767.0).toFloat().coerceIn(0f, 1f)
                        _localAudioLevel.value = normalized

                        val now = System.currentTimeMillis()
                        if (rms > speakRmsThreshold) {
                            lastSpokeTime = now
                        }

                        val speaking = (now - lastSpokeTime) < holdMs
                        if (_isLocalSpeaking.value != speaking) {
                            _isLocalSpeaking.value = speaking
                        }
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.e("AudioDeviceManager", "Missing RECORD_AUDIO permission for VAD: ${e.message}")
        } catch (e: Exception) {
            Log.e("AudioDeviceManager", "Error starting VAD: ${e.message}", e)
        }
    }

    fun stopVoiceActivityDetection() {
        isRecording = false
        vadJob?.cancel()
        vadJob = null
        try {
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
        } catch (e: Exception) {
            Log.e("AudioDeviceManager", "Error releasing AudioRecord: ${e.message}")
        }
        _isLocalSpeaking.value = false
        _localAudioLevel.value = 0f
    }
}
