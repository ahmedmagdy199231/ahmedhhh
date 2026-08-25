package app.lovable.giant.webrtc

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import app.lovable.giant.MainActivity

class RoomVoiceForegroundService : Service() {

    companion object {
        const val CHANNEL_ID = "giant_voice_room_channel"
        const val NOTIFICATION_ID = 9012

        const val ACTION_START = "app.lovable.giant.action.START_VOICE_SERVICE"
        const val ACTION_STOP = "app.lovable.giant.action.STOP_VOICE_SERVICE"
        const val ACTION_UPDATE_NOTIFICATION = "app.lovable.giant.action.UPDATE_VOICE_NOTIFICATION"
        const val ACTION_TOGGLE_MUTE = "app.lovable.giant.action.TOGGLE_MUTE_FROM_NOTIFICATION"
        const val ACTION_LEAVE = "app.lovable.giant.action.LEAVE_FROM_NOTIFICATION"

        const val EXTRA_ROOM_ID = "extra_room_id"
        const val EXTRA_ROOM_NAME = "extra_room_name"
        const val EXTRA_IS_SPEAKER = "extra_is_speaker"
        const val EXTRA_IS_MUTED = "extra_is_muted"

        fun startService(context: Context, roomId: String, roomName: String, isSpeaker: Boolean, isMuted: Boolean) {
            val intent = Intent(context, RoomVoiceForegroundService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_ROOM_ID, roomId)
                putExtra(EXTRA_ROOM_NAME, roomName)
                putExtra(EXTRA_IS_SPEAKER, isSpeaker)
                putExtra(EXTRA_IS_MUTED, isMuted)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, RoomVoiceForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun updateNotification(context: Context, roomName: String, isSpeaker: Boolean, isMuted: Boolean) {
            val intent = Intent(context, RoomVoiceForegroundService::class.java).apply {
                action = ACTION_UPDATE_NOTIFICATION
                putExtra(EXTRA_ROOM_NAME, roomName)
                putExtra(EXTRA_IS_SPEAKER, isSpeaker)
                putExtra(EXTRA_IS_MUTED, isMuted)
            }
            context.startService(intent)
        }
    }

    private var wakeLock: PowerManager.WakeLock? = null
    private var currentRoomName: String = "الغرفة الصوتية"
    private var currentIsSpeaker: Boolean = false
    private var currentIsMuted: Boolean = true

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        acquireWakeLock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                currentRoomName = intent.getStringExtra(EXTRA_ROOM_NAME) ?: "الغرفة الصوتية"
                currentIsSpeaker = intent.getBooleanExtra(EXTRA_IS_SPEAKER, false)
                currentIsMuted = intent.getBooleanExtra(EXTRA_IS_MUTED, true)

                val notification = buildNotification(currentRoomName, currentIsSpeaker, currentIsMuted)
                val serviceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    if (currentIsSpeaker) {
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE or ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                    } else {
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                    }
                } else {
                    0
                }

                try {
                    ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, serviceType)
                } catch (e: Exception) {
                    Log.e("RoomVoiceService", "Error starting foreground service: ${e.message}")
                    startForeground(NOTIFICATION_ID, notification)
                }
            }
            ACTION_UPDATE_NOTIFICATION -> {
                currentRoomName = intent.getStringExtra(EXTRA_ROOM_NAME) ?: currentRoomName
                currentIsSpeaker = intent.getBooleanExtra(EXTRA_IS_SPEAKER, currentIsSpeaker)
                currentIsMuted = intent.getBooleanExtra(EXTRA_IS_MUTED, currentIsMuted)

                val notification = buildNotification(currentRoomName, currentIsSpeaker, currentIsMuted)
                val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.notify(NOTIFICATION_ID, notification)
            }
            ACTION_TOGGLE_MUTE -> {
                NativeVoiceRoomController.getInstance(applicationContext).toggleMic()
            }
            ACTION_LEAVE -> {
                NativeVoiceRoomController.getInstance(applicationContext).leaveRoom()
                stopSelf()
            }
            ACTION_STOP -> {
                releaseWakeLock()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        releaseWakeLock()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "الغرف الصوتية المباشرة",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "استمرار تشغيل الصوت في الخلفية أثناء التواجد في الغرفة الصوتية"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(roomName: String, isSpeaker: Boolean, isMuted: Boolean): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val muteIntent = Intent(this, RoomVoiceForegroundService::class.java).apply {
            action = ACTION_TOGGLE_MUTE
        }
        val mutePendingIntent = PendingIntent.getService(
            this,
            1,
            muteIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val leaveIntent = Intent(this, RoomVoiceForegroundService::class.java).apply {
            action = ACTION_LEAVE
        }
        val leavePendingIntent = PendingIntent.getService(
            this,
            2,
            leaveIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val statusText = when {
            isSpeaker && !isMuted -> "متحدث في المنصة (الميكروفون نشط)"
            isSpeaker && isMuted -> "متحدث في المنصة (الميكروفون مكتوم)"
            else -> "مستمع في البث المباشر"
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(roomName)
            .setContentText(statusText)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(openPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)

        if (isSpeaker) {
            val muteLabel = if (isMuted) "فتح المايك" else "كتم المايك"
            builder.addAction(android.R.drawable.ic_lock_silent_mode, muteLabel, mutePendingIntent)
        }

        builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "مغادرة الغرفة", leavePendingIntent)

        return builder.build()
    }

    private fun acquireWakeLock() {
        if (wakeLock == null) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Giant:RoomVoiceWakeLock").apply {
                setReferenceCounted(false)
                acquire(4 * 60 * 60 * 1000L) // 4 hours maximum safety timeout
            }
        }
    }

    private fun releaseWakeLock() {
        try {
            wakeLock?.let {
                if (it.isHeld) it.release()
            }
        } catch (_: Exception) {}
        wakeLock = null
    }
}
