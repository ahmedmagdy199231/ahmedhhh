package app.lovable.giant.data.remote

import android.util.Log
import app.lovable.giant.data.SupabaseConfig
import app.lovable.giant.data.models.ChatMessage
import app.lovable.giant.data.models.Room
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class SupabaseRestClient {
    private val baseUrl = SupabaseConfig.URL
    private val apiKey = SupabaseConfig.ANON_KEY

    suspend fun getRooms(token: String? = null): Result<List<Room>> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseUrl/rest/v1/rooms?select=*&order=created_at.desc")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("apikey", apiKey)
                if (!token.isNullOrEmpty()) {
                    setRequestProperty("Authorization", "Bearer $token")
                }
                connectTimeout = 15000
                readTimeout = 15000
            }

            val responseCode = conn.responseCode
            if (responseCode in 200..299) {
                val responseStr = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                val jsonArray = JSONArray(responseStr)
                val roomList = mutableListOf<Room>()

                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    roomList.add(
                        Room(
                            id = obj.getString("id"),
                            name = obj.optString("name", "غرفة بدون اسم"),
                            description = obj.optString("description", null),
                            ownerId = obj.optString("owner_id", null),
                            memberCount = obj.optInt("member_count", 0),
                            isPrivate = obj.optBoolean("is_private", false),
                            category = obj.optString("category", "general")
                        )
                    )
                }
                Result.success(roomList)
            } else {
                val err = BufferedReader(InputStreamReader(conn.errorStream ?: conn.inputStream)).use { it.readText() }
                Result.failure(Exception("Failed to load rooms: $responseCode ($err)"))
            }
        } catch (e: Exception) {
            Log.e("SupabaseRestClient", "Error loading rooms", e)
            Result.failure(e)
        }
    }

    suspend fun getRoomDetails(roomId: String, token: String? = null): Result<Room> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseUrl/rest/v1/rooms?id=eq.$roomId&select=*")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("apikey", apiKey)
                if (!token.isNullOrEmpty()) {
                    setRequestProperty("Authorization", "Bearer $token")
                }
                connectTimeout = 15000
                readTimeout = 15000
            }

            if (conn.responseCode in 200..299) {
                val responseStr = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                val jsonArray = JSONArray(responseStr)
                if (jsonArray.length() > 0) {
                    val obj = jsonArray.getJSONObject(0)
                    val room = Room(
                        id = obj.getString("id"),
                        name = obj.optString("name", "غرفة بدون اسم"),
                        description = obj.optString("description", null),
                        ownerId = obj.optString("owner_id", null),
                        memberCount = obj.optInt("member_count", 0),
                        isPrivate = obj.optBoolean("is_private", false),
                        category = obj.optString("category", "general")
                    )
                    Result.success(room)
                } else {
                    Result.failure(Exception("Room not found"))
                }
            } else {
                Result.failure(Exception("Error loading room details (${conn.responseCode})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createRoom(name: String, description: String?, isPrivate: Boolean, token: String): Result<Room> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseUrl/rest/v1/rooms")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("apikey", apiKey)
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Prefer", "return=representation")
                doOutput = true
            }

            val payload = JSONObject().apply {
                put("name", name)
                put("description", description ?: "")
                put("is_private", isPrivate)
            }

            OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }

            if (conn.responseCode in 200..299) {
                val res = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                val jsonArray = JSONArray(res)
                val obj = jsonArray.getJSONObject(0)
                val room = Room(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    description = obj.optString("description", null),
                    ownerId = obj.optString("owner_id", null),
                    memberCount = 1,
                    isPrivate = obj.optBoolean("is_private", false),
                    category = obj.optString("category", "general")
                )
                Result.success(room)
            } else {
                val err = BufferedReader(InputStreamReader(conn.errorStream ?: conn.inputStream)).use { it.readText() }
                Result.failure(Exception("Create room failed: $err"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun joinRoom(roomId: String, userId: String, token: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseUrl/rest/v1/room_members")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("apikey", apiKey)
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Prefer", "resolution=merge-duplicates")
                doOutput = true
            }

            val payload = JSONObject().apply {
                put("room_id", roomId)
                put("user_id", userId)
                put("status", "active")
            }

            OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }

            if (conn.responseCode in 200..299) {
                Result.success(true)
            } else {
                Result.failure(Exception("Join room failed: ${conn.responseCode}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun leaveRoom(roomId: String, userId: String, token: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseUrl/rest/v1/room_members?room_id=eq.$roomId&user_id=eq.$userId")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "DELETE"
                setRequestProperty("apikey", apiKey)
                setRequestProperty("Authorization", "Bearer $token")
            }

            if (conn.responseCode in 200..299) {
                Result.success(true)
            } else {
                Result.failure(Exception("Leave room failed: ${conn.responseCode}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getRoomMessages(roomId: String, token: String? = null): Result<List<ChatMessage>> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseUrl/rest/v1/room_messages?room_id=eq.$roomId&select=*&order=created_at.asc&limit=50")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("apikey", apiKey)
                if (!token.isNullOrEmpty()) {
                    setRequestProperty("Authorization", "Bearer $token")
                }
            }

            if (conn.responseCode in 200..299) {
                val res = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                val jsonArray = JSONArray(res)
                val messages = mutableListOf<ChatMessage>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    messages.add(
                        ChatMessage(
                            id = obj.optString("id", i.toString()),
                            roomId = obj.optString("room_id", roomId),
                            senderId = obj.optString("sender_id", obj.optString("user_id", "")),
                            senderName = obj.optString("sender_name", null),
                            content = obj.optString("content", obj.optString("text", "")),
                            createdAt = obj.optString("created_at", ""),
                            messageType = obj.optString("message_type", "text")
                        )
                    )
                }
                Result.success(messages)
            } else {
                Result.failure(Exception("Failed to get messages: ${conn.responseCode}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendRoomMessage(roomId: String, userId: String, text: String, token: String): Result<ChatMessage> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseUrl/rest/v1/room_messages")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("apikey", apiKey)
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Prefer", "return=representation")
                doOutput = true
            }

            val payload = JSONObject().apply {
                put("room_id", roomId)
                put("sender_id", userId)
                put("content", text)
            }

            OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }

            if (conn.responseCode in 200..299) {
                val res = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                val jsonArray = JSONArray(res)
                val obj = jsonArray.getJSONObject(0)
                val msg = ChatMessage(
                    id = obj.optString("id", "msg_1"),
                    roomId = roomId,
                    senderId = userId,
                    senderName = obj.optString("sender_name", null),
                    content = text,
                    createdAt = obj.optString("created_at", "")
                )
                Result.success(msg)
            } else {
                Result.failure(Exception("Send message failed: ${conn.responseCode}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDirectMessagesList(myUserId: String, token: String): Result<List<app.lovable.giant.data.models.ConversationItem>> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseUrl/rest/v1/direct_messages?select=sender_id,receiver_id,content,created_at,read_at,message_type&or=(sender_id.eq.$myUserId,receiver_id.eq.$myUserId)&order=created_at.desc&limit=200")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("apikey", apiKey)
                setRequestProperty("Authorization", "Bearer $token")
            }

            if (conn.responseCode in 200..299) {
                val res = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                val jsonArray = JSONArray(res)
                val peerMap = mutableMapOf<String, Triple<String, String, Int>>() // otherId -> (lastMsg, createdAt, unreadCount)

                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val senderId = obj.getString("sender_id")
                    val receiverId = obj.getString("receiver_id")
                    val content = obj.optString("content", "")
                    val createdAt = obj.optString("created_at", "")
                    val readAt = obj.optString("read_at", null)
                    val otherId = if (senderId == myUserId) receiverId else senderId
                    val isUnread = receiverId == myUserId && (readAt.isNullOrEmpty() || readAt == "null")

                    if (!peerMap.containsKey(otherId)) {
                        peerMap[otherId] = Triple(content.ifEmpty { "رسالة وسائط" }, createdAt, if (isUnread) 1 else 0)
                    } else if (isUnread) {
                        val current = peerMap[otherId]!!
                        peerMap[otherId] = Triple(current.first, current.second, current.third + 1)
                    }
                }

                val resultList = mutableListOf<app.lovable.giant.data.models.ConversationItem>()
                for ((otherId, data) in peerMap) {
                    val profileRes = getUserProfile(otherId, token)
                    val username = profileRes.getOrNull()?.username ?: "مستخدم"
                    val avatarUrl = profileRes.getOrNull()?.avatarUrl
                    resultList.add(
                        app.lovable.giant.data.models.ConversationItem(
                            otherId = otherId,
                            username = username,
                            avatarUrl = avatarUrl,
                            lastMessage = data.first,
                            createdAt = data.second,
                            unreadCount = data.third
                        )
                    )
                }
                Result.success(resultList)
            } else {
                Result.failure(Exception("Failed to get direct messages list: ${conn.responseCode}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getConversationMessages(myUserId: String, otherId: String, token: String): Result<List<app.lovable.giant.data.models.DirectMessage>> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseUrl/rest/v1/direct_messages?select=id,sender_id,receiver_id,content,created_at,message_type,media_url,read_at,delivered_at,reply_to_id&or=(and(sender_id.eq.$myUserId,receiver_id.eq.$otherId),and(sender_id.eq.$otherId,receiver_id.eq.$myUserId))&order=created_at.asc&limit=150")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("apikey", apiKey)
                setRequestProperty("Authorization", "Bearer $token")
            }

            if (conn.responseCode in 200..299) {
                val res = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                val jsonArray = JSONArray(res)
                val messages = mutableListOf<app.lovable.giant.data.models.DirectMessage>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    messages.add(
                        app.lovable.giant.data.models.DirectMessage(
                            id = obj.getString("id"),
                            senderId = obj.getString("sender_id"),
                            receiverId = obj.getString("receiver_id"),
                            content = obj.optString("content", ""),
                            createdAt = obj.optString("created_at", ""),
                            messageType = obj.optString("message_type", "text"),
                            mediaUrl = obj.optString("media_url", null),
                            readAt = obj.optString("read_at", null),
                            deliveredAt = obj.optString("delivered_at", null),
                            replyToId = obj.optString("reply_to_id", null)
                        )
                    )
                }
                Result.success(messages)
            } else {
                Result.failure(Exception("Failed to get DM messages: ${conn.responseCode}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendDirectMessage(senderId: String, receiverId: String, content: String, replyToId: String?, token: String): Result<app.lovable.giant.data.models.DirectMessage> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseUrl/rest/v1/direct_messages")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("apikey", apiKey)
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Prefer", "return=representation")
                doOutput = true
            }

            val payload = JSONObject().apply {
                put("sender_id", senderId)
                put("receiver_id", receiverId)
                put("content", content)
                put("message_type", "text")
                if (!replyToId.isNullOrEmpty()) {
                    put("reply_to_id", replyToId)
                }
            }

            OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }

            if (conn.responseCode in 200..299) {
                val res = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                val jsonArray = JSONArray(res)
                val obj = jsonArray.getJSONObject(0)
                val msg = app.lovable.giant.data.models.DirectMessage(
                    id = obj.getString("id"),
                    senderId = obj.getString("sender_id"),
                    receiverId = obj.getString("receiver_id"),
                    content = obj.optString("content", content),
                    createdAt = obj.optString("created_at", ""),
                    messageType = obj.optString("message_type", "text"),
                    replyToId = obj.optString("reply_to_id", replyToId)
                )
                Result.success(msg)
            } else {
                val err = BufferedReader(InputStreamReader(conn.errorStream ?: conn.inputStream)).use { it.readText() }
                Result.failure(Exception("Send DM failed: ${conn.responseCode} - $err"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchUsers(query: String, myUserId: String, token: String): Result<List<app.lovable.giant.data.models.SearchUserItem>> = withContext(Dispatchers.IO) {
        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = URL("$baseUrl/rest/v1/profiles?username=ilike.*$encodedQuery*&id=neq.$myUserId&select=id,username,avatar_url&limit=15")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("apikey", apiKey)
                setRequestProperty("Authorization", "Bearer $token")
            }

            if (conn.responseCode in 200..299) {
                val res = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                val jsonArray = JSONArray(res)
                val users = mutableListOf<app.lovable.giant.data.models.SearchUserItem>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    users.add(
                        app.lovable.giant.data.models.SearchUserItem(
                            id = obj.getString("id"),
                            username = obj.optString("username", "مستخدم"),
                            avatarUrl = obj.optString("avatar_url", null)
                        )
                    )
                }
                Result.success(users)
            } else {
                Result.failure(Exception("Search users failed: ${conn.responseCode}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserProfile(userId: String, token: String? = null): Result<app.lovable.giant.data.models.UserProfile> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseUrl/rest/v1/profiles?id=eq.$userId&select=*")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("apikey", apiKey)
                if (!token.isNullOrEmpty()) {
                    setRequestProperty("Authorization", "Bearer $token")
                }
            }

            if (conn.responseCode in 200..299) {
                val res = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                val jsonArray = JSONArray(res)
                if (jsonArray.length() > 0) {
                    val obj = jsonArray.getJSONObject(0)
                    val prof = app.lovable.giant.data.models.UserProfile(
                        id = obj.getString("id"),
                        username = obj.optString("username", "مستخدم"),
                        bio = obj.optString("bio", null),
                        avatarUrl = obj.optString("avatar_url", null),
                        coverUrl = obj.optString("cover_url", null),
                        coverType = obj.optString("cover_type", null),
                        level = obj.optInt("level", 1),
                        points = obj.optLong("points", 0),
                        isVip = obj.optBoolean("is_vip", false),
                        gender = obj.optString("gender", null),
                        country = obj.optString("country", null),
                        hideLastSeen = obj.optBoolean("hide_last_seen", false),
                        dmLocked = obj.optBoolean("dm_locked", false),
                        profileViews = obj.optLong("profile_views", 0)
                    )
                    Result.success(prof)
                } else {
                    Result.failure(Exception("Profile not found"))
                }
            } else {
                Result.failure(Exception("Get profile failed: ${conn.responseCode}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProfile(
        userId: String,
        token: String,
        bio: String?,
        gender: String?,
        country: String?,
        hideLastSeen: Boolean,
        dmLocked: Boolean
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseUrl/rest/v1/profiles?id=eq.$userId")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "PATCH"
                setRequestProperty("apikey", apiKey)
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Prefer", "return=minimal")
                doOutput = true
            }

            val payload = JSONObject().apply {
                if (bio != null) put("bio", bio) else put("bio", JSONObject.NULL)
                if (gender != null) put("gender", gender) else put("gender", JSONObject.NULL)
                if (country != null) put("country", country) else put("country", JSONObject.NULL)
                put("hide_last_seen", hideLastSeen)
                put("dm_locked", dmLocked)
            }

            OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }

            if (conn.responseCode in 200..299) {
                Result.success(true)
            } else {
                val err = BufferedReader(InputStreamReader(conn.errorStream ?: conn.inputStream)).use { it.readText() }
                Result.failure(Exception("Update profile failed: ${conn.responseCode} - $err"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateAvatarUrl(userId: String, token: String, avatarUrl: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseUrl/rest/v1/profiles?id=eq.$userId")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "PATCH"
                setRequestProperty("apikey", apiKey)
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Prefer", "return=minimal")
                doOutput = true
            }

            val payload = JSONObject().apply {
                put("avatar_url", avatarUrl)
            }

            OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }

            if (conn.responseCode in 200..299) {
                Result.success(true)
            } else {
                Result.failure(Exception("Update avatar failed: ${conn.responseCode}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadAvatar(userId: String, token: String, bytes: ByteArray, mimeType: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val ext = if (mimeType.contains("png")) "png" else "jpg"
            val path = "$userId/avatar.$ext"
            val uploadUrl = URL("$baseUrl/storage/v1/object/avatars/$path")
            val conn = (uploadUrl.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("apikey", apiKey)
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Content-Type", mimeType)
                setRequestProperty("x-upsert", "true")
                doOutput = true
            }

            conn.outputStream.use { it.write(bytes) }

            if (conn.responseCode in 200..299) {
                val publicUrl = "$baseUrl/storage/v1/object/public/avatars/$path?v=${System.currentTimeMillis()}"
                updateAvatarUrl(userId, token, publicUrl)
                Result.success(publicUrl)
            } else {
                val err = BufferedReader(InputStreamReader(conn.errorStream ?: conn.inputStream)).use { it.readText() }
                Result.failure(Exception("Upload avatar failed: ${conn.responseCode} - $err"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateAccountEmail(token: String, newEmail: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseUrl/auth/v1/user")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "PUT"
                setRequestProperty("apikey", apiKey)
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
            }

            val payload = JSONObject().apply {
                put("email", newEmail.trim())
            }

            OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }

            if (conn.responseCode in 200..299) {
                Result.success(true)
            } else {
                val err = BufferedReader(InputStreamReader(conn.errorStream ?: conn.inputStream)).use { it.readText() }
                Result.failure(Exception("Update email failed: ${conn.responseCode} - $err"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateAccountPassword(token: String, newPassword: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseUrl/auth/v1/user")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "PUT"
                setRequestProperty("apikey", apiKey)
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
            }

            val payload = JSONObject().apply {
                put("password", newPassword)
            }

            OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }

            if (conn.responseCode in 200..299) {
                Result.success(true)
            } else {
                val err = BufferedReader(InputStreamReader(conn.errorStream ?: conn.inputStream)).use { it.readText() }
                Result.failure(Exception("Update password failed: ${conn.responseCode} - $err"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAuthUser(token: String): Result<JSONObject> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseUrl/auth/v1/user")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("apikey", apiKey)
                setRequestProperty("Authorization", "Bearer $token")
            }

            if (conn.responseCode in 200..299) {
                val res = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                Result.success(JSONObject(res))
            } else {
                Result.failure(Exception("Get auth user failed: ${conn.responseCode}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
