package app.lovable.giant.data.remote

import android.util.Log
import app.lovable.giant.data.SupabaseConfig
import app.lovable.giant.data.models.ChatMessage
import app.lovable.giant.data.models.CommunityCommentModel
import app.lovable.giant.data.models.CommunityPostModel
import app.lovable.giant.data.models.CommunityReactionModel
import app.lovable.giant.data.models.ConversationItem
import app.lovable.giant.data.models.DailyTaskModel
import app.lovable.giant.data.models.DirectMessage
import app.lovable.giant.data.models.GiftCatalogModel
import app.lovable.giant.data.models.LevelThresholdModel
import app.lovable.giant.data.models.Room
import app.lovable.giant.data.models.RoomMemberItem
import app.lovable.giant.data.models.SearchUserItem
import app.lovable.giant.data.models.ShopItemModel
import app.lovable.giant.data.models.StoryItemModel
import app.lovable.giant.data.models.StoryReactionItem
import app.lovable.giant.data.models.StoryUserModel
import app.lovable.giant.data.models.StoryViewItem
import app.lovable.giant.data.models.UserBadge
import app.lovable.giant.data.models.UserProfile
import app.lovable.giant.data.models.UserSession
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

    // ==========================================
    // STORE, GIFTS & DAILY TASKS METHODS
    // ==========================================

    suspend fun getShopItems(token: String): Result<List<ShopItemModel>> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseUrl/rest/v1/shop_items?select=*&order=sort_order.asc")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("apikey", apiKey)
                setRequestProperty("Authorization", "Bearer $token")
            }

            if (conn.responseCode in 200..299) {
                val res = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                val array = JSONArray(res)
                val items = mutableListOf<ShopItemModel>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val payloadObj = obj.optJSONObject("payload")
                    val colorHex = payloadObj?.optString("color", null)
                    val emoji = payloadObj?.optString("emoji", null)
                    items.add(
                        ShopItemModel(
                            id = obj.getString("id"),
                            kind = obj.getString("kind"),
                            code = obj.getString("code"),
                            nameAr = obj.getString("name_ar"),
                            price = obj.optLong("price", 0L),
                            payload = payloadObj?.toString() ?: "{}",
                            sortOrder = obj.optInt("sort_order", 0),
                            genderTarget = if (obj.isNull("gender_target")) null else obj.optString("gender_target"),
                            colorHex = colorHex,
                            previewEmoji = emoji
                        )
                    )
                }
                Result.success(items)
            } else {
                Result.failure(Exception("Failed to load shop items: ${conn.responseCode}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserInventory(userId: String, token: String): Result<Set<String>> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseUrl/rest/v1/user_inventory?user_id=eq.$userId&select=item_id")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("apikey", apiKey)
                setRequestProperty("Authorization", "Bearer $token")
            }

            if (conn.responseCode in 200..299) {
                val res = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                val array = JSONArray(res)
                val set = mutableSetOf<String>()
                for (i in 0 until array.length()) {
                    set.add(array.getJSONObject(i).getString("item_id"))
                }
                Result.success(set)
            } else {
                Result.failure(Exception("Failed to load inventory: ${conn.responseCode}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getEquippedItems(userId: String, token: String): Result<Map<String, String?>> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseUrl/rest/v1/profiles?id=eq.$userId&select=points,gender,equipped_badge,equipped_name_color,equipped_chat_color,equipped_effect,equipped_frame")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("apikey", apiKey)
                setRequestProperty("Authorization", "Bearer $token")
            }

            if (conn.responseCode in 200..299) {
                val res = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                val array = JSONArray(res)
                val map = mutableMapOf<String, String?>()
                if (array.length() > 0) {
                    val obj = array.getJSONObject(0)
                    map["badge"] = if (obj.isNull("equipped_badge")) null else obj.optString("equipped_badge")
                    map["name_color"] = if (obj.isNull("equipped_name_color")) null else obj.optString("equipped_name_color")
                    map["chat_color"] = if (obj.isNull("equipped_chat_color")) null else obj.optString("equipped_chat_color")
                    map["effect"] = if (obj.isNull("equipped_effect")) null else obj.optString("equipped_effect")
                    map["avatar_frame"] = if (obj.isNull("equipped_frame")) null else obj.optString("equipped_frame")
                    map["points"] = obj.optLong("points", 0L).toString()
                    map["gender"] = if (obj.isNull("gender")) null else obj.optString("gender")
                }
                Result.success(map)
            } else {
                Result.failure(Exception("Failed to load equipped items: ${conn.responseCode}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getGiftsCatalog(token: String): Result<List<GiftCatalogModel>> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseUrl/rest/v1/gifts_catalog?is_active=eq.true&select=id,name,emoji,cost_points,scope,category&order=sort_order.asc")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("apikey", apiKey)
                setRequestProperty("Authorization", "Bearer $token")
            }

            if (conn.responseCode in 200..299) {
                val res = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                val array = JSONArray(res)
                val gifts = mutableListOf<GiftCatalogModel>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    gifts.add(
                        GiftCatalogModel(
                            id = obj.getString("id"),
                            name = obj.getString("name"),
                            emoji = if (obj.isNull("emoji")) "🎁" else obj.optString("emoji", "🎁"),
                            costPoints = obj.optLong("cost_points", 0L),
                            scope = obj.optString("scope", "room"),
                            category = if (obj.isNull("category")) null else obj.optString("category")
                        )
                    )
                }
                Result.success(gifts)
            } else {
                Result.failure(Exception("Failed to load gifts catalog: ${conn.responseCode}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun purchaseShopItem(itemId: String, token: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseUrl/rest/v1/rpc/shop_purchase")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("apikey", apiKey)
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
            }

            val payload = JSONObject().apply {
                put("_item", itemId)
            }

            OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }

            if (conn.responseCode in 200..299) {
                Result.success(true)
            } else {
                val err = BufferedReader(InputStreamReader(conn.errorStream ?: conn.inputStream)).use { it.readText() }
                val message = if (err.contains("insufficient")) "نقاطك غير كافية"
                else if (err.contains("already_owned")) "تمتلك هذا العنصر بالفعل"
                else "تعذر الشراء: ${conn.responseCode}"
                Result.failure(Exception(message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun equipShopItem(itemId: String, token: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseUrl/rest/v1/rpc/shop_equip")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("apikey", apiKey)
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
            }

            val payload = JSONObject().apply {
                put("_item", itemId)
            }

            OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }

            if (conn.responseCode in 200..299) {
                Result.success(true)
            } else {
                val err = BufferedReader(InputStreamReader(conn.errorStream ?: conn.inputStream)).use { it.readText() }
                val message = if (err.contains("gender_restricted")) "هذا المؤثر مخصص لجنس آخر" else "تعذر تطبيق العنصر"
                Result.failure(Exception(message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun unequipShopKind(kind: String, token: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseUrl/rest/v1/rpc/shop_unequip")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("apikey", apiKey)
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
            }

            val payload = JSONObject().apply {
                put("_kind", kind)
            }

            OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }

            if (conn.responseCode in 200..299) {
                Result.success(true)
            } else {
                Result.failure(Exception("تعذر إزالة التطبيق"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendGift(receiverId: String, giftId: String, roomId: String, token: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseUrl/rest/v1/rpc/send_gift")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("apikey", apiKey)
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
            }

            val payload = JSONObject().apply {
                put("_receiver", receiverId)
                put("_gift", giftId)
                put("_room", roomId)
            }

            OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }

            if (conn.responseCode in 200..299) {
                Result.success(true)
            } else {
                val err = BufferedReader(InputStreamReader(conn.errorStream ?: conn.inputStream)).use { it.readText() }
                Result.failure(Exception(if (err.contains("insufficient")) "نقاطك لا تكفي" else "تعذر إرسال الهدية"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getRoomMembersWithProfiles(roomId: String, currentUserId: String, token: String): Result<List<RoomMemberItem>> = withContext(Dispatchers.IO) {
        try {
            // First get members
            val urlMembers = URL("$baseUrl/rest/v1/room_members?room_id=eq.$roomId&select=user_id")
            val connMembers = (urlMembers.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("apikey", apiKey)
                setRequestProperty("Authorization", "Bearer $token")
            }

            if (connMembers.responseCode in 200..299) {
                val res = BufferedReader(InputStreamReader(connMembers.inputStream)).use { it.readText() }
                val arr = JSONArray(res)
                val userIds = mutableListOf<String>()
                for (i in 0 until arr.length()) {
                    val uid = arr.getJSONObject(i).getString("user_id")
                    if (uid != currentUserId) {
                        userIds.add(uid)
                    }
                }

                if (userIds.isEmpty()) {
                    return@withContext Result.success(emptyList())
                }

                val inQuery = userIds.joinToString(",")
                val urlProfiles = URL("$baseUrl/rest/v1/profiles?id=in.($inQuery)&select=id,username,avatar_url")
                val connProfiles = (urlProfiles.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    setRequestProperty("apikey", apiKey)
                    setRequestProperty("Authorization", "Bearer $token")
                }

                if (connProfiles.responseCode in 200..299) {
                    val pRes = BufferedReader(InputStreamReader(connProfiles.inputStream)).use { it.readText() }
                    val pArr = JSONArray(pRes)
                    val members = mutableListOf<RoomMemberItem>()
                    for (i in 0 until pArr.length()) {
                        val pObj = pArr.getJSONObject(i)
                        members.add(
                            RoomMemberItem(
                                userId = pObj.getString("id"),
                                username = pObj.optString("username", "مستخدم"),
                                avatarUrl = if (pObj.isNull("avatar_url")) null else pObj.optString("avatar_url")
                            )
                        )
                    }
                    Result.success(members)
                } else {
                    Result.success(emptyList())
                }
            } else {
                Result.failure(Exception("Failed to get room members"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDailyTasks(token: String): Result<List<DailyTaskModel>> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseUrl/rest/v1/rpc/get_my_daily_tasks")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("apikey", apiKey)
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
            }
            OutputStreamWriter(conn.outputStream).use { it.write("{}") }

            if (conn.responseCode in 200..299) {
                val res = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                val array = JSONArray(res)
                val tasks = mutableListOf<DailyTaskModel>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    tasks.add(
                        DailyTaskModel(
                            kind = obj.getString("kind"),
                            label = obj.getString("label"),
                            target = obj.getInt("target"),
                            reward = obj.getLong("reward"),
                            progress = obj.getInt("progress"),
                            claimed = obj.getBoolean("claimed")
                        )
                    )
                }
                Result.success(tasks)
            } else {
                Result.failure(Exception("Failed to load daily tasks: ${conn.responseCode}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getLevelThresholds(token: String): Result<List<LevelThresholdModel>> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseUrl/rest/v1/level_thresholds?select=*&order=level.asc")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("apikey", apiKey)
                setRequestProperty("Authorization", "Bearer $token")
            }

            if (conn.responseCode in 200..299) {
                val res = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                val array = JSONArray(res)
                val levels = mutableListOf<LevelThresholdModel>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    levels.add(
                        LevelThresholdModel(
                            level = obj.getInt("level"),
                            minPoints = obj.getLong("min_points"),
                            name = obj.getString("name")
                        )
                    )
                }
                Result.success(levels)
            } else {
                Result.failure(Exception("Failed to load levels: ${conn.responseCode}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun claimDailyReward(kind: String, token: String): Result<JSONObject> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseUrl/rest/v1/rpc/claim_daily_reward")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("apikey", apiKey)
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
            }

            val payload = JSONObject().apply {
                put("_kind", kind)
            }

            OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }

            if (conn.responseCode in 200..299) {
                val res = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                Result.success(JSONObject(res))
            } else {
                val err = BufferedReader(InputStreamReader(conn.errorStream ?: conn.inputStream)).use { it.readText() }
                val message = if (err.contains("already_claimed")) "تم استلام هذه المكافأة سابقًا"
                else if (err.contains("task_not_completed")) "المهمة لم تكتمل بعد"
                else "تعذّر استلام المكافأة"
                Result.failure(Exception(message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPointsSellerUsername(token: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseUrl/rest/v1/app_config?key=eq.points_seller_username&select=value")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("apikey", apiKey)
                setRequestProperty("Authorization", "Bearer $token")
            }

            if (conn.responseCode in 200..299) {
                val res = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                val array = JSONArray(res)
                if (array.length() > 0) {
                    Result.success(array.getJSONObject(0).optString("value", "admin"))
                } else {
                    Result.success("admin")
                }
            } else {
                Result.success("admin")
            }
        } catch (e: Exception) {
            Result.success("admin")
        }
    }

    // ==========================================
    // COMMUNITY POSTS, REACTIONS & COMMENTS
    // ==========================================

    suspend fun getCommunityPosts(token: String): Result<List<CommunityPostModel>> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseUrl/rest/v1/community_posts?select=*&order=created_at.desc&limit=100")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("apikey", apiKey)
                setRequestProperty("Authorization", "Bearer $token")
            }

            if (conn.responseCode in 200..299) {
                val res = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                val array = JSONArray(res)
                val posts = mutableListOf<CommunityPostModel>()
                val authorIds = mutableSetOf<String>()
                val postIds = mutableListOf<String>()

                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val authorId = obj.getString("author_id")
                    authorIds.add(authorId)
                    postIds.add(obj.getString("id"))
                }

                // Batch fetch profiles
                val profilesMap = mutableMapOf<String, Pair<String?, String?>>()
                if (authorIds.isNotEmpty()) {
                    val filterIds = authorIds.joinToString(",") { "\"$it\"" }
                    val profUrl = URL("$baseUrl/rest/v1/profiles?id=in.($filterIds)&select=id,username,avatar_url")
                    val profConn = (profUrl.openConnection() as HttpURLConnection).apply {
                        requestMethod = "GET"
                        setRequestProperty("apikey", apiKey)
                        setRequestProperty("Authorization", "Bearer $token")
                    }
                    if (profConn.responseCode in 200..299) {
                        val pRes = BufferedReader(InputStreamReader(profConn.inputStream)).use { it.readText() }
                        val pArray = JSONArray(pRes)
                        for (j in 0 until pArray.length()) {
                            val pObj = pArray.getJSONObject(j)
                            profilesMap[pObj.getString("id")] = Pair(
                                pObj.optString("username", null),
                                pObj.optString("avatar_url", null)
                            )
                        }
                    }
                }

                // Batch fetch reactions
                val reactionsCountMap = mutableMapOf<String, Int>()
                val myReactionMap = mutableMapOf<String, String>()
                if (postIds.isNotEmpty()) {
                    val pFilter = postIds.joinToString(",") { "\"$it\"" }
                    val rxUrl = URL("$baseUrl/rest/v1/community_reactions?post_id=in.($pFilter)&select=post_id,user_id,reaction")
                    val rxConn = (rxUrl.openConnection() as HttpURLConnection).apply {
                        requestMethod = "GET"
                        setRequestProperty("apikey", apiKey)
                        setRequestProperty("Authorization", "Bearer $token")
                    }
                    if (rxConn.responseCode in 200..299) {
                        val rxRes = BufferedReader(InputStreamReader(rxConn.inputStream)).use { it.readText() }
                        val rxArray = JSONArray(rxRes)
                        for (k in 0 until rxArray.length()) {
                            val rObj = rxArray.getJSONObject(k)
                            val pId = rObj.getString("post_id")
                            reactionsCountMap[pId] = (reactionsCountMap[pId] ?: 0) + 1
                        }
                    }
                }

                // Batch fetch comments counts
                val commentsCountMap = mutableMapOf<String, Int>()
                if (postIds.isNotEmpty()) {
                    val pFilter = postIds.joinToString(",") { "\"$it\"" }
                    val cUrl = URL("$baseUrl/rest/v1/community_comments?post_id=in.($pFilter)&select=post_id")
                    val cConn = (cUrl.openConnection() as HttpURLConnection).apply {
                        requestMethod = "GET"
                        setRequestProperty("apikey", apiKey)
                        setRequestProperty("Authorization", "Bearer $token")
                    }
                    if (cConn.responseCode in 200..299) {
                        val cRes = BufferedReader(InputStreamReader(cConn.inputStream)).use { it.readText() }
                        val cArray = JSONArray(cRes)
                        for (k in 0 until cArray.length()) {
                            val cObj = cArray.getJSONObject(k)
                            val pId = cObj.getString("post_id")
                            commentsCountMap[pId] = (commentsCountMap[pId] ?: 0) + 1
                        }
                    }
                }

                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val id = obj.getString("id")
                    val authorId = obj.getString("author_id")
                    val profile = profilesMap[authorId]

                    posts.add(
                        CommunityPostModel(
                            id = id,
                            authorId = authorId,
                            content = obj.optString("content", null),
                            mediaUrl = obj.optString("media_url", null),
                            mediaType = obj.optString("media_type", null),
                            kind = obj.optString("kind", "text"),
                            createdAt = obj.getString("created_at"),
                            edited = obj.optBoolean("edited", false),
                            authorUsername = profile?.first,
                            authorAvatarUrl = profile?.second,
                            reactionsCount = reactionsCountMap[id] ?: 0,
                            commentsCount = commentsCountMap[id] ?: 0,
                            myReaction = myReactionMap[id]
                        )
                    )
                }

                Result.success(posts)
            } else {
                Result.failure(Exception("Failed to get community posts: ${conn.responseCode}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createCommunityPost(
        userId: String,
        content: String?,
        mediaUrl: String?,
        mediaType: String?,
        kind: String,
        token: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseUrl/rest/v1/community_posts")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("apikey", apiKey)
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Prefer", "return=minimal")
                doOutput = true
            }

            val payload = JSONObject().apply {
                put("author_id", userId)
                put("content", if (content.isNullOrBlank()) JSONObject.NULL else content.trim())
                put("media_url", if (mediaUrl.isNullOrBlank()) JSONObject.NULL else mediaUrl)
                put("media_type", if (mediaType.isNullOrBlank()) JSONObject.NULL else mediaType)
                put("kind", kind)
            }

            OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }

            if (conn.responseCode in 200..299) {
                // Record daily action for publishing post
                try {
                    recordDailyAction("publish_post", 1, token)
                } catch (_: Exception) {}
                Result.success(true)
            } else {
                Result.failure(Exception("Failed to create post: ${conn.responseCode}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateCommunityPost(
        postId: String,
        content: String,
        token: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseUrl/rest/v1/community_posts?id=eq.$postId")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "PATCH"
                setRequestProperty("apikey", apiKey)
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
            }

            val payload = JSONObject().apply {
                put("content", content.trim())
                put("edited", true)
            }

            OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }

            if (conn.responseCode in 200..299) {
                Result.success(true)
            } else {
                Result.failure(Exception("Failed to edit post: ${conn.responseCode}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteCommunityPost(
        postId: String,
        isAdmin: Boolean,
        token: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            if (isAdmin) {
                val url = URL("$baseUrl/rest/v1/rpc/admin_delete_post")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("apikey", apiKey)
                    setRequestProperty("Authorization", "Bearer $token")
                    setRequestProperty("Content-Type", "application/json")
                    doOutput = true
                }
                val payload = JSONObject().apply { put("_post", postId) }
                OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }
                if (conn.responseCode in 200..299) return@withContext Result.success(true)
            }

            val url = URL("$baseUrl/rest/v1/community_posts?id=eq.$postId")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "DELETE"
                setRequestProperty("apikey", apiKey)
                setRequestProperty("Authorization", "Bearer $token")
            }

            if (conn.responseCode in 200..299) {
                Result.success(true)
            } else {
                Result.failure(Exception("Failed to delete post: ${conn.responseCode}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun reportCommunityPost(
        postId: String,
        reporterId: String,
        reason: String,
        token: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseUrl/rest/v1/community_reports")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("apikey", apiKey)
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
            }

            val payload = JSONObject().apply {
                put("post_id", postId)
                put("reporter_id", reporterId)
                put("reason", reason)
            }

            OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }

            if (conn.responseCode in 200..299) {
                Result.success(true)
            } else {
                Result.failure(Exception("Failed to report post: ${conn.responseCode}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun reactToCommunityPost(
        postId: String,
        userId: String,
        reaction: String,
        token: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseUrl/rest/v1/community_reactions")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("apikey", apiKey)
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Prefer", "resolution=merge-duplicates")
                doOutput = true
            }

            val payload = JSONObject().apply {
                put("post_id", postId)
                put("user_id", userId)
                put("reaction", reaction)
            }

            OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }

            if (conn.responseCode in 200..299) {
                try {
                    recordDailyAction("react_messages", 1, token)
                } catch (_: Exception) {}
                Result.success(true)
            } else {
                Result.failure(Exception("Failed to react: ${conn.responseCode}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeReactionFromPost(
        postId: String,
        userId: String,
        token: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseUrl/rest/v1/community_reactions?post_id=eq.$postId&user_id=eq.$userId")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "DELETE"
                setRequestProperty("apikey", apiKey)
                setRequestProperty("Authorization", "Bearer $token")
            }

            if (conn.responseCode in 200..299) {
                Result.success(true)
            } else {
                Result.failure(Exception("Failed to remove reaction: ${conn.responseCode}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCommunityComments(
        postId: String,
        token: String
    ): Result<List<CommunityCommentModel>> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseUrl/rest/v1/community_comments?post_id=eq.$postId&select=*&order=created_at.asc")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("apikey", apiKey)
                setRequestProperty("Authorization", "Bearer $token")
            }

            if (conn.responseCode in 200..299) {
                val res = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                val array = JSONArray(res)
                val comments = mutableListOf<CommunityCommentModel>()
                val authorIds = mutableSetOf<String>()

                for (i in 0 until array.length()) {
                    authorIds.add(array.getJSONObject(i).getString("author_id"))
                }

                val profilesMap = mutableMapOf<String, Pair<String?, String?>>()
                if (authorIds.isNotEmpty()) {
                    val filterIds = authorIds.joinToString(",") { "\"$it\"" }
                    val profUrl = URL("$baseUrl/rest/v1/profiles?id=in.($filterIds)&select=id,username,avatar_url")
                    val profConn = (profUrl.openConnection() as HttpURLConnection).apply {
                        requestMethod = "GET"
                        setRequestProperty("apikey", apiKey)
                        setRequestProperty("Authorization", "Bearer $token")
                    }
                    if (profConn.responseCode in 200..299) {
                        val pRes = BufferedReader(InputStreamReader(profConn.inputStream)).use { it.readText() }
                        val pArray = JSONArray(pRes)
                        for (j in 0 until pArray.length()) {
                            val pObj = pArray.getJSONObject(j)
                            profilesMap[pObj.getString("id")] = Pair(
                                pObj.optString("username", null),
                                pObj.optString("avatar_url", null)
                            )
                        }
                    }
                }

                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val authorId = obj.getString("author_id")
                    val prof = profilesMap[authorId]
                    comments.add(
                        CommunityCommentModel(
                            id = obj.getString("id"),
                            postId = obj.getString("post_id"),
                            authorId = authorId,
                            content = obj.getString("content"),
                            createdAt = obj.getString("created_at"),
                            authorUsername = prof?.first,
                            authorAvatarUrl = prof?.second
                        )
                    )
                }

                Result.success(comments)
            } else {
                Result.failure(Exception("Failed to get comments: ${conn.responseCode}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addCommunityComment(
        postId: String,
        authorId: String,
        content: String,
        token: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseUrl/rest/v1/community_comments")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("apikey", apiKey)
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
            }

            val payload = JSONObject().apply {
                put("post_id", postId)
                put("author_id", authorId)
                put("content", content.trim())
            }

            OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }

            if (conn.responseCode in 200..299) {
                Result.success(true)
            } else {
                Result.failure(Exception("Failed to add comment: ${conn.responseCode}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteCommunityComment(
        commentId: String,
        token: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseUrl/rest/v1/community_comments?id=eq.$commentId")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "DELETE"
                setRequestProperty("apikey", apiKey)
                setRequestProperty("Authorization", "Bearer $token")
            }

            if (conn.responseCode in 200..299) {
                Result.success(true)
            } else {
                Result.failure(Exception("Failed to delete comment: ${conn.responseCode}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==========================================
    // STORIES REST & RPCs
    // ==========================================

    suspend fun getActiveStories(token: String): Result<List<StoryUserModel>> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseUrl/rest/v1/rpc/get_active_stories")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("apikey", apiKey)
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
            }

            OutputStreamWriter(conn.outputStream).use { it.write("{}") }

            if (conn.responseCode in 200..299) {
                val res = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                val array = JSONArray(res)
                val list = mutableListOf<StoryUserModel>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        StoryUserModel(
                            userId = obj.getString("user_id"),
                            username = obj.optString("username", null),
                            avatarUrl = obj.optString("avatar_url", null),
                            equippedFrame = obj.optString("equipped_frame", null),
                            storyCount = obj.optInt("story_count", 0),
                            latestAt = obj.optString("latest_at", ""),
                            hasUnseen = obj.optBoolean("has_unseen", false)
                        )
                    )
                }
                Result.success(list)
            } else {
                Result.failure(Exception("Failed to load active stories: ${conn.responseCode}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserStories(userId: String, token: String): Result<List<StoryItemModel>> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseUrl/rest/v1/rpc/get_user_stories")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("apikey", apiKey)
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
            }

            val payload = JSONObject().apply {
                put("_user", userId)
            }

            OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }

            if (conn.responseCode in 200..299) {
                val res = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                val array = JSONArray(res)
                val list = mutableListOf<StoryItemModel>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        StoryItemModel(
                            id = obj.getString("id"),
                            userId = obj.getString("user_id"),
                            content = obj.optString("content", null),
                            mediaUrl = obj.optString("media_url", null),
                            mediaType = obj.optString("media_type", null),
                            background = obj.optString("background", null),
                            createdAt = obj.optString("created_at", ""),
                            expiresAt = obj.optString("expires_at", ""),
                            isHidden = obj.optBoolean("is_hidden", false)
                        )
                    )
                }
                Result.success(list)
            } else {
                Result.failure(Exception("Failed to get user stories: ${conn.responseCode}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun viewStory(storyId: String, token: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseUrl/rest/v1/rpc/view_story")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("apikey", apiKey)
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
            }

            val payload = JSONObject().apply {
                put("_story", storyId)
            }

            OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }

            if (conn.responseCode in 200..299) {
                Result.success(true)
            } else {
                Result.failure(Exception("Failed to record story view: ${conn.responseCode}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun publishStory(
        content: String?,
        mediaUrl: String?,
        mediaType: String?,
        background: String?,
        token: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseUrl/rest/v1/rpc/publish_story")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("apikey", apiKey)
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
            }

            val payload = JSONObject().apply {
                put("_content", if (content.isNullOrBlank()) JSONObject.NULL else content.trim())
                put("_media_url", if (mediaUrl.isNullOrBlank()) JSONObject.NULL else mediaUrl)
                put("_media_type", if (mediaType.isNullOrBlank()) JSONObject.NULL else mediaType)
                put("_background", if (background.isNullOrBlank()) JSONObject.NULL else background)
            }

            OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }

            if (conn.responseCode in 200..299) {
                val res = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                Result.success(res.trim().replace("\"", ""))
            } else {
                Result.failure(Exception("Failed to publish story: ${conn.responseCode}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun editStory(
        storyId: String,
        content: String?,
        mediaUrl: String?,
        mediaType: String?,
        background: String?,
        token: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseUrl/rest/v1/rpc/edit_story")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("apikey", apiKey)
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
            }

            val payload = JSONObject().apply {
                put("_story", storyId)
                put("_content", if (content.isNullOrBlank()) JSONObject.NULL else content.trim())
                put("_media_url", if (mediaUrl.isNullOrBlank()) JSONObject.NULL else mediaUrl)
                put("_media_type", if (mediaType.isNullOrBlank()) JSONObject.NULL else mediaType)
                put("_background", if (background.isNullOrBlank()) JSONObject.NULL else background)
            }

            OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }

            if (conn.responseCode in 200..299) {
                Result.success(true)
            } else {
                Result.failure(Exception("Failed to edit story: ${conn.responseCode}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteStory(storyId: String, token: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseUrl/rest/v1/stories?id=eq.$storyId")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "DELETE"
                setRequestProperty("apikey", apiKey)
                setRequestProperty("Authorization", "Bearer $token")
            }

            if (conn.responseCode in 200..299) {
                Result.success(true)
            } else {
                Result.failure(Exception("Failed to delete story: ${conn.responseCode}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getStoryViews(storyId: String, token: String): Result<List<StoryViewItem>> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseUrl/rest/v1/story_views?story_id=eq.$storyId&select=viewer_id,viewed_at&order=viewed_at.desc")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("apikey", apiKey)
                setRequestProperty("Authorization", "Bearer $token")
            }

            if (conn.responseCode in 200..299) {
                val res = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                val array = JSONArray(res)
                val list = mutableListOf<StoryViewItem>()
                val viewerIds = mutableSetOf<String>()
                for (i in 0 until array.length()) {
                    viewerIds.add(array.getJSONObject(i).getString("viewer_id"))
                }

                val profilesMap = mutableMapOf<String, Pair<String?, String?>>()
                if (viewerIds.isNotEmpty()) {
                    val filterIds = viewerIds.joinToString(",") { "\"$it\"" }
                    val profUrl = URL("$baseUrl/rest/v1/profiles?id=in.($filterIds)&select=id,username,avatar_url")
                    val profConn = (profUrl.openConnection() as HttpURLConnection).apply {
                        requestMethod = "GET"
                        setRequestProperty("apikey", apiKey)
                        setRequestProperty("Authorization", "Bearer $token")
                    }
                    if (profConn.responseCode in 200..299) {
                        val pRes = BufferedReader(InputStreamReader(profConn.inputStream)).use { it.readText() }
                        val pArray = JSONArray(pRes)
                        for (j in 0 until pArray.length()) {
                            val pObj = pArray.getJSONObject(j)
                            profilesMap[pObj.getString("id")] = Pair(
                                pObj.optString("username", null),
                                pObj.optString("avatar_url", null)
                            )
                        }
                    }
                }

                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val vId = obj.getString("viewer_id")
                    val prof = profilesMap[vId]
                    list.add(
                        StoryViewItem(
                            viewerId = vId,
                            viewedAt = obj.getString("viewed_at"),
                            username = prof?.first,
                            avatarUrl = prof?.second
                        )
                    )
                }

                Result.success(list)
            } else {
                Result.failure(Exception("Failed to get story views: ${conn.responseCode}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getStoryReactions(storyId: String, token: String): Result<List<StoryReactionItem>> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseUrl/rest/v1/rpc/get_story_reactions")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("apikey", apiKey)
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
            }

            val payload = JSONObject().apply {
                put("_story", storyId)
            }

            OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }

            if (conn.responseCode in 200..299) {
                val res = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                val array = JSONArray(res)
                val list = mutableListOf<StoryReactionItem>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        StoryReactionItem(
                            emoji = obj.getString("emoji"),
                            count = obj.optInt("count", 1),
                            mine = obj.optBoolean("mine", false)
                        )
                    )
                }
                Result.success(list)
            } else {
                Result.failure(Exception("Failed to load story reactions: ${conn.responseCode}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun reactToStory(storyId: String, emoji: String, token: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseUrl/rest/v1/rpc/react_to_story")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("apikey", apiKey)
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
            }

            val payload = JSONObject().apply {
                put("_story", storyId)
                put("_emoji", emoji)
            }

            OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }

            if (conn.responseCode in 200..299) {
                Result.success(true)
            } else {
                Result.failure(Exception("Failed to react to story: ${conn.responseCode}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun unreactToStory(storyId: String, token: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseUrl/rest/v1/rpc/unreact_to_story")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("apikey", apiKey)
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
            }

            val payload = JSONObject().apply {
                put("_story", storyId)
            }

            OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }

            if (conn.responseCode in 200..299) {
                Result.success(true)
            } else {
                Result.failure(Exception("Failed to unreact to story: ${conn.responseCode}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun commentOnStory(storyId: String, message: String, token: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseUrl/rest/v1/rpc/comment_on_story")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("apikey", apiKey)
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
            }

            val payload = JSONObject().apply {
                put("_story", storyId)
                put("_message", message.trim())
            }

            OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }

            if (conn.responseCode in 200..299) {
                Result.success(true)
            } else {
                Result.failure(Exception("Failed to comment on story: ${conn.responseCode}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
