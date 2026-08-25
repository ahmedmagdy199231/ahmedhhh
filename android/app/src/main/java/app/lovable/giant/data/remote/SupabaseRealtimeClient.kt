package app.lovable.giant.data.remote

import android.util.Log
import app.lovable.giant.data.SupabaseConfig
import app.lovable.giant.data.models.DirectMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.net.URI
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class SupabaseRealtimeClient(
    private val host: String = SupabaseConfig.SUPABASE_URL,
    private val apiKey: String = SupabaseConfig.SUPABASE_ANON_KEY
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val okHttpClient = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(25, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private var heartbeatJob: Job? = null
    private val refCounter = AtomicInteger(1)

    private val _incomingDirectMessages = MutableSharedFlow<DirectMessage>(extraBufferCapacity = 64)
    val incomingDirectMessages: SharedFlow<DirectMessage> = _incomingDirectMessages.asSharedFlow()

    private var isConnected = false

    fun connect() {
        if (isConnected && webSocket != null) return

        try {
            val uri = URI(host)
            val wsHost = uri.host
            val wsUrl = "wss://$wsHost/realtime/v1/websocket?apikey=$apiKey&vsn=1.0.0"

            val request = Request.Builder()
                .url(wsUrl)
                .build()

            webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    isConnected = true
                    Log.d("SupabaseRealtime", "Connected to Supabase Realtime WebSocket")
                    startHeartbeat()
                    joinDirectMessagesChannel()
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    handleMessage(text)
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    isConnected = false
                    heartbeatJob?.cancel()
                    Log.d("SupabaseRealtime", "WebSocket closed: $code $reason")
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    isConnected = false
                    heartbeatJob?.cancel()
                    Log.e("SupabaseRealtime", "WebSocket failure", t)
                    // Schedule auto-reconnect
                    scope.launch {
                        delay(5000)
                        if (!isConnected) {
                            connect()
                        }
                    }
                }
            })
        } catch (e: Exception) {
            Log.e("SupabaseRealtime", "Error connecting to realtime", e)
        }
    }

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isActive && isConnected) {
                delay(25000)
                sendHeartbeat()
            }
        }
    }

    private fun sendHeartbeat() {
        val ref = refCounter.incrementAndGet().toString()
        val json = JSONObject().apply {
            put("topic", "phoenix")
            put("event", "heartbeat")
            put("payload", JSONObject())
            put("ref", ref)
        }
        webSocket?.send(json.toString())
    }

    private fun joinDirectMessagesChannel() {
        val ref = refCounter.incrementAndGet().toString()
        val config = JSONObject().apply {
            val postgresChanges = org.json.JSONArray().apply {
                put(JSONObject().apply {
                    put("event", "*")
                    put("schema", "public")
                    put("table", "direct_messages")
                })
            }
            put("postgres_changes", postgresChanges)
        }

        val payload = JSONObject().apply {
            put("config", config)
        }

        val joinMsg = JSONObject().apply {
            put("topic", "realtime:public:direct_messages")
            put("event", "phx_join")
            put("payload", payload)
            put("ref", ref)
        }

        webSocket?.send(joinMsg.toString())
        Log.d("SupabaseRealtime", "Joined direct_messages channel")
    }

    private fun handleMessage(text: String) {
        try {
            val json = JSONObject(text)
            val event = json.optString("event")
            val payload = json.optJSONObject("payload") ?: return

            if (event == "postgres_changes") {
                val data = payload.optJSONObject("data")
                val record = data?.optJSONObject("record")
                if (record != null) {
                    val dm = DirectMessage(
                        id = record.getString("id"),
                        senderId = record.getString("sender_id"),
                        receiverId = record.getString("receiver_id"),
                        content = record.optString("content", ""),
                        createdAt = record.optString("created_at", ""),
                        messageType = record.optString("message_type", "text"),
                        mediaUrl = record.optString("media_url", null),
                        readAt = record.optString("read_at", null),
                        deliveredAt = record.optString("delivered_at", null),
                        replyToId = record.optString("reply_to_id", null)
                    )
                    scope.launch {
                        _incomingDirectMessages.emit(dm)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SupabaseRealtime", "Error parsing realtime message: $text", e)
        }
    }

    fun disconnect() {
        isConnected = false
        heartbeatJob?.cancel()
        webSocket?.close(1000, "Normal closure")
        webSocket = null
    }

    companion object {
        val instance: SupabaseRealtimeClient by lazy { SupabaseRealtimeClient() }
    }
}
