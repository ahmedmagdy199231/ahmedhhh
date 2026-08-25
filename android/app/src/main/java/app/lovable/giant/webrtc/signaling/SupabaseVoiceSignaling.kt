package app.lovable.giant.webrtc.signaling

import android.util.Log
import app.lovable.giant.data.remote.SupabaseConfig
import app.lovable.giant.webrtc.models.RaisedHand
import app.lovable.giant.webrtc.models.RoomSpeaker
import app.lovable.giant.webrtc.models.SpeakerInvite
import app.lovable.giant.webrtc.models.VoiceSignal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class SupabaseVoiceSignaling {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private fun buildHeaders(token: String?): Map<String, String> {
        val headers = mutableMapOf(
            "apikey" to SupabaseConfig.ANON_KEY,
            "Content-Type" to "application/json"
        )
        if (!token.isNullOrEmpty()) {
            headers["Authorization"] = "Bearer $token"
        } else {
            headers["Authorization"] = "Bearer ${SupabaseConfig.ANON_KEY}"
        }
        return headers
    }

    suspend fun getSpeakers(roomId: String, token: String?): Result<List<RoomSpeaker>> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.BASE_URL}/rest/v1/room_speakers?room_id=eq.$roomId&select=id,room_id,user_id,is_muted,is_speaking,added_by,joined_at&order=joined_at.asc"
            val reqBuilder = Request.Builder().url(url)
            buildHeaders(token).forEach { (k, v) -> reqBuilder.addHeader(k, v) }

            val resp = client.newCall(reqBuilder.build()).execute()
            val body = resp.body?.string() ?: "[]"
            if (!resp.isSuccessful) {
                return@withContext Result.failure(Exception("Failed to fetch speakers: ${resp.code}"))
            }

            val array = JSONArray(body)
            val userIds = mutableListOf<String>()
            val rawSpeakers = mutableListOf<RoomSpeaker>()

            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val uid = obj.getString("user_id")
                userIds.add(uid)
                rawSpeakers.add(
                    RoomSpeaker(
                        id = obj.optString("id", ""),
                        roomId = obj.optString("room_id", roomId),
                        userId = uid,
                        isMuted = obj.optBoolean("is_muted", false),
                        isSpeaking = obj.optBoolean("is_speaking", false),
                        addedBy = obj.optString("added_by", null),
                        joinedAt = obj.optString("joined_at", null)
                    )
                )
            }

            // Fetch profile usernames and avatars
            val profileMap = fetchProfiles(userIds, token)
            val enrichedSpeakers = rawSpeakers.map { sp ->
                val prof = profileMap[sp.userId]
                sp.copy(username = prof?.first, avatarUrl = prof?.second)
            }

            Result.success(enrichedSpeakers)
        } catch (e: Exception) {
            Log.e("VoiceSignaling", "Error getting speakers: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getRaisedHands(roomId: String, token: String?): Result<List<RaisedHand>> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.BASE_URL}/rest/v1/room_raised_hands?room_id=eq.$roomId&select=id,room_id,user_id,created_at&order=created_at.asc"
            val reqBuilder = Request.Builder().url(url)
            buildHeaders(token).forEach { (k, v) -> reqBuilder.addHeader(k, v) }

            val resp = client.newCall(reqBuilder.build()).execute()
            val body = resp.body?.string() ?: "[]"
            if (!resp.isSuccessful) {
                return@withContext Result.failure(Exception("Failed to fetch raised hands: ${resp.code}"))
            }

            val array = JSONArray(body)
            val userIds = mutableListOf<String>()
            val rawHands = mutableListOf<RaisedHand>()

            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val uid = obj.getString("user_id")
                userIds.add(uid)
                rawHands.add(
                    RaisedHand(
                        id = obj.optString("id", ""),
                        roomId = obj.optString("room_id", roomId),
                        userId = uid,
                        createdAt = obj.optString("created_at", null)
                    )
                )
            }

            val profileMap = fetchProfiles(userIds, token)
            val enriched = rawHands.map { h ->
                val prof = profileMap[h.userId]
                h.copy(username = prof?.first, avatarUrl = prof?.second)
            }
            Result.success(enriched)
        } catch (e: Exception) {
            Log.e("VoiceSignaling", "Error getting raised hands: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getInvites(roomId: String, token: String?): Result<List<SpeakerInvite>> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.BASE_URL}/rest/v1/room_speaker_invites?room_id=eq.$roomId&select=id,room_id,user_id,invited_by,created_at"
            val reqBuilder = Request.Builder().url(url)
            buildHeaders(token).forEach { (k, v) -> reqBuilder.addHeader(k, v) }

            val resp = client.newCall(reqBuilder.build()).execute()
            val body = resp.body?.string() ?: "[]"
            if (!resp.isSuccessful) {
                return@withContext Result.failure(Exception("Failed to fetch invites: ${resp.code}"))
            }

            val array = JSONArray(body)
            val list = mutableListOf<SpeakerInvite>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    SpeakerInvite(
                        id = obj.optString("id", ""),
                        roomId = obj.optString("room_id", roomId),
                        userId = obj.getString("user_id"),
                        invitedBy = obj.optString("invited_by", ""),
                        createdAt = obj.optString("created_at", null)
                    )
                )
            }
            Result.success(list)
        } catch (e: Exception) {
            Log.e("VoiceSignaling", "Error getting invites: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun pollSignals(roomId: String, myUserId: String, token: String?): Result<List<VoiceSignal>> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.BASE_URL}/rest/v1/room_voice_signals?room_id=eq.$roomId&to_user=eq.$myUserId&select=id,room_id,from_user,to_user,signal_type,payload,created_at&order=created_at.asc"
            val reqBuilder = Request.Builder().url(url)
            buildHeaders(token).forEach { (k, v) -> reqBuilder.addHeader(k, v) }

            val resp = client.newCall(reqBuilder.build()).execute()
            val body = resp.body?.string() ?: "[]"
            if (!resp.isSuccessful) {
                return@withContext Result.failure(Exception("Failed to poll signals: ${resp.code}"))
            }

            val array = JSONArray(body)
            val list = mutableListOf<VoiceSignal>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    VoiceSignal(
                        id = obj.optString("id", ""),
                        roomId = obj.optString("room_id", roomId),
                        fromUser = obj.optString("from_user", ""),
                        toUser = obj.optString("to_user", myUserId),
                        signalType = obj.optString("signal_type", ""),
                        payloadJson = obj.opt("payload")?.toString() ?: "{}",
                        createdAt = obj.optString("created_at", null)
                    )
                )
            }
            Result.success(list)
        } catch (e: Exception) {
            Log.e("VoiceSignaling", "Error polling signals: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun sendSignal(roomId: String, fromUser: String, toUser: String, signalType: String, payloadJson: String, token: String?): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.BASE_URL}/rest/v1/room_voice_signals"
            val bodyObj = JSONObject().apply {
                put("room_id", roomId)
                put("from_user", fromUser)
                put("to_user", toUser)
                put("signal_type", signalType)
                // Payload can be raw JSON object
                val parsedPayload = try { JSONObject(payloadJson) } catch (_: Exception) { JSONObject().put("raw", payloadJson) }
                put("payload", parsedPayload)
            }

            val reqBuilder = Request.Builder()
                .url(url)
                .post(bodyObj.toString().toRequestBody(jsonMediaType))
            buildHeaders(token).forEach { (k, v) -> reqBuilder.addHeader(k, v) }

            val resp = client.newCall(reqBuilder.build()).execute()
            if (resp.isSuccessful || resp.code == 201) {
                Result.success(true)
            } else {
                Result.failure(Exception("Failed to send signal: ${resp.code}"))
            }
        } catch (e: Exception) {
            Log.e("VoiceSignaling", "Error sending signal: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun deleteSignal(signalId: String, token: String?) = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.BASE_URL}/rest/v1/room_voice_signals?id=eq.$signalId"
            val reqBuilder = Request.Builder().url(url).delete()
            buildHeaders(token).forEach { (k, v) -> reqBuilder.addHeader(k, v) }
            client.newCall(reqBuilder.build()).execute().close()
        } catch (e: Exception) {
            Log.w("VoiceSignaling", "Error deleting signal: ${e.message}")
        }
    }

    suspend fun joinStage(roomId: String, userId: String, token: String?): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.BASE_URL}/rest/v1/room_speakers"
            val body = JSONObject().apply {
                put("room_id", roomId)
                put("user_id", userId)
                put("is_muted", false)
                put("added_by", userId)
            }
            val reqBuilder = Request.Builder()
                .url(url)
                .post(body.toString().toRequestBody(jsonMediaType))
            buildHeaders(token).forEach { (k, v) -> reqBuilder.addHeader(k, v) }

            val resp = client.newCall(reqBuilder.build()).execute()
            if (resp.isSuccessful || resp.code == 201) {
                // Also remove my invite and raised hand
                deleteInvite(roomId, userId, token)
                lowerHand(roomId, userId, token)
                Result.success(true)
            } else {
                Result.failure(Exception("Failed to join stage: ${resp.code}"))
            }
        } catch (e: Exception) {
            Log.e("VoiceSignaling", "Error joining stage: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun leaveStage(roomId: String, userId: String, token: String?): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.BASE_URL}/rest/v1/room_speakers?room_id=eq.$roomId&user_id=eq.$userId"
            val reqBuilder = Request.Builder().url(url).delete()
            buildHeaders(token).forEach { (k, v) -> reqBuilder.addHeader(k, v) }

            val resp = client.newCall(reqBuilder.build()).execute()
            if (resp.isSuccessful || resp.code == 204) {
                Result.success(true)
            } else {
                Result.failure(Exception("Failed to leave stage: ${resp.code}"))
            }
        } catch (e: Exception) {
            Log.e("VoiceSignaling", "Error leaving stage: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun updateMuteState(roomId: String, userId: String, isMuted: Boolean, token: String?): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.BASE_URL}/rest/v1/room_speakers?room_id=eq.$roomId&user_id=eq.$userId"
            val body = JSONObject().apply {
                put("is_muted", isMuted)
            }
            val reqBuilder = Request.Builder()
                .url(url)
                .patch(body.toString().toRequestBody(jsonMediaType))
            buildHeaders(token).forEach { (k, v) -> reqBuilder.addHeader(k, v) }

            val resp = client.newCall(reqBuilder.build()).execute()
            if (resp.isSuccessful) {
                Result.success(true)
            } else {
                Result.failure(Exception("Failed to update mute state: ${resp.code}"))
            }
        } catch (e: Exception) {
            Log.e("VoiceSignaling", "Error updating mute state: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun raiseHand(roomId: String, userId: String, token: String?): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.BASE_URL}/rest/v1/room_raised_hands"
            val body = JSONObject().apply {
                put("room_id", roomId)
                put("user_id", userId)
            }
            val reqBuilder = Request.Builder()
                .url(url)
                .post(body.toString().toRequestBody(jsonMediaType))
            buildHeaders(token).forEach { (k, v) -> reqBuilder.addHeader(k, v) }

            val resp = client.newCall(reqBuilder.build()).execute()
            if (resp.isSuccessful || resp.code == 201) {
                Result.success(true)
            } else {
                Result.failure(Exception("Failed to raise hand: ${resp.code}"))
            }
        } catch (e: Exception) {
            Log.e("VoiceSignaling", "Error raising hand: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun lowerHand(roomId: String, userId: String, token: String?): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.BASE_URL}/rest/v1/room_raised_hands?room_id=eq.$roomId&user_id=eq.$userId"
            val reqBuilder = Request.Builder().url(url).delete()
            buildHeaders(token).forEach { (k, v) -> reqBuilder.addHeader(k, v) }

            val resp = client.newCall(reqBuilder.build()).execute()
            if (resp.isSuccessful || resp.code == 204) {
                Result.success(true)
            } else {
                Result.failure(Exception("Failed to lower hand: ${resp.code}"))
            }
        } catch (e: Exception) {
            Log.e("VoiceSignaling", "Error lowering hand: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun inviteSpeaker(roomId: String, userId: String, invitedBy: String, token: String?): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.BASE_URL}/rest/v1/room_speaker_invites"
            val body = JSONObject().apply {
                put("room_id", roomId)
                put("user_id", userId)
                put("invited_by", invitedBy)
            }
            val reqBuilder = Request.Builder()
                .url(url)
                .post(body.toString().toRequestBody(jsonMediaType))
            buildHeaders(token).forEach { (k, v) -> reqBuilder.addHeader(k, v) }

            val resp = client.newCall(reqBuilder.build()).execute()
            if (resp.isSuccessful || resp.code == 201) {
                Result.success(true)
            } else {
                Result.failure(Exception("Failed to invite speaker: ${resp.code}"))
            }
        } catch (e: Exception) {
            Log.e("VoiceSignaling", "Error inviting speaker: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun deleteInvite(roomId: String, userId: String, token: String?): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.BASE_URL}/rest/v1/room_speaker_invites?room_id=eq.$roomId&user_id=eq.$userId"
            val reqBuilder = Request.Builder().url(url).delete()
            buildHeaders(token).forEach { (k, v) -> reqBuilder.addHeader(k, v) }

            val resp = client.newCall(reqBuilder.build()).execute()
            if (resp.isSuccessful || resp.code == 204) {
                Result.success(true)
            } else {
                Result.failure(Exception("Failed to delete invite: ${resp.code}"))
            }
        } catch (e: Exception) {
            Log.e("VoiceSignaling", "Error deleting invite: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun kickSpeaker(roomId: String, userId: String, token: String?): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.BASE_URL}/rest/v1/room_speakers?room_id=eq.$roomId&user_id=eq.$userId"
            val reqBuilder = Request.Builder().url(url).delete()
            buildHeaders(token).forEach { (k, v) -> reqBuilder.addHeader(k, v) }

            val resp = client.newCall(reqBuilder.build()).execute()
            if (resp.isSuccessful || resp.code == 204) {
                Result.success(true)
            } else {
                Result.failure(Exception("Failed to kick speaker: ${resp.code}"))
            }
        } catch (e: Exception) {
            Log.e("VoiceSignaling", "Error kicking speaker: ${e.message}", e)
            Result.failure(e)
        }
    }

    private suspend fun fetchProfiles(userIds: List<String>, token: String?): Map<String, Pair<String?, String?>> = withContext(Dispatchers.IO) {
        if (userIds.isEmpty()) return@withContext emptyMap()
        try {
            val filter = userIds.joinToString(",")
            val url = "${SupabaseConfig.BASE_URL}/rest/v1/profiles?id=in.($filter)&select=id,username,avatar_url"
            val reqBuilder = Request.Builder().url(url)
            buildHeaders(token).forEach { (k, v) -> reqBuilder.addHeader(k, v) }

            val resp = client.newCall(reqBuilder.build()).execute()
            val body = resp.body?.string() ?: "[]"
            if (!resp.isSuccessful) return@withContext emptyMap()

            val array = JSONArray(body)
            val map = mutableMapOf<String, Pair<String?, String?>>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val id = obj.getString("id")
                val uname = obj.optString("username", null)
                val avatar = obj.optString("avatar_url", null)
                map[id] = Pair(uname, avatar)
            }
            map
        } catch (e: Exception) {
            Log.e("VoiceSignaling", "Error fetching profiles: ${e.message}")
            emptyMap()
        }
    }
}
