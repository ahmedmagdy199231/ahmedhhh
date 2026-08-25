package app.lovable.giant.data.remote

import android.util.Log
import app.lovable.giant.data.SupabaseConfig
import app.lovable.giant.data.models.UserSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class SupabaseAuthClient {
    private val baseUrl = SupabaseConfig.URL
    private val apiKey = SupabaseConfig.ANON_KEY

    suspend fun signIn(email: String, password: String):Result<UserSession> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseUrl/auth/v1/token?grant_type=password")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("apikey", apiKey)
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
                connectTimeout = 15000
                readTimeout = 15000
            }

            val body = JSONObject().apply {
                put("email", email.trim())
                put("password", password)
            }

            OutputStreamWriter(conn.outputStream).use { it.write(body.toString()) }

            val responseCode = conn.responseCode
            if (responseCode in 200..299) {
                val responseStr = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                val json = JSONObject(responseStr)
                val userObj = json.getJSONObject("user")
                val accessToken = json.getString("access_token")
                val userId = userObj.getString("id")
                val userEmail = userObj.optString("email", email)
                val userMetadata = userObj.optJSONObject("user_metadata")
                val username = userMetadata?.optString("username", userEmail.substringBefore("@")) ?: userEmail.substringBefore("@")
                val avatarUrl = userMetadata?.optString("avatar_url", null)

                val session = UserSession(
                    userId = userId,
                    email = userEmail,
                    username = username,
                    avatarUrl = avatarUrl,
                    accessToken = accessToken
                )
                Result.success(session)
            } else {
                val errStr = BufferedReader(InputStreamReader(conn.errorStream ?: conn.inputStream)).use { it.readText() }
                val errJson = try { JSONObject(errStr) } catch (_: Exception) { null }
                val msg = errJson?.optString("error_description") ?: errJson?.optString("msg") ?: "Sign in failed ($responseCode)"
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            Log.e("SupabaseAuthClient", "Sign in error", e)
            Result.failure(e)
        }
    }

    suspend fun signUp(email: String, password: String, username: String): Result<UserSession> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseUrl/auth/v1/signup")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("apikey", apiKey)
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
                connectTimeout = 15000
                readTimeout = 15000
            }

            val body = JSONObject().apply {
                put("email", email.trim())
                put("password", password)
                put("data", JSONObject().apply {
                    put("username", username.trim())
                })
            }

            OutputStreamWriter(conn.outputStream).use { it.write(body.toString()) }

            val responseCode = conn.responseCode
            if (responseCode in 200..299) {
                val responseStr = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                val json = JSONObject(responseStr)
                val userObj = json.optJSONObject("user") ?: json
                val accessToken = json.optString("access_token", "")
                val userId = userObj.getString("id")
                val userEmail = userObj.optString("email", email)

                val session = UserSession(
                    userId = userId,
                    email = userEmail,
                    username = username,
                    avatarUrl = null,
                    accessToken = if (accessToken.isNotEmpty()) accessToken else null
                )
                Result.success(session)
            } else {
                val errStr = BufferedReader(InputStreamReader(conn.errorStream ?: conn.inputStream)).use { it.readText() }
                val errJson = try { JSONObject(errStr) } catch (_: Exception) { null }
                val msg = errJson?.optString("error_description") ?: errJson?.optString("msg") ?: "Sign up failed ($responseCode)"
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            Log.e("SupabaseAuthClient", "Sign up error", e)
            Result.failure(e)
        }
    }

    suspend fun verifyUser(accessToken: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseUrl/auth/v1/user")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("apikey", apiKey)
                setRequestProperty("Authorization", "Bearer $accessToken")
                connectTimeout = 10000
                readTimeout = 10000
            }
            conn.responseCode == 200
        } catch (e: Exception) {
            Log.w("SupabaseAuthClient", "Verify user failed", e)
            false
        }
    }
}
