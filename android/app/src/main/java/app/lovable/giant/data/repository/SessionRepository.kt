package app.lovable.giant.data.repository

import android.content.Context
import android.content.SharedPreferences
import app.lovable.giant.data.models.UserSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SessionRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("giant_native_session", Context.MODE_PRIVATE)
    
    private val _sessionState = MutableStateFlow<UserSession?>(null)
    val sessionState: StateFlow<UserSession?> = _sessionState.asStateFlow()

    init {
        loadSession()
    }

    fun loadSession(): UserSession? {
        val userId = prefs.getString("user_id", null)
        if (userId != null) {
            val session = UserSession(
                userId = userId,
                email = prefs.getString("email", null),
                username = prefs.getString("username", null),
                avatarUrl = prefs.getString("avatar_url", null),
                accessToken = prefs.getString("access_token", null)
            )
            _sessionState.value = session
            return session
        }
        _sessionState.value = null
        return null
    }

    fun saveSession(session: UserSession) {
        prefs.edit().apply {
            putString("user_id", session.userId)
            putString("email", session.email)
            putString("username", session.username)
            putString("avatar_url", session.avatarUrl)
            putString("access_token", session.accessToken)
            apply()
        }
        _sessionState.value = session
    }

    fun clearSession() {
        prefs.edit().clear().apply()
        _sessionState.value = null
    }

    fun hasValidSession(): Boolean {
        return prefs.getString("user_id", null) != null
    }
}
